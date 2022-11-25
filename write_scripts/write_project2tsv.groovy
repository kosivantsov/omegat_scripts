/* :name=       Write Project to TSV :description=Writes all project entries to a tab separated values file
 * 
 *              THIS SCRIPT WAS SPONSORED BY
 *                 ==   cApStAn sprl   ==
 *                     www.capstan.be
 * 
 * #Purpose:    Export the current file into a TSV file containing
 *              all project segments.
 * #Files:      Writes a TSV file containing all project segments
 *              in the 'script_output' subfolder
 *              of the current project's root.              
 * 
 * @author:     Kos Ivantsov
 * @date:       2022-11-22
 * @latest:     2022-11-25
 * @version:    1.0
 */

import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

import java.awt.Desktop
import org.omegat.util.OStrings
import org.omegat.util.StaticUtils
import org.omegat.util.StringUtil

//// Script options
autoopen   = "none"           // Automatically open the table file upon creation
                              // ("folder"|"file"|"none")
//   Place the following variables (with quotes): 
//       "filePath", "fileName", "segmentNumber", "source", "target",
//       "createID", "createDate", "changeID", "changeDate", "comment",
//       "segmentID", "note", "altUniq", "xStatus", "origin"
//   in the sequence you want your data to appear in the TSV file
//   "altUniq" is a column that will contain info about repetitions, alternative translation, paragraph breaks,
//   empty translations and non-translated segmets. Markers used for this info can be changed (see the next block of variables)
tsvFields = ["fileName", "segmentID", "segmentNumber", "source", "target", "createDate", "altUniq"]
dateFormat = "YY/MM/dd HH:mm" //If creation and change dates are to be included, this is the format to be used
langCodeHead = true           // Header for the source and target fields: either language code (if true), or a predifined string (if false)

//// UI Strings
//   Script interaction strings
name="Write Project to TSV"
description="Writes all project entries to a tab separated values file"
msgTitle="Export Project to a TSV File"
msgNoProject="Please try again after you open a project."
message="{0} segments written to {1}"
//   Markers for the altUniq field
uniqStr          = ""         //Marker for uniq segments
firstStr         = "1"        //Marker for the 1st occurance of a repeated segment
repStr           = "+"        //Marker for further occurances of a repeated segment
altStr           = "a"        //Marker for alternative translation of the segmnent
defStr           = ""         //Marker for default translation of the segmnent
notTranslated    = "NT"       //Marker for segments with no translation (NOT empty, but untranslated)
emptyTrans       = "<EMPTY>"  //Marker for empty translations (intentionally empty, NOT untranslated)
nA               = "N/A"      //Marker for non-availabe data
paragraphMark    = "§"        //Marker for segments that begin new paragraphs in the source text
//  Headers in the TVS file
filePath         = "File Path"
fileName         = "File Name"
segmentNumber    = "Segment"
source           = "Source"
target           = "Target"
createID         = "Translator"
createDate       = "Date"
changeID         = "Editor"
changeDate       = "Date"
comment          = "Comment"
segmentID        = "ID"
note             = "Note"
altUniq          = "Alt/Uniq"
xStatus          = "xStatus"
origin           = "Origin"

//// CLI or GUI probing
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

//// External resources hack (use hardcoded strings if .properties file isn't found)
resBundle = { k,v ->
    try {
        v = res.getString(k)
    }
    catch (MissingResourceException e) {
        v
    }
}

//// Check if the project is open
def prop = project.projectProperties
if (!prop) {
    final def title = resBundle("msgTitle", msgTitle)
    final def msg   = resBundle("msgNoProject", msgNoProject)
    showMessageDialog null, msg, title, INFORMATION_MESSAGE
    return
}

//// Make sure this script works with older version of OmegaT
utils = (StringUtil.getMethods().toString().findAll("format")) ? StringUtil : StaticUtils

//// Get language codes
srcCode = project.projectProperties.sourceLanguage
tgtCode = project.projectProperties.targetLanguage

//// Map the variables used to collect the data, with their values
tsvMap = [filePath:      resBundle("filePath", filePath),
          fileName:      resBundle("fileName", fileName),
          segmentNumber: resBundle("segmentNumber", segmentNumber),
          source:        resBundle("source", source),
          target:        resBundle("target", target),
          createID:      resBundle("createID", createID),
          createDate:    resBundle("createDate", createDate),
          changeID:      resBundle("changeID", changeID),
          changeDate:    resBundle("changeDate", changeDate),
          comment:       resBundle("comment", comment),
          segmentID:     resBundle("segmentID", segmentID),
          note:          resBundle("note", note),
          altUniq:       resBundle("altUniq", altUniq),
          xStatus:       resBundle("xStatus", xStatus),
          origin:        resBundle("origin", origin)]

//// Use language codes for source and target headers
if (langCodeHead) {
    tsvMap.source = srcCode
    tsvMap.target = tgtCode
}

//// Create a StringWriter that will collect strings to be written to the TSV file
tsvData = new StringWriter()

