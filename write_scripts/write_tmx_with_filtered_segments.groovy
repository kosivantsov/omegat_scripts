/* :name = Write TMX with Filtered Segments :description = Export segments filtered by the translator's ID and/or date
 *  
 * @author  Kos Ivantsov
 * @date    2032-11-03
 * @version 0.2
 */

/* Set the script variables to filter segments for export.
   If <project_folder>/.ini/tmxexport.ini is found,
   variables will be read from that file.
   If a variable is not set, the default values will be used
   (see comments after each var) */
filter       = "" //possible values with quotes: "translator", "editor", "creationDate", "changeDate" [defaults to "changeDate"]
secondFilter = "" //possible values with quotes: "translator",  "editor", "creationDate", "changeDate", "none" [defaults to "none"]
translator   = "" //always in quotes [defaults to the current Translator's ID]
editor       = "" //always in quotes [defaults to the current Translator's ID]
creationDate = "" //yyyy-MM-dd[ HH:mm] – All TU's created after this date will be used [defaults to 24h ago]
changeDate   = "" //yyyy-MM-dd[ HH:mm] – All TU's changed after this date will be used [defaults to 24h ago]
includeXAuto = false

/* Import */
import groovy.util.ConfigSlurper
import java.util.Calendar
import org.omegat.util.Preferences
import org.omegat.util.StringUtil
import org.omegat.util.TMXReader2
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

/* CLI or GUI probing */
def echo
def cli
try {
    mainWindow.statusLabel.getText()
    echo = {
        k -> console.println(k.toString())
    }
    cli = false
} catch(Exception e) {
    echo = { k -> 
        println("\n~~~ Script output ~~~\n\n" + k.toString() + "\n\n^^^^^^^^^^^^^^^^^^^^^\n")
    }
    cli = true
}

/* Report the script name in the console */
name = "Write Filtered Segments to TMX"
echo("$name\n${"=" * (name.size())}")

/* Check if the project is open */
prop = project.projectProperties
if (!prop) {
    def title = 'Export TMX'
    def msg   = 'Please try again after you open a project.'
    echo(msg)
    showMessageDialog null, msg, title, INFORMATION_MESSAGE
    return
}

/* Try to get variables from the external file */
varsFile = new File(prop.getProjectRoot() + ".ini" + File.separator + "tmxexport.ini")
if (varsFile.exists()) {
    echo("Using filtering settings defined in ${varsFile}:")
    externalVars = new ConfigSlurper().parse(varsFile.toURL())
    filter = externalVars.filter ?: filter
    secondFilter = externalVars.secondFilter ?: secondFilter
    translator = externalVars.translator ?: translator
    editor = externalVars.editor ?: editor
    creationDate = externalVars.creationDate ?: creationDate
    changeDate = externalVars.changeDate ?: changeDate
    includeXAuto = externalVars.includeXAuto ?: includeXAuto
} else {
    echo("Using filtering settings defined in this script:")
}

/* Get the default values for the filtering if not set */
translator = translator ? translator : Preferences.getPreferenceDefault(Preferences.TEAM_AUTHOR, System.getProperty("user.name"))
editor = editor ? editor : Preferences.getPreferenceDefault(Preferences.TEAM_AUTHOR, System.getProperty("user.name"))
parseDate = { dateString ->
    possibleDateFormats = ["yyyy-MM-dd HH:mm", "yyyy-MM-dd"]
    parsedDate = null
    def dateFormat
    for (format in possibleDateFormats) {
        try {
            dateFormat = new java.text.SimpleDateFormat(format)
            parsedDate = dateFormat.parse(dateString)
            break
        } catch (java.text.ParseException e) {
            // If parsing fails, try the next format
        }
    }
    return parsedDate
}
/* If the date wasn't specified, get the date 24h ago */
currentDate = new Date()
calendar = Calendar.getInstance()
calendar.time = currentDate
calendar.add(Calendar.HOUR_OF_DAY, -24)
defaultDate = calendar.time
creationDate = parseDate(creationDate) ? parseDate(creationDate) : defaultDate
changeDate = parseDate(changeDate) ? parseDate(changeDate) : defaultDate

