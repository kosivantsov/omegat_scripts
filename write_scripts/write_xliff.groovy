/* :name=Write XLIFF :description=
 * @Purpose:    Export the whole project to XLF file for other CAT tools
 * @author:     Kos Ivantsov
 * @date:       2016-04-02
 * @version:    1.0
 */

/* set to true to export all project
 * otherwise the script exports only the current file   */
all_project = false

/* set to true to write a settings file for Okapi Rainbow that can be
 * used to convert the XLF file produced by this script, to TMX.
 * Otherwise set to false    */
rainbow = true

/* set to true to output only approved entries from XLF to TMX during
 * conversion in Rainbow    */
get_only_approved = true

/* add xml:space='preserve' to each TU in the resultant XLF	*/
preserve_spaces = true

/* Changing this variable lets the user specify
 * what to do with untranslated segments
 * "copy" for copying source to target
 * "empty" for leaving the target empty
 * "ignore" for not including target element    */
xliffUntranslated = "ignore"

/* The value set for the following variable will
 * be inserted into INTENTIONALLY empty translations as target   */
emptyTranslation = ''


import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*
import org.omegat.util.StringUtil
import org.omegat.core.data.ProtectedPart

def prop = project.projectProperties
if (!prop) {
    final def title = 'Export project to XLIFF file(s)'
    final def msg   = 'Please try again after you open a project.'
    showMessageDialog null, msg, title, INFORMATION_MESSAGE
    return
}
def folder = prop.projectRoot+'script_output'+File.separator
projname = new File(prop.getProjectRoot()).getName()
// create folder if it doesn't exist
if (! (new File (folder)).exists()) {
    (new File(folder)).mkdir()
    }
filename = folder + projname +'.xlf'
xliff_file = new File(filename).newWriter('UTF-8')

count = transcount = untranscount = emptytranscount = ignorecount = writecount = 0 

sourceLocale = prop.getSourceLanguage().toString().toLowerCase()
targetLocale = prop.getTargetLanguage().toString().toLowerCase()
xmlspace = preserve_spaces ? "xml:space=\'preserve\'" : ""

xliff_file.write("""<?xml version="1.0" encoding="UTF-8"?>
<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
""")

if (all_project) {
    files = project.projectFiles
} else {
    files = project.projectFiles.subList(editor.@displayedFileIndex, editor.@displayedFileIndex + 1)
}
    for (i in 0 ..< files.size())
    {
        fi = files[i]
        xliff_file.append("""  <file original="${StringUtil.makeValidXML(fi.filePath)}" source-language="$sourceLocale" target-language="$targetLocale" datatype="x-application/x-tmx+xml">
    <body>
      <trans-unit id="0" approved="yes">
        <source xml:lang="$sourceLocale"><ph id="filename">==FILENAME: "${StringUtil.makeValidXML(fi.filePath)}"==</ph>
        </source>
        <target xml:lang="$targetLocale" state="final"><ph id="filename">==FILENAME: "${StringUtil.makeValidXML(fi.filePath)}"==</ph>
        </target>
      </trans-unit>\n""")
        for (j in 0 ..< fi.entries.size()) {
            def state
            def approved = ''
            def unitnote = ''
            def ignore = ''
            ste = fi.entries[j]
            seg_num = ste.entryNum()
            source = ste.getSrcText()
            info = project.getTranslationInfo(ste)
            target = info ? info.translation : null
            def alltags = ''
            List xmlTags = []
            for (ProtectedPart pp : ste.getProtectedParts()) {
                alltags += pp.getTextInSourceSegment()
                xmlTags.add(StringUtil.makeValidXML(pp.getTextInSourceSegment()))
            }
            if (source == alltags) {
                count++
                ignorecount++
                continue
            }
            if (target != null) {
                approved = 'approved="yes"'
                state = 'state="final" state-qualifier="exact-match"'
                transcount++
                if (target.size() == 0 ) {
                    target = emptyTranslation
                    emptytranscount++
                    }
            }else{
                state = 'state="needs-translation"'
                switch (xliffUntranslated) {
                    case "copy" :
                        target = source
                        break
                    case ["empty", "ignore"] :
                        target = ''
                        break
                    default :
                        target =  source
                        break
                }
                untranscount++
            }
            unitnote = (info.hasNote()) ? "\n        <note>${StringUtil.makeValidXML(info.note)}</note>" : ""
            if (source != alltags){
                source = StringUtil.makeValidXML(source)
                target = StringUtil.makeValidXML(target)
                xmlTags = xmlTags.unique()
                for (x in 0 ..< xmlTags.size()){
                    source =  source.replace(xmlTags[x], "<ph id=\"$x\">" + xmlTags[x].toString() + "</ph>")
                    target =  target.replace(xmlTags[x], "<ph id=\"$x\">" + xmlTags[x].toString() + "</ph>")
                }
                targetLine = (target == '' && xliffUntranslated == "ignore") ? '' : "\n        <target $state xml:lang=\"$targetLocale\"><mrk mid=\"0\" mtype=\"seg\">$target</mrk></target>"
                xliff_file.append("""\
      <trans-unit id="$seg_num" $xmlspace $approved>
        <source xml:lang="$sourceLocale">$source</source>
        <seg-source><mrk mid="0" mtype="seg">$source</mrk></seg-source>\
$targetLine\
$unitnote
      </trans-unit>\n""")
                writecount++
            }
        count++
        }
        xliff_file.append("    </body>\n  </file>\n")
    }
