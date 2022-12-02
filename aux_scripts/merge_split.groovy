/* :name=  Merge or split segments :description= \
 *         Merge current segment with the next or split it at the cursor (if in source text)
 * 
 * @author  Yu Tang, Kos Ivantsov
 * @date    2022-12-02
 * @version 0.5
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

//// External resources hack (use hardcoded strings if .properties file isn't found)
resBundle = { k,v ->
    try {
        v = res.getString(k)
    }
    catch (MissingResourceException e) {
        v
    }
}

//// UI Strings
name="Merge or split segments"
description="Merge current segment with the next, or split it at the cursor location"

noNewRule="No new rule added." 
newSegmentationActive="New segmentation rule activated."
splitMessage="Split result:" 
mergeMessage="Merge result:" 
proceed="Proceed?"
noMappingRule="MappingRule for the source language is not found." 
ruleExists="This rule already exists." 
terminating=" Terminating now!"
noReload="New rule added, but it will be activated only after the project is reloaded."
noProjectOpen="No project open!" 
noProjectSegmentation="The script works only with the project-specific segmentation rules!" 
noMerge="Merging with the next segment is not possible!" 
noSplit="Split point should not be at the beginning or the end of the source text!"
mergeTitle="Merging Segments"
splitTitle="Splitting Current Segment"


org.omegat.util.gui.UIThreadsUtil.executeInSwingThread {

    utils = (StringUtil.getMethods().toString().findAll("makeValidXML")) ? StringUtil : StaticUtils

    //check if the caret is in the source text
    entry = editor.currentEntry
    src = entry.srcText
    position = editor.editor.getCaretPosition()
    srcEnd = editor.editor.getOmDocument().getTranslationStart() - 1
    srcStart = srcEnd - src.size()
    srcRange = srcStart+1..srcEnd-1
    //if the caret is in the source text of the current segment, we split
    split = srcRange.contains(position) ? true : false
    //if the caret is at the beginning or end of the source text, splitting won't make sense
    boundry = position.equals(srcStart) || position.equals(srcEnd) ? true : false
    if (boundry) {
        message = resBundle("noSplit", noSplit) + resBundle("terminating", terminating)
        message.alert()
        return
    }
    nextEntry = project.allEntries[entry.entryNum()] ? project.allEntries[entry.entryNum()] : null
    // merge is to see if the merge is possible at all (would not be before the paragraph start)
    if (!nextEntry) {
        merge = false
    } else {
        merge = nextEntry.paragraphStart ? false : true
    }
    //get fragments to split or merge
    String nextSeg = entry.key.next ? entry.key.next : ""
    String beforeBreak = split ? src.substring(0, position - srcStart) : entry.srcText
    String afterBreak = split ? src.substring(position - srcStart, src.size()): nextSeg

    initializeScript()

    // check if requirements are met
    if (! isReadyForNewRule()) {
        return
    }

    // check for the MappingRule
    Language srcLang = project.projectProperties.sourceLanguage
    def mapRule = project.projectProperties.projectSRX.findMappingRule(srcLang)
    if (! mapRule) {
        message = resBundle("noMappingRule", noMappingRule) + resBundle("terminating", terminating)
        message.alert()
        return
    }

    // show confirm dialog
    def separator = ""
    if (! srcLang.isCJK()) {
        separator = " "
    }
    String message = split ?
    WordUtils.wrap("<html><i><b>$beforeBreak</b></i></html>\n\n<html><i><b>$afterBreak</b></i></html>\n\n", 400) + resBundle("proceed", proceed) :
    WordUtils.wrap("<html><i><b>$beforeBreak$separator$afterBreak</b></i></html>\n\n", 400) + resBundle("proceed", proceed)
    if (message.confirm() != 0) {
        console.clear()
        console.println(resBundle("noNewRule", noNewRule))
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
        message = resBundle("ruleExists", ruleExists) + resBundle("terminating", terminating)
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
        console.print(resBundle("noReload", noReload))
        return
    }

    // fin
    console.println(resBundle("newSegmentationActive", newSegmentationActive) + "(${new Date().timeString})")

} as Runnable

// ******************************************************
// methods
// ******************************************************
public static String escapeNonRegex(String text) {
    return escapeNonRegex(text, true)
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
        text = text.replaceAll("\\" + c, "\\\\" + c)
    }

    // handle "wildcard characters" ? and * (only if requested)
    // do this last, or the additional period (.) will cause trouble
    if (escapeWildcards) {
        // simply escape * and ?
        text = text.replaceAll("\\?", "\\\\?")
        text = text.replaceAll("\\*", "\\\\*")
    } else {
        // convert * (0 or more characters) and ? (1 character)
        // to their regex equivalents (\S* and \S? respectively)
        // text = text.replaceAll("\\?", "\\S?"); // do ? first, or * will
        // be converted twice
        // text = text.replaceAll("\\*", "\\S*")
        // The above lines were not working:
        // [ 1680081 ] Search: simple wilcards do not work
        // The following correction was contributed by Tiago Saboga
        text = text.replaceAll("\\?", "\\\\S")    // do ? first, or * will be
                                                  // converted twice
        text = text.replaceAll("\\*", "\\\\S*")
    }
    //make tags optional
    text = text.replaceAll(/(\\\<\/?\w+\d+\s?\/?\\\>)/, /\($1\)\?/) 
    return text
}

boolean isSplit() {
    entry = editor.currentEntry
    src = entry.srcText
    position = editor.editor.getCaretPosition()
    srcEnd = editor.editor.getOmDocument().getTranslationStart() - 1
    srcStart = srcEnd - src.size()
    srcRange = srcStart+1..srcEnd-1
    //if the caret is in the source text of the current segment, we split
    split = srcRange.contains(position) ? true : false    
    return split
}

boolean isMerge() {
    def nextEntry = project.allEntries[entry.entryNum()] ? project.allEntries[entry.entryNum()] : null
    if (!nextEntry) {
        merge = false
    } else {
        merge = nextEntry.paragraphStart ? false : true
    }
    return merge
}

boolean isReadyForNewRule() {
    if (! project.isProjectLoaded()) {
        message = resBundle("noProjectOpen", noProjectOpen) + resBundle("terminating", terminating)
        return message.alert()
    }

    def srx = project.projectProperties.projectSRX
    if (! srx) {
        message = resBundle("noProjectSegmentation", noProjectSegmentation) + resBundle("terminating", terminating)
        return message.alert()
    }

    split = isSplit()
    merge = isMerge()
    
    if (! split && ! merge) {
        message = resBundle("noMerge", noMerge) + resBundle("terminating", terminating)
        return message.alert()
    }

    // OK
    true
}

void initializeScript() {
    split = isSplit()
    merge = isMerge()

    // String class
    String.metaClass.toXML = { ->
        utils.makeValidXML(delegate as String)
    }
    String.metaClass.toRegexPattern = { ->
        escapeNonRegex(delegate as String)
    }
    String.metaClass.alert = { ->
        showMessageDialog null, delegate, split ? resBundle("splitTitle", splitTitle) : resBundle("mergeTitle", mergeTitle), INFORMATION_MESSAGE
        false
    }
    String.metaClass.confirm = { ->
        showConfirmDialog null, delegate, split ? resBundle("splitMessage", splitMessage) : resBundle("mergeMessage", mergeMessage), YES_NO_OPTION
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