/* Format strings for the exported TMX filename
   and to report the settings in the console */
switch (filter) {
    case "translator":
        fileNamePart = "_T-${translator.replaceAll(/ /, "")}"
        varMessage = "Exporting segments translated by $translator" 
        break
    case "editor":
        fileNamePart = "_E-${editor.replaceAll(/ /, "")}"
        varMessage = "Exporting segments changed by $editor"
        break
    case "creationDate":
        fileNamePart = "_T-${creationDate.format("yyyy-MM-dd_HHmm")}"
        varMessage = "Exporting segments created after ${creationDate.format("yyyy-MM-dd HH:mm")}"
        break
    case "changeDate":
        fileNamePart = "_E-${changeDate.format("yyyy-MM-dd_HHmm")}"
        varMessage = "Exporting segments changed after ${changeDate.format("yyyy-MM-dd HH:mm")}"
        break
    default:
        filter = "changeDate"
        fileNamePart = "_E-${changeDate.format("yyyy-MM-dd_HHmm")}"
        varMessage = "Exporting segments changed after ${changeDate.format("yyyy-MM-dd HH:mm")}"
        break
}
switch (secondFilter) {
    case "translator":
        if (filter == "translator") {
           secondFilter = "none"
        } else {
            fileNamePart = "${fileNamePart}_T-${translator.replaceAll(/ /, "")}"
            varMessage = "${varMessage} and created by $translator"
        }
        break
    case "editor":
        if (filter == "editor") {
           secondFilter = "none"
        } else {
            fileNamePart = "${fileNamePart}_E-${editor.replaceAll(/ /, "")}"
            varMessage = "${varMessage} and edited by $editor"
        }
        break
    case "creationDate":
        if (filter == "creationDate") {
           secondFilter = "none"
        } else {
            fileNamePart = "${fileNamePart}_T-${creationDate.format("yyyy-MM-dd_HHmm")}"
            varMessage = "${varMessage} and created after ${creationDate.format("yyyy-MM-dd HH:mm")}"
        }
        break
    case "changeDate":
        if (filter == "changeDate") {
           secondFilter = "none"
        } else {
            fileNamePart = "${fileNamePart}_E-${changeDate.format("yyyy-MM-dd_HHmm")}"
            varMessage = "${varMessage} and changed after ${changeDate.format("yyyy-MM-dd HH:mm")}"
        }
        break
    default:
        secondFilter = "none"
}
/* Report the selected settings */
echo("${varMessage}\n${"-" * (name.size())}")

/* Collect project-specific data */
sourceLocale = prop.getSourceLanguage().toString()
targetLocale = prop.getTargetLanguage().toString()
if (prop.isSentenceSegmentingEnabled()) {
    segmenting = TMXReader2.SEG_SENTENCE
} else {
    segmenting = TMXReader2.SEG_PARAGRAPH
}

/* Create a StringWriter object that will contain the exported TMX to be written to the file */
tmxContents = new StringWriter()
/* Add the TMX header */
tmxContents << """\
<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE tmx SYSTEM \"tmx14.dtd\">
<tmx version=\"1.4\">
 <header
  creationtool=\"OmegaTScripting\"
  segtype=\"$segmenting\"
  o-tmf=\"OmegaT TMX\"
  adminlang=\"EN-US\"
  srclang=\"$sourceLocale"\
  datatype=\"plaintext\"/>
  <body>\
"""

