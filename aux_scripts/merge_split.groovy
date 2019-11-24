/* :name=  Merge or split segments :description= \
 *         Merge current segment with the next or split it at the selection
 * 
 * @author  Yu Tang, Dimitry Prihodko, Kos Ivantsov
 * @date    2019-11-23
 * @version 0.4.12
 *
 *         Make sure to use this script with its .properties file
 */
import org.apache.commons.lang.WordUtils
import org.omegat.core.segmentation.MapRule
import org.omegat.core.segmentation.Rule
import org.omegat.core.segmentation.SRX
import org.omegat.util.Language
import org.omegat.util.OStrings
import org.omegat.util.StaticUtils
import org.omegat.util.StringUtil
import javax.swing.JOptionPane

import static javax.swing.JOptionPane.*
import static org.omegat.util.StaticUtils.*
import static org.omegat.util.StringUtil.*

org.omegat.util.gui.UIThreadsUtil.executeInSwingThread {

    utils = (StringUtil.getMethods().toString().findAll("makeValidXML")) ? StringUtil : StaticUtils

    //check if we have selection at the end of the current source
    def entry = editor.currentEntry
    def src = entry.srcText
    def split
    if (editor.selectedText) {
        sel = editor.selectedText
        split = src.endsWith(sel) ? true : false
        if (split) {
            console.println(res.getString("endSelected"))
        }else{
            console.println(res.getString("wrongSelected"))
        }
    }

    split = split ? true : false

    initializeScript()

    // check if requirements are met
    if (! isReadyForNewRule()) {
        return
    }

    //get fragments to split or merge
    def nxtEntry = project.allEntries[entry.entryNum()]
    String nextSeg = entry.key.next ? entry.key.next : nxtEntry.srcText
    String beforeBreak = split ? src - sel : entry.srcText
    String afterBreak = split ? sel : nextSeg


    // exists check for the MappingRule
    Language srcLang = project.projectProperties.sourceLanguage
    def mapRule = project.projectProperties.projectSRX.findMappingRule(srcLang)
    if (! mapRule) {
        message = res.getString("noMappingRule") + res.getString("terminating")
        message.alert()
        return
    }

    // show confirm dialog
    def separator = ""
    if (! srcLang.isCJK()) {
        separator = " "
    }
    String message = split ?
    WordUtils.wrap("$beforeBreak\n\n$afterBreak\n\n", 180) + res.getString("proceed") :
    WordUtils.wrap("$beforeBreak$separator$afterBreak\n\n", 180) + res.getString("proceed")
    if (message.confirm() != 0) {
        console.clear()
        console.println(res.getString("noNewRule"))
        return
    }

    // create new rule
    boolean breakRule = split ? true : false // Exception
    beforeBreak = beforeBreak.toRegexPattern()
    afterBreak = afterBreak.toRegexPattern()
    if (! split && ! srcLang.isCJK()) {
        afterBreak = /\s?/ + afterBreak
    }
    def rule = new Rule(breakRule, beforeBreak, afterBreak)

    // check if there's a conficting split rule
    def conflict = mapRule.rules.find {
        it.beforebreak.trim() == beforeBreak.trim() && it.afterbreak.replaceAll(/^\\s\?/, '').trim() == afterBreak.replaceAll(/^\\s\?/, '').trim()
    }
    if (conflict) {
        mapRule.rules.remove(conflict)
    }


    // exists check for the new rule
    def found = mapRule.rules.find {
        it.beforebreak == beforeBreak && it.afterbreak == afterBreak
    }
    if (found) {
        message = res.getString("ruleExists") + res.getString("terminating")
        message.alert()
        return
    }

    // register new rule to the segmentation
    mapRule.rules[0..<0] = rule // Appends a new rule to the head of List.

    // reload the project
    if (showConfirmDialog(mainWindow, OStrings.getString("MW_REOPEN_QUESTION"),
        OStrings.getString("MW_REOPEN_TITLE"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        org.omegat.gui.main.ProjectUICommands.projectReload()
    } else {
        console.print(res.getString("noReload"))
        return
    }

    // fin
    console.println(res.getString("newSegmentationActive") + "(${new Date().timeString})")

} as Runnable

// ******************************************************
// methods
// ******************************************************
    public static String escapeNonRegex(String text) {
        return escapeNonRegex(text, true);
    }

    /**
     * Escapes the passed string for use in regex matching, so special regex
     * characters are interpreted as normal characters during regex searches.
     *
     * This is done by prepending a backslash before each occurrence of the
     * following characters: \^.+[]{}()&|-:=!<>
     *
     * If the parameter escapeWildcards is true, asterisks (*) and questions
     * marks (?) will also be escaped. If false, these will be converted to
     * regex tokens (* ->
     *
     * @param text
     *            The text to escape
     * @param escapeWildcards
     *            If true, asterisks and question marks are also escaped. If
     *            false, these are converted to there regex equivalents.
     *
     * @return The escaped text
     */
    public static String escapeNonRegex(String text, boolean escapeWildcards) {
        // handle backslash
        text = text.replaceAll("\\\\", "\\\\\\\\"); // yes, that's the correct
                                                    // nr of backslashes

        // String escape = "^.*+[]{}()&|-:=?!<>";
        for (char c : "^.+[]{}()&|-:=!<>".toCharArray()) {
            text = text.replaceAll("\\" + c, "\\\\" + c);
        }

        // handle "wildcard characters" ? and * (only if requested)
        // do this last, or the additional period (.) will cause trouble
        if (escapeWildcards) {
            // simply escape * and ?
            text = text.replaceAll("\\?", "\\\\?");
            text = text.replaceAll("\\*", "\\\\*");
        } else {
            // convert * (0 or more characters) and ? (1 character)
            // to their regex equivalents (\S* and \S? respectively)
            // text = text.replaceAll("\\?", "\\S?"); // do ? first, or * will
            // be converted twice
            // text = text.replaceAll("\\*", "\\S*");
            // The above lines were not working:
            // [ 1680081 ] Search: simple wilcards do not work
            // The following correction was contributed by Tiago Saboga
            text = text.replaceAll("\\?", "\\\\S"); // do ? first, or * will be
                                                    // converted twice
            text = text.replaceAll("\\*", "\\\\S*");
        }
        //make tags optional
        text = text.replaceAll(/(\\\<\/?\w+\d+\s?\/?\\\>)/, /\($1\)\?/) 
        return text;
    }

boolean isReadyForNewRule() {
    if (! project.isProjectLoaded()) {
        message = res.getString("noProjectOpen") + res.getString("terminating")
        return message.alert()
    }

    def srx = project.projectProperties.projectSRX
    if (! srx) {
        message = res.getString("noProjectSegmentation") + res.getString("terminating")
        return message.alert()
    }

    def entry = editor.currentEntry
    def src = entry.srcText
    def allProjEntries = project.allEntries
    def nxtEntry = allProjEntries[entry.entryNum()]
    String nextSeg = entry.key.next ? entry.key.next : nxtEntry.srcText
    def split
    def sel
    if (editor.selectedText) {
        sel = editor.selectedText
        split = src.endsWith(sel) ? true : false
    }
    split = split ? true :false

    if (! split && (! entry.srcText || ! nextSeg)) {
        message = res.getString("noMerge") + res.getString("terminating")
        return message.alert()
    }
    
    if ( split && (src == sel) ) {
        message = res.getString("noSplit") + res.getString("terminating")
        return message.alert()
    }

    // OK
    true
}

void initializeScript() {

    def entry = editor.currentEntry
    def src = entry.srcText
    def split
    if (editor.selectedText) {
        def sel = editor.selectedText
        split = src.endsWith(sel) ? true : false
    }

    // String class
    String.metaClass.toXML = { ->
        utils.makeValidXML(delegate as String)
    }
    String.metaClass.toRegexPattern = { ->
        escapeNonRegex(delegate as String)
    }
    String.metaClass.alert = { ->
        showMessageDialog null, delegate, split ? res.getString("splitTitle") : res.getString("mergeTitle"), INFORMATION_MESSAGE
        false
    }
    String.metaClass.confirm = { ->
        showConfirmDialog null, delegate, split ? res.getString("splitMessage") : res.getString("mergeMessage"), YES_NO_OPTION
    }

    // SRX class
    SRX.metaClass.findMappingRule = { Language srclang ->
        delegate.mappingRules.find { MapRule maprule ->
            maprule.compiledPattern.matcher(srclang.language).matches()
        }
    }

    // Language class
    Language.metaClass.isCJK = { ->
        delegate.languageCode.toUpperCase(Locale.ENGLISH) in ['ZH', 'JA', 'KO']
    }
}
