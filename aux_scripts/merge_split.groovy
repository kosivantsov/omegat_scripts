/* :name=  Merge or split segments :description= \
 *         Merge current segment with the next or split it at the cursor (if in source text)
 * 
 * @author  Yu Tang, Kos Ivantsov
 * @date    2024-09-10
 * @version 1.5
 */

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel

import org.omegat.core.data.ProtectedPart
import org.omegat.core.segmentation.datamodels.MappingRulesModel
import org.omegat.core.segmentation.MapRule
import org.omegat.core.segmentation.Rule
import org.omegat.core.segmentation.SRX
import org.omegat.util.Language
import org.omegat.util.OStrings
import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import org.omegat.util.StringUtil

import static javax.swing.JOptionPane.*
import static org.omegat.util.StaticUtils.*
import static org.omegat.util.StringUtil.*

enforceProjectSRX   = true   //if true, the script will make sure project-specific segmentation is enabled
separateMappingRule = true   //if true, the script will add a separate group for its rules
showTags            = true   //if false, tags won't be shown in the confirmation message
paintTags           = true   //if true, tags will be shown in different font size and color
tagColor            = "gray" //tag color
tagSize             = 1      //tag size

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
name = "Merge or split segments"
description = "Merge current segment with the next, or split it at the cursor location"

srxEnabled = "Project-specific segmentation rules have been enabled.\nRun the script again after the project is reloaded."
noNewRule = "No new rule added." 
newSegmentationActive = "New segmentation rule activated."
splitMessage = "Split result:" 
mergeMessage = "Merge result:" 
proceed = "Add this rule?"
noMappingRule = "MappingRule for the source language is not found."
ruleExists = "This rule already exists." 
terminating = " Terminating now!"
noReload = "New rule added, but it will be activated only after the project is reloaded."
noProjectOpen = "No project open!" 
noProjectSegmentation = "The script works only with the project-specific segmentation rules!" 
noMerge = "Merging with the next segment is not possible!" 
noSplit = "Split point should not be at the beginning or the end of the source text!"
inTag = "Split point cannot be inside a tag!" 
mergeTitle="Merging Segments"
splitTitle="Splitting Current Segment"