/* Now traverse the project and collect entries */
filteredCount = 0
translatedEntries = []
project.allEntries.each() { ste ->
    info = project.getTranslationInfo(ste)
    /* Only translated segments */
    if (info.translation !== null) {
        entryMap = [:]
        includeEntry = false
        entryMap.isXAuto = info.linked.toString()
        entryMap.creationId = info.creator
        entryMap.changeId = info.changer
        entryMap.creationDate = info.creationDate
        entryMap.changeDate = info.changeDate
        entryMap.source = StringUtil.makeValidXML(ste.srcText)
        entryMap.target = info.translation ? StringUtil.makeValidXML(info.translation) : ""
        if (info.hasNote()) {
            entryMap.note = StringUtil.makeValidXML(info.note)
        }
        /* If the segment is marked as an alternative translation, collect additional props */
        if (! info.defaultTranslation) {
            entryMap.keyID = ste.key.id
            entryMap.keyFile = ste.key.file
            entryMap.keyNext = ste.key.next
            entryMap.keyPrev = ste.key.prev
        }
        if (filter == "translator" || secondFilter == "translator") {
            if (info.creator == translator) {
                includeEntry = true
            }
        } else if (filter == "editor" || secondFilter == "editor") {
            if (info.changer == changer) {
                includeEntry = true
            }
        } else if (filter == "creationDate" || secondFilter == "creationDate") {
            if (new Date(info.creationDate) >= creationDate) {
                includeEntry = true
            }
        } else if (filter == "changeDate" || secondFilter == "changeDate") {
            if (new Date(info.changeDate) >= changeDate) {
                includeEntry = true
            }
        }
        if (!includeXAuto) {
            if (info.linked.toString() == "xAUTO") {
                includeEntry = false
            }
        }
        if (includeEntry) {
            translatedEntries.add(entryMap)
            filteredCount++
        }
    }
}

/* Remove repeated entries. If a non-unique segment has an alternative translation,
   each alternative + one default translations are going to be stored */
uniqueEntries = []
skipCount = 0
translatedEntries.each() { entry ->
    entryAsString = entry.toString()
    if (! uniqueEntries.any { it.toString() == entryAsString }) {
        uniqueEntries.add(entry)
    } else {
        skipCount++
    }
}
/* Traverse collected entries and populate tmxContents */
uniqueEntries.each() { entry ->
    tmxContents << """
    <tu>"""
    if (entry.note) {
        tmxContents << """
      <note>${entry.note}</note>"""
    }
    if (entry.keyFile) {
       prevStr = (entry.keyPrev !== null) ? "\n      <prop type=\"prev\">${StringUtil.makeValidXML(entry.keyPrev)}</prop>" : ""
       nextStr = (entry.keyNext !== null) ? "\n      <prop type=\"next\">${StringUtil.makeValidXML(entry.keyNext)}</prop>" : ""
       idStr = (entry.keyID !== null) ? "\n      <prop type=\"id\">${entry.keyID}</prop>" : ""
       tmxContents << """
      <prop type=\"file\">${entry.keyFile}</prop>${prevStr}${nextStr}${idStr}"""
    }
    tmxContents << """
      <tuv xml:lang=\"$sourceLocale\">
        <seg>${entry.source}</seg>
      </tuv>
      <tuv xml:lang=\"$targetLocale\" changeid=\"${entry.changeId}\" changedate=\"${new Date(entry.changeDate).format("yyyyMMdd'T'HHmmss'Z'")}\" \
creationid=\"${entry.creationId}\" creationdate=\"${new Date(entry.creationDate).format("yyyyMMdd'T'HHmmss'Z'")}\">
        <seg>${entry.target}</seg>
      </tuv>
    </tu>"""
}
/* Add the TMX ending to the tmxContents */
tmxContents << "\n  </body>\n</tmx>"

/* Write the TMX file if there was at least one TU in the uniqueEntries */
if (uniqueEntries.size() > 0) {
    /* Check if the the export folder exists. If it doesn't, it will be created */
    exportFolder = new File(prop.projectRoot + "script_output" + File.separator + "tmx_export")
    if (!exportFolder.exists()) {
        exportFolder.mkdirs()
    }
    /* Write collected entries to the TMX file */
    exportFile = new File(exportFolder.toString() + File.separator + sourceLocale + "_" + targetLocale + fileNamePart + ".tmx")
    exportFile.write(tmxContents.toString(), "UTF-8")
    echo("Found entries: $filteredCount\nRepeated with default translation: $skipCount\n" +
                    "${uniqueEntries.size()} entries written to ${exportFile.toString()}\n${"=" * (name.size())}\nDone")
} else {
    echo("No entries found for the export\n${"=" * (name.size())}\nDone")
}
return
