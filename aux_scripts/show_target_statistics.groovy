/* :name=   Utils - Target Statistics :description=\
 *          Dumps target statistics to the scripting console
 * 
 * 
 * Shows target statistics for the current project. Keep in mind
 * that the script doesn't discriminate between
 * unique and non-unique segments.
 * Results are copied to the clipboard automatically.
 * 
 * 
 * @author  Kos Ivantsov, Briac Pilpre
 * @date    2016-03-21
 * @version 0.2
 */

import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import org.omegat.core.statistics.Statistics
import org.omegat.core.data.ProtectedPart

def prop = project.getProjectProperties()

if (! prop ) {
    def title = "Target Statistics"
    def msg = "No project open."
    console.clear()
    console.println(title + "\n" + "="*title.size() + "\n" + msg)
    return
    }

def count_segment (s) {
    if (s == null) return 0

    spaces = /[\u00a0|\p{Blank}|\p{Space}]+/
    w = s.trim().replaceAll(spaces, " ").split(spaces)
    c = w.length

    return c
}

files = project.projectFiles
longestName = files.filePath.max {it.size()}
//console.println("Longest name is ${files.filePath.max {it}}, it's ${longestName.size()} chars long")
perfile = (" Target statistics per file:\n\
|${'-'*(longestName.size()+2)}+${"-"*9}+${"-"*27}+${"-"*24}+${"-"*24}+${"-"*35}|\n\
| Filename${' '*(longestName.size()-7)}|  Words  | Characters without spaces | Characters with spaces |\
 MSWord words in target | MSWord words in translated source |\n\
|${'-'*(longestName.size()+2)}+${"-"*9}+${"-"*27}+${"-"*24}+${"-"*24}+${"-"*35}|\n\
")

//console.clear()

totalWords = 0
totalCharsWithoutSpaces = 0
totalCharsWithSpaces = 0
totalTargetWordsMS = 0
totalSourceWordsMS = 0
sourceWordsMS_a = 0

for (i in 0 ..< files.size())
    {
        fi = files[i]
        curfilename = fi.filePath
        def words = 0
        def charsWithoutSpaces = 0
        def charsWithSpaces = 0
        def sourceWordsMS = 0
        def targetWordsMS = 0
        for (j in 0 ..< fi.entries.size()) {
            ste = fi.entries[j]
            src = ste.getSrcText()
            targ = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null
            if (targ == null){
	       for (ProtectedPart pp : ste.getProtectedParts()) {
                    src = src.replace(pp.getTextInSourceSegment(), pp.getReplacementWordsCountCalculation())
                }
            sourceWordsMS_a += count_segment(src)
            continue
            }
            for (ProtectedPart pp : ste.getProtectedParts()) {
                src = src.replace(pp.getTextInSourceSegment(), pp.getReplacementWordsCountCalculation())
                targ = targ.replace(pp.getTextInSourceSegment(), pp.getReplacementWordsCountCalculation())
            }
            words += Statistics.numberOfWords(targ)
            charsWithoutSpaces += Statistics.numberOfCharactersWithoutSpaces(targ)
            charsWithSpaces += Statistics.numberOfCharactersWithSpaces(targ)
            targetWordsMS += count_segment(targ)
            sourceWordsMS += count_segment(src)
        }
        perfile += ("\
| $curfilename ${" "*(longestName.size()-curfilename.size())}\
|${" "*(8-words.toString().size().toInteger())}$words \
|${" "*(26-charsWithoutSpaces.toString().size().toInteger())}$charsWithoutSpaces \
|${" "*(23-charsWithSpaces.toString().size().toInteger())}$charsWithSpaces \
|${" "*(23-targetWordsMS.toString().size().toInteger())}$targetWordsMS \
|${" "*(34-sourceWordsMS.toString().size().toInteger())}$sourceWordsMS \
|\n\
")
        totalWords += words
        totalCharsWithoutSpaces += charsWithoutSpaces
        totalCharsWithSpaces += charsWithSpaces
        totalTargetWordsMS += targetWordsMS
        totalSourceWordsMS += sourceWordsMS
    }

total = "\
\n Total target statistics:\n\
|${"-"*9}+${"-"*27}+${"-"*24}+${"-"*24}+${"-"*35}+${"-"*24}|\n\
|  Words  | Characters without spaces | Characters with spaces |\
 MSWord words in target | MSWord words in translated source | MS words in all source |\n\
|${"-"*9}+${"-"*27}+${"-"*24}+${"-"*24}+${"-"*35}+${"-"*24}|\n\
|${" "*(8-totalWords.toString().size().toInteger())}$totalWords \
|${" "*(26-totalCharsWithoutSpaces.toString().size().toInteger())}$totalCharsWithoutSpaces \
|${" "*(23-totalCharsWithSpaces.toString().size().toInteger())}$totalCharsWithSpaces \
|${" "*(23-totalTargetWordsMS.toString().size().toInteger())}$totalTargetWordsMS \
|${" "*(34-totalSourceWordsMS.toString().size().toInteger())}$totalSourceWordsMS \
|${" "*(23-(sourceWordsMS_a + totalSourceWordsMS).toString().size().toInteger())}${sourceWordsMS_a + totalSourceWordsMS} |\n\
|${"-"*9}+${"-"*27}+${"-"*24}+${"-"*24}+${"-"*35}+${"-"*24}|\n\
${"\n"*2}"

total += perfile + "|${'-'*(longestName.size()+2)}+${"-"*9}+${"-"*27}+${"-"*24}+${"-"*24}+${"-"*35}|"
console.println(total)
//clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
//clipboard.setContents(new StringSelection(total), null)