if (! project.isProjectLoaded()) {
    message = resBundle("noProjectOpen", noProjectOpen) + resBundle("terminating", terminating)
    final def msg = message
    final def title = resBundle("name", name)
    showMessageDialog null, msg, title, INFORMATION_MESSAGE
    console.println(message)
    return
}

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

    currentFile = project.projectFiles.subList(editor.@displayedFileIndex, editor.@displayedFileIndex + 1)[0]
    entryRelativeNum = (entry.entryNum() - currentFile.entries[0].entryNum() + 1)
    segmentsInFile = currentFile.entries.size()
    nextEntry = (entryRelativeNum != segmentsInFile) ? project.allEntries[entry.entryNum()] : null
    // merge is to see if the merge is possible at all (would not be on the last entry of a file or before a new paragraph)
    if (!nextEntry) {
        merge = false
    } else {
        merge = nextEntry.paragraphStart ? false : true
    }
    //get fragments to split or merge
    String nextSeg = nextEntry ? nextEntry.srcText : ""
    String beforeBreak = split ? src.substring(0, position - srcStart) : entry.srcText
    String afterBreak = split ? src.substring(position - srcStart, src.size()): nextSeg
    if (showTags) {
        beforeBreakMsg = beforeBreak.replaceAll(/\</, /\&lt\;/).replaceAll(/\>/, /\&gt\;/)
        afterBreakMsg = afterBreak.replaceAll(/\</, /\&lt\;/).replaceAll(/\>/, /\&gt\;/)
        if (paintTags) {
            beforeBreakMsg = beforeBreakMsg.replaceAll(/(\&lt\;\/?\s?\w+\s?\/?\d+?\s?\/?\s?\/?\&gt\;)/, /\<font size=$tagSize style=color:$tagColor\>$1\<\/font\>/)
            afterBreakMsg = afterBreakMsg.replaceAll(/(\&lt\;\/?\s?\w+\s?\/?\d+?\s?\/?\s?\/?\&gt\;)/, /\<font size=$tagSize style=color:$tagColor\>$1\<\/font\>/)
        }
    }
    initializeScript()
    // check if requirements are met
    if (! isReadyForNewRule()) {
        return
    }
    // Enforcing per-project srx
    if (! project.projectProperties.projectSRX && enforceProjectSRX) {
        srx = Preferences.getSRX()
        projectSRX = srx.copy()
        if  (OStrings.VERSION < '6.1.0') {
            projectSRXFile = new File(project.projectProperties.getProjectInternal() + "segmentation.conf")
            projectSRX.saveTo(projectSRX, projectSRXFile)
        } else {
            projectSRXFile = new File(project.projectProperties.getProjectInternal())
            projectSRX.saveToSrx(projectSRX, projectSRXFile)
        }
        project.projectProperties.setProjectSRX(projectSRX)
        message = resBundle("srxEnabled", srxEnabled)
        message.alert()
        org.omegat.gui.main.ProjectUICommands.projectReload()
        return
    }
    srx = project.projectProperties.getProjectSRX()

    // check for the MappingRule
    Language srcLang = project.projectProperties.sourceLanguage
    srcCode = srcLang.getLanguageCode().toUpperCase()
    if (separateMappingRule) {
        mergeSplitMapRule = new MapRule("MergeSplit", "$srcCode.*", new ArrayList<>())
        if (! srx.getMappingRules()[0].toString().contains("MergeSplit ($srcCode.*)")) {
            srx.getMappingRules().add(0, mergeSplitMapRule)
        }
    }
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
    //"""<html><hr/><i><b>${beforeBreakMsg}</b></i><br/><hr/><i><b>${afterBreakMsg}</b></i><hr/><br/><br/><br/>""" + resBundle("proceed", proceed) + "</html>" :
    //"""<html><hr/><i><b>${beforeBreakMsg}${separator}${afterBreakMsg}</b></i><hr/><br/><br/><br/>""" + resBundle("proceed", proceed) + "</html>"
    """<html>${resBundle("proceed", proceed)}<br/><br/><hr/><i><b>${beforeBreakMsg}</b></i><br/><hr/><i><b>${afterBreakMsg}</b></i><hr/></html>""" :
    """<html>${resBundle("proceed", proceed)}<br/><br/><hr/><i><b>${beforeBreakMsg}${separator}${afterBreakMsg}</b></i><hr/><br/><br/><br/>"""
    
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

    if (showConfirmDialog(null, OStrings.getString("MW_REOPEN_QUESTION"),
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

def getActions() {
    entry = editor.currentEntry
    src = entry.srcText
    tags = entry.getProtectedParts()
    position = editor.editor.getCaretPosition()
    srcEnd = editor.editor.getOmDocument().getTranslationStart() - 1
    srcStart = srcEnd - src.size()
    srcRange = srcStart+1..srcEnd-1
    //if the caret is in the source text of the current segment, we split
    split = srcRange.contains(position) ? true : false    
    //if next segment starts a new paragraph or there's no next segment, we refuse to merge
    def nextEntry = project.allEntries[entry.entryNum()] ? project.allEntries[entry.entryNum()] : null
    if (!nextEntry) {
        merge = false
    } else {
        merge = nextEntry.paragraphStart ? false : true
    }

    //if the caret is at the beginning or end of the source text, splitting won't make sense
    boundary = position.equals(srcStart) || position.equals(srcEnd) ? true : false
    //if the caret is in the source text, we'll try to figure out if it's in the middle of a tag
    intag = false
    if (split && ! boundary) {
        jumpBack = (position - srcStart) < 5 ? 0 : position - srcStart - 5
        jumpForth = (position - srcStart + 5 > src.size()) ? src.size() : position - srcStart + 5
        beginChunk = src.substring(jumpBack, position - srcStart).find(/(\<|\{).*$/)
        endChunk = src.substring(position - srcStart, jumpForth).find(/^.*(\>|\})/)
        beginChunk = beginChunk ? beginChunk : ""
        endChunk = endChunk ? endChunk : ""
        wholeChunk = beginChunk+endChunk
        def textTags = []
        if (tags.size()>0) {
            for (i in 0..tags.size()-1) {
                textTags.add(tags[i].getTextInSourceSegment())
            }
        }
        if (textTags.contains(beginChunk) || textTags.contains(endChunk)) {
            intag = false
        } else {
            if (textTags.contains(wholeChunk)) {
                intag = true
            }
        }
    }
    return [split, merge, boundary, intag]
}

boolean isReadyForNewRule() {

    def srx = project.projectProperties.projectSRX
    if (! srx && ! enforceProjectSRX) {
        message = resBundle("noProjectSegmentation", noProjectSegmentation) + resBundle("terminating", terminating)
        return message.alert()
    }

    result = getActions()
    split = result[0]
    merge = result[1]
    boundary = result[2]
    intag = result[3]
    
    if (boundary) {
        message = resBundle("noSplit", noSplit) + resBundle("terminating", terminating)
        console.println message
        return message.alert()
    }

    if (! split && ! merge) {
        message = resBundle("noMerge", noMerge) + resBundle("terminating", terminating)
        return message.alert()
    }

    if (intag) {
        message = resBundle("inTag", inTag) + resBundle("terminating", terminating)
        return message.alert()
        
    }

    // OK
    true
}

void initializeScript() {
    result = getActions()
    split = result[0]
    merge = result[1]

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
        pane = new JOptionPane()
        panel = new JPanel(new BorderLayout())
        text = delegate
        bestSize = new Dimension((text.size() < 370 ? 350 : text.size().intdiv(2) + 100 ), (text.size() < 370 ? 250 : text.size().intdiv(3) + 100))
        panel.setPreferredSize(bestSize)
	   label = new JLabel(text)
	   panel.add(label, "North")
        pane.showConfirmDialog(null, panel, split ? resBundle("splitMessage", splitMessage) : resBundle("mergeMessage", mergeMessage), YES_NO_OPTION)
        //showConfirmDialog( null, delegate, split ? resBundle("splitMessage", splitMessage) : resBundle("mergeMessage", mergeMessage), YES_NO_OPTION)
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