xliff_file.append("</xliff>")
xliff_file.close()
console.clear()
console.println """\
${'*'*(filename.toString().size()+15)}
Output file:   $filename
${'*'*(filename.toString().size()+15)}
Segments processed: $count
Translated: $transcount
Untranslated: $untranscount
Empty translation: $emptytranscount
Segments written: $writecount
Segments ignored: $ignorecount
"""
mainWindow.statusLabel.setText("XLIFF Export: $filename written")
Timer timer = new Timer().schedule({mainWindow.statusLabel.setText(null); console.clear()} as TimerTask, 10000)

if (rainbow == true) {
    def approved = get_only_approved ? 'true' : 'false'
    rainbowfile = new File(folder + projname +'.xlf2tmx.rnb')
    rainbowfile.write("""\
<?xml version="1.0" encoding="UTF-8"?>
<rainbowProject version="4">
    <fileSet id="1">
        <root useCustom="0"></root>
    </fileSet>
    <fileSet id="2">
        <root useCustom="0"></root>
    </fileSet>
    <fileSet id="3">
        <root useCustom="0"></root>
    </fileSet>
    <output>
        <root use="0"></root>
        <subFolder use="0"></subFolder>
        <extension use="1" style="0">.out</extension>
        <replace use="0" oldText="" newText=""></replace>
        <prefix use="0"></prefix>
        <suffix use="0"></suffix>
    </output>
    <options sourceLanguage="$sourceLocale" sourceEncoding="UTF-8" targetLanguage="$targetLocale" targetEncoding="UTF-8"></options>
    <parametersFolder useCustom="0"></parametersFolder>
    <utilities xml:spaces="preserve"><params id="currentProjectPipeline">&lt;?xml version="1.0" encoding="UTF-8"?>
&lt;rainbowPipeline version="1">&lt;step class="net.sf.okapi.steps.common.RawDocumentToFilterEventsStep">&lt;/step>
&lt;step class="net.sf.okapi.steps.codesremoval.CodesRemovalStep">#v1
stripSource.b=true
stripTarget.b=true
mode.i=0
includeNonTranslatable.b=true
replaceWithSpace.b=false&lt;/step>
&lt;step class="net.sf.okapi.steps.formatconversion.FormatConversionStep">#v1
singleOutput.b=false
autoExtensions.b=true
targetStyle.i=0
outputPath=
outputFormat=tmx
useGenericCodes.b=false
skipEntriesWithoutText.b=true
approvedEntriesOnly.b=$approved
overwriteSameSource.b=false&lt;/step>
&lt;step class="net.sf.okapi.steps.common.FilterEventsToRawDocumentStep">&lt;/step>
&lt;/rainbowPipeline>
</params></utilities>
</rainbowProject>
""", 'UTF-8')
    }
return