//// Check which fields should be present in the TSV file, and get their values
collectData = {
    String tsvTemp = ""
    for (c in 0..tsvFields.size()-1) {
        text = tsvMap."${tsvFields[c]}"
        tsvTemp += "\"${text}\"\u240B"
    }
    tsvData << "${tsvTemp}\n"
}

//// Create the header line
collectData()

//// Traverse the whole project and collect the data
files = project.projectFiles
count = 0
for (i in 0 ..< files.size()) {
    fi = files[i]
    for (j in 0 ..< fi.entries.size()) {
        altUniq = "\u2007"
        def ste = fi.entries[j]
        segmentNumber = ste.entryNum()
        source = ste.getSrcText().replaceAll("\"", "\"\"")
        target = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null
	   // nullTargetReplacement is used to substitute a null target (non-translated segment)
        nullTargetReplacement = "zzz" + new Date().format('yyyyMMddHHmmss') + new Random().nextInt(1000000000) + "zzz"
        if (target == null) {
            altUniq += resBundle("notTranslated", notTranslated)
            target = nullTargetReplacement
        }
        if (target.size() == 0) {
            altUniq += resBundle("emptyTrans", emptyTrans)
            target = "\u2007"
        }
        target = target.replaceAll("\"", "\"\"").replaceAll(nullTargetReplacement, "\u2007")
        filePath = fi.filePath.toString()
        fileNameTemp = filePath.tokenize("\\/")
        fileName = fileNameTemp[fileNameTemp.size()-1]
        info = project.getTranslationInfo(ste)
        isDup = ste.getDuplicate()
        if (isDup == "FIRST") {
            altUniq += resBundle("firstStr", firstStr)
        }
        if (isDup == "NEXT") {
            altUniq += resBundle("repStr", repStr)
        }
        if (isDup == "NONE") {
            altUniq += resBundle("uniqStr", uniqStr)
        }
        isAlt = info.defaultTranslation ? defStr : altStr
        altUniq += resBundle("isAlt", isAlt)
        createID = info.creator
        createDate = new Date(info.creationDate).format(dateFormat)
        changeID = info.changer
        changeDate = new Date(info.changeDate).format(dateFormat)
        xStatus = info.linked ? info.linked.toString() : "\u2007"
        comment = ste.getComment() ? ste.getComment() : "\u2007"
        segmentID = ste.key.id ? ste.key.id : resBundle("nA", nA)
        note = info.note ? info.note : "\u2007"
        // origin was introduced in v.5.8
        if (OStrings.VERSION >= "5.8.0") {
            origin = info.origin ? info.origin : resBundle("nA", nA)
        } else {
            origin = "\u2007" //resBundle("nA", nA)
        }
        newPar = ste.paragraphStart ? ste.paragraphStart.toString() : null
        if (newPar) {
            altUniq += resBundle("paragraphMark", paragraphMark)
        }
        
        // Update the map
        tsvMap.filePath      = filePath
        tsvMap.fileName      = fileName
        tsvMap.segmentNumber = segmentNumber
        tsvMap.source        = source
        tsvMap.target        = target
        tsvMap.createID      = createID
        tsvMap.createDate    = createDate
        tsvMap.changeID      = changeID
        tsvMap.changeDate    = changeDate
        tsvMap.comment       = comment
        tsvMap.segmentID     = segmentID
        tsvMap.note          = note
        tsvMap.altUniq       = altUniq
        tsvMap.xStatus       = xStatus
        tsvMap.origin        = origin

        // Add a new line to the StringWriter with the collected data from the processed segment
        collectData()
        count++
	}
}

if (count > 0) {
    tsvStringData = tsvData.toString()
    // Some cleanup on the collected data
    tsvStringData = tsvStringData.replaceAll(/\u240B\n/, "\n")
    tsvStringData = tsvStringData.replaceAll(/\u240B/, "\t")
    tsvStringData = tsvStringData.replaceAll(/\u2007/, "")
    projectname = new File(prop.getProjectRoot()).getName().toString()
    tsvFileName = projectname + " ($srcCode - $tgtCode).tsv"
    folder = prop.projectRoot+'script_output'+File.separator
    tsvFile = new File(folder+tsvFileName)

    // Create output folder if it doesn't exist
    if (! (new File (folder)).exists()) {
            (new File(folder)).mkdir()
    }
    tsvFile.write(tsvStringData, "UTF-8")
    message=utils.format(resBundle("message", message), count, tsvFile)
    echo(message)
    if (autoopen in ["folder", "file"]) {
        autoopen = evaluate("${autoopen}")
        echo("Opening " + autoopen)
        Desktop.getDesktop().open(new File(autoopen.toString()))
    }
    if (!cli) {
        mainWindow.statusLabel.setText(message)
        Timer timer = new Timer().schedule({mainWindow.statusLabel.setText(null); console.clear()} as TimerTask, 10000)
    }
}
tsvData.flush()
tsvData.close()
return
