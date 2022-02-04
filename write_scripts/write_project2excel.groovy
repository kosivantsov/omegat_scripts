/* :name=       Write Project to Excel :description=
 * 
 * 
 * 
 *              THIS SCRIPT WAS SPONSORED BY
 *                 ==   cApStAn sprl   ==
 *                     www.capstan.be
 * 
 * 
 * 
 * #Purpose:    Export the current file into an Excel file containing
 *              one sheet per each source file.
 * #Files:      Writes Excel file containing one sheet
 *              in the 'script_output' subfolder
 *              of current project's root. Each source file is exported to a separate
 *              sheet. The Excel file also contains a Master Sheet with links
 *              to all other sheets to simplify navigation.
 *              It requires the JExcel library (http://jexcelapi.sourceforge.net/),
 *              which the script fetches from the Internet
 *              
 * 
 * @author:     Kos Ivantsov, Briac Pilpre
 * @date:       2019-06-21
 * @latest:     2022-02-03
 * @version:    1.2
 */
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

import java.awt.Desktop
import org.omegat.core.Core
import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import org.omegat.util.StringUtil

//Script options
autoopen         = "none"   //Automatically open the table file upon creation ("folder"|"spreadsheet"|"none")
includeSegmentId = true     //Add a column for segment ID
includeCreatedId = true     //Add a column for the original author
includeChangedId = true     //Add a column for the author of changes
includeNotes     = true     //Add a column for segment notes
includeExtraCol  = true     //Add a column with info about uniqness and alternative translation
fillEmptTran     = true     //Add custom string to empty translations, i.e. where translation is INTENTIONALLY set to empty (true|false)
markNonUniq      = true     //Add color background to non-unique segments
markAlt          = true     //Add color borders to segments with alternative translation
markPara         = true     //First segments in paragraphs will have a different color for the top border
uniqStr          = ""       //Extra cell text for uniq segments
firstStr         = "1"      //Extra cell text for the 1st occurance of a repeated segment
repStr           = "+"      //Extra cell text for further occurances of a repeated segment
altStr           = "a"      //Extra cell text for alternative translation of the segmnent
defStr           = ""       //Extra cell text for default translation of the segmnent
notTranslated    = "NT"     //Extra cell text for segments with no translation (NOT empty, but untranslated)

//Color Settings
/*
=== Available colors: ===
AQUA, AUTOMATIC, BLACK, BLUE, BLUE_GREY, BLUE2,
BRIGHT_GREEN, BROWN, CORAL, DARK_BLUE, DARK_BLUE2,
DARK_GREEN, DARK_PURPLE, DARK_RED, DARK_RED2, DARK_TEAL,
DARK_YELLOW, DEFAULT_BACKGROUND, DEFAULT_BACKGROUND1,
GOLD, GRAY_25, GRAY_50, GRAY_80, GREEN,
GREY_25_PERCENT, GREY_40_PERCENT, GREY_50_PERCENT, GREY_80_PERCENT,
ICE_BLUE, INDIGO, IVORY, LAVENDER, LIGHT_BLUE, LIGHT_GREEN,
LIGHT_ORANGE, LIGHT_TURQUOISE, LIGHT_TURQUOISE2, LIME,
OCEAN_BLUE, OLIVE_GREEN, ORANGE, PALE_BLUE, PALETTE_BLACK,
PERIWINKLE, PINK, PINK2, PLUM, PLUM2, RED, ROSE, SEA_GREEN,
SKY_BLUE, TAN, TEAL, TEAL2, TURQOISE2, TURQUOISE, UNKNOWN,
VERY_LIGHT_YELLOW, VIOLET, VIOLET2, WHITE, YELLOW, YELLOW2
=========================
*/
headerFontColor    = "IVORY"               //Font color for the first row on each sheet
headerBgColor      = "GRAY_80"             //Background color for the first row
colHeaderFontColor = "BLACK"               //Font color for column headers
colHeaderBgColor   = "GRAY_50"             //Background color for column headers
segNumFontColor    = "BLACK"               //Font color for segment numbers
segNumBgColor      = "GRAY_50"             //Background color for for segment numbers
baseFontColor      = "BLACK"               //Font color for source and text segments
repeatBgColor      = "LIGHT_TURQUOISE2"    //Background color for repeated segments
noteBgColor        = "VERY_LIGHT_YELLOW"   //Background color for notes
altFontColor       = "DARK_TEAL"           //Font color for segments with alternative translations
altBgColor         = "TEAL2"               //Background color for alternative mark

//UI Strings
name="Write Excel Table for Revision (multiple sheets)"
description="Write Excel table with individual sheets per each source file"
msgTitle="Export Project to Excel Table"
msgNoProject="Please try again after you open a project."
projFiles="Project Files ({0})"
masterSheet="Master Sheet"
segmentNumber="Segment #"
sheetName="Sheet Name"
emptyTrans="<EMPTY>"
extraColStr = "Alt/Uniq"
segmentID="Segment ID"
createdID="Created"
changedID="Changed"
n_a = "N/A"
note = "Note"
message="{0} segments written to {1}"

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

//External resources hack (use hardcoded strings if .properties file isn't found)
resBundle = { k,v ->
    try {
        v = res.getString(k)
    }
    catch (MissingResourceException e) {
        v
    }
}

//Get the remote lib and assign some classes for easy reference
@Grab(group='net.sourceforge.jexcelapi', module='jxl', version='2.6.12')
def Alignment          = Class.forName('jxl.format.Alignment')
def Border             = Class.forName('jxl.format.Border')
def BorderLineStyle    = Class.forName('jxl.format.BorderLineStyle')
def Colour             = Class.forName('jxl.format.Colour')
def Label              = Class.forName('jxl.write.Label')
def Pattern            = Class.forName('jxl.format.Pattern')
def UnderlineStyle 	   = Class.forName('jxl.format.UnderlineStyle')
def VerticalAlignment  = Class.forName('jxl.format.VerticalAlignment')
def Workbook           = Class.forName('jxl.Workbook')
def WorkbookSettings   = Class.forName('jxl.WorkbookSettings')
def WritableCellFormat = Class.forName('jxl.write.WritableCellFormat')
def WritableFont       = Class.forName('jxl.write.WritableFont')
def WritableHyperlink  = Class.forName('jxl.write.WritableHyperlink')

//Check for if a project is open
def prop = project.projectProperties
if (!prop) {
    final def title = resBundle("msgTitle", msgTitle)
    final def msg   = resBundle("msgNoProject", msgNoProject)
    showMessageDialog null, msg, title, INFORMATION_MESSAGE
    return
}

//Make sure this script could work with older version of OmegaT
utils = (StringUtil.getMethods().toString().findAll("format")) ? StringUtil : StaticUtils

//Get language code and set the filename
srcCode = project.projectProperties.sourceLanguage.languageCode
tgtCode = project.projectProperties.targetLanguage.languageCode
projectname = new File(prop.getProjectRoot()).getName().toString()
xlsfilename = projectname + " ($srcCode - $tgtCode).xls"
folder = prop.projectRoot+'script_output'+File.separator
spreadsheet = new File(folder+xlsfilename)

// create folder if it doesn't exist
if (! (new File (folder)).exists()) {
        (new File(folder)).mkdir()
}

//Header formatting for each sheet
headerFormat = WritableCellFormat.newInstance() //headers in each worksheet
headerFormat.setFont(WritableFont.newInstance(WritableFont.ARIAL, 15, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour."$headerFontColor"))
headerFormat.setAlignment(Alignment.CENTRE)
headerFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
headerFormat.setWrap(true)
headerFormat.setShrinkToFit(true)
headerFormat.setBackground(Colour."$headerBgColor")

//Bigger bold text
boldFormat = WritableCellFormat.newInstance()
boldFormat.setFont(WritableFont.newInstance(WritableFont.ARIAL, 12, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour."$colHeaderFontColor"))
boldFormat.setAlignment(Alignment.CENTRE)
boldFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
boldFormat.setWrap(true)
boldFormat.setShrinkToFit(true)
boldFormat.setBackground(Colour."$colHeaderBgColor")

//Smaller bold text
boldsFormat = WritableCellFormat.newInstance() 
boldsFormat.setFont(WritableFont.newInstance(WritableFont.ARIAL, 9, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour."$colHeaderFontColor"))
boldsFormat.setAlignment(Alignment.LEFT)
boldsFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
boldsFormat.setWrap(true)
boldsFormat.setShrinkToFit(true)
boldsFormat.setBackground(Colour."$colHeaderBgColor")

//Segment number formatting
segFormat = WritableCellFormat.newInstance()
segFormat.setFont(WritableFont.newInstance(WritableFont.ARIAL, 9, WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour."$segNumFontColor"))
segFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
segFormat.setAlignment(Alignment.RIGHT)
segFormat.setBackground(Colour."$segNumBgColor")

//Filenames formatting
fileFormat = WritableCellFormat.newInstance()
fileFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
fileFormat.setWrap(true)

//Base formatting for source and target text. It will be expanded according to the segment specifics
textFormat = WritableCellFormat.newInstance()
textFormat.setFont(WritableFont.newInstance(WritableFont.ARIAL, 11, WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour."$baseFontColor"))
textFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
textFormat.setAlignment(Alignment.LEFT)
textFormat.setWrap(true)
textFormat.setIndentation(1)

//Metainfo formatting
metaFormat = WritableCellFormat.newInstance()
metaFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
metaFormat.setAlignment(Alignment.LEFT)
metaFormat.setWrap(true)

//Excel magic, create workbook, then masterSheet (the first sheet with links), and a sheet for each project file
def wbs = WorkbookSettings.newInstance()
wbs.setDrawingsDisabled(true)

def fos = new FileOutputStream(spreadsheet)
def w = Workbook.createWorkbook(fos, wbs)
def mastersheet = w.createSheet(resBundle("masterSheet", masterSheet), 0)

//And now this masterSheet and other sheets are going to be populated
def segments = 0
def sheetcount = 0

//Get the number of additional rows to properly merge worksheet headers
def filenameLastCell = 2
if (includeExtraCol)
    filenameLastCell++
if (includeSegmentId)
    filenameLastCell++
if (includeCreatedId)
    filenameLastCell++
if (includeChangedId)
    filenameLastCell++
if (includeNotes)
    filenameLastCell++

//Now go through the files and enties and collect data to populate sheets
files = project.projectFiles
for (i in 0 ..< files.size())
{
    fi = files[i]
    curfilename = fi.filePath.toString()
    sheetname = (sheetcount+1).toString()
    
    //Get the longest string in source and target to set their column widths
    //    (so it's not to long for files with only short segments)
    def sourceLength = []
    def targetLength = []
    fi.entries.each {
        it.getSrcText().tokenize("\n").each {
            sourceLength.add(it.size())
        }
        translation = project.getTranslationInfo(it) ? project.getTranslationInfo(it).translation : null
        translation.toString().tokenize("\n").each {
            targetLength.add(it.size())
        }
    }
    returnLongest = { k ->
        length = k.sort()[k.size()-1]
        if (length > 100) {
            length = 100
        }
        return length
    }
    def sourceWidth = returnLongest(sourceLength)
    def targetWidth = returnLongest(targetLength)

    sheet = w.createSheet(sheetname, sheetcount + 1)
   
    wh = WritableHyperlink.newInstance(0, sheetcount + 2, 2, 0, "", sheet, 1, 0, 0, 0)
    mastersheet.addHyperlink(wh)
    mastersheet.setColumnView(0, 100)
    mastersheet.setRowView(0, 480)
    mastersheet.setColumnView(1, resBundle("sheetName", sheetName).size()+1)
    mastersheet.addCell(Label.newInstance(0, 0, projectname + " ($srcCode > $tgtCode)", headerFormat))
    mastersheet.mergeCells(0, 0, 1, 0)
    mastersheet.addCell(Label.newInstance(0, 1, utils.format(resBundle("projFiles", projFiles), files.size().toString()), boldsFormat))
    mastersheet.addCell(Label.newInstance(1, 1, resBundle("sheetName", sheetName), boldsFormat))
    mastersheet.addCell(Label.newInstance(0, sheetcount + 2, curfilename, fileFormat))
    mastersheet.addCell(Label.newInstance(1, sheetcount + 2, sheetname, fileFormat))

    mh = WritableHyperlink.newInstance(1, 0, 0, 0, "", mastersheet, 0, sheetcount + 2, 0, 0)
    sheet.addHyperlink(mh)

    def headerNum = 0
    sheet.setColumnView(headerNum, resBundle("segmentNumber", segmentNumber).size() + 1)
    headerNum++
    sheet.setColumnView(headerNum, sourceWidth + 5)
    headerNum++
    sheet.setColumnView(headerNum, targetWidth + 5)
    if (includeExtraCol) {
        headerNum++
        sheet.setColumnView(headerNum, resBundle("extraColStr", extraColStr).size() + 5)
    }
    if (includeSegmentId) {    
        headerNum++
        sheet.setColumnView(headerNum, 20)
    }
    if (includeCreatedId) {    
        headerNum++
        sheet.setColumnView(headerNum, 20)
    }
    if (includeChangedId) {    
        headerNum++
        sheet.setColumnView(headerNum, 20)
    }
    if (includeNotes) {
        headerNum++
        sheet.setColumnView(headerNum, 50)
    }
    sheet.addCell(Label.newInstance(0, 0, curfilename, headerFormat))
    sheet.mergeCells(0, 0, 2, 0)
    if (filenameLastCell > 2) {
        sheet.addCell(Label.newInstance(3, 0, "", headerFormat))
        sheet.mergeCells(3, 0, filenameLastCell, 0)
    }
    sheet.setRowView(0, 480)
    columnNum = 0
    sheet.addCell(Label.newInstance(columnNum, 1, resBundle("segmentNumber", segmentNumber), boldsFormat))
    columnNum++
    sheet.addCell(Label.newInstance(columnNum, 1, srcCode, boldFormat))
    columnNum++
    sheet.addCell(Label.newInstance(columnNum, 1, tgtCode, boldFormat))
    columnNum++
    if (includeExtraCol) {
        sheet.addCell(Label.newInstance(columnNum, 1, resBundle("extraColStr", extraColStr), boldFormat))
        columnNum++
    }
    if (includeSegmentId) {
        sheet.addCell(Label.newInstance(columnNum, 1, resBundle("segmentID", segmentID), boldFormat))
        columnNum++
    }
    if (includeCreatedId) {
        sheet.addCell(Label.newInstance(columnNum, 1, resBundle("createdID", createdID), boldFormat))
        columnNum++
    }
    if (includeChangedId) {
        sheet.addCell(Label.newInstance(columnNum, 1, resBundle("changedID", changedID), boldFormat))
        columnNum++
    }
    if (includeNotes) {
        sheet.addCell(Label.newInstance(columnNum, 1, resBundle("note", note), boldFormat))
        columnNum++
    }
    sheetcount++
    count = 1
    for (j in 0 ..< fi.entries.size())
    {
        def finalTextFormat = WritableCellFormat.newInstance(textFormat)
        def extraFormat = WritableCellFormat.newInstance(metaFormat)
        def extraCont = ""
        ste = fi.entries[j]
        info = project.getTranslationInfo(ste)
        def changeId = info.changer
        def changeDate = info.changeDate
        def creationId = info.creator
        def creationDate = info.creationDate
        def isDup = ste.getDuplicate()
        def isAlt = info.defaultTranslation ? defStr : altStr
        def newPar = ste.paragraphStart ? ste.paragraphStart.toString() : null
        if (newPar) {
            finalTextFormat.setBorder(Border.TOP, BorderLineStyle.THIN)
        }
        def seg_num = ste.entryNum().toString()
        def segmentId = ste.key.id ? ste.key.id : resBundle("n_a", n_a)
        def source = ste.getSrcText()
        def target = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null
        if (target != null && target.size() == 0 )
        {
            target = fillEmptTran ? resBundle("emptyTrans", emptyTrans) : ""
        }
        if (!target) {
            extraCont = "$notTranslated "
        }
        if (isDup.toString() != 'NONE' && markNonUniq) {
            finalTextFormat.setBackground(Colour."$repeatBgColor")
        }
        if (isAlt == altStr && markAlt) {
            finalTextFormat.setBorder(Border.RIGHT, BorderLineStyle.THICK)
            finalTextFormat.setBorder(Border.LEFT, BorderLineStyle.THICK)
            finalTextFormat.setFont(WritableFont.newInstance(WritableFont.ARIAL, 11, WritableFont.BOLD, true, UnderlineStyle.NO_UNDERLINE, Colour."$altFontColor"))
            extraFormat.setBackground(Colour."$altBgColor")
        }
        isDup = isDup.toString().toString().replaceAll(/NONE/, uniqStr).replaceAll(/FIRST/, firstStr).replaceAll(/NEXT/, repStr)
        extraCont = extraCont + "$isDup $isAlt"

        sheet.addCell(Label.newInstance(0, count + 1, seg_num, segFormat))
        columnNum = 1
        sheet.addCell(Label.newInstance(columnNum, count + 1, source, finalTextFormat))
        columnNum++
        sheet.addCell(Label.newInstance(columnNum, count + 1, target, finalTextFormat))
        columnNum++
        if (includeExtraCol) {
            if (extraCont != "") {
                extraFormat.setAlignment(Alignment.CENTRE)
            }
            sheet.addCell(Label.newInstance(columnNum, count + 1, extraCont, extraFormat))
            columnNum++
        }
        if (includeSegmentId) {
            sheet.addCell(Label.newInstance(columnNum, count + 1, segmentId, metaFormat))
            columnNum++
        }
        if (includeCreatedId) {
            sheet.addCell(Label.newInstance(columnNum, count + 1, creationId, metaFormat))
            columnNum++
        }
        if (includeChangedId) {
            sheet.addCell(Label.newInstance(columnNum, count + 1, changeId, metaFormat))
            columnNum++
        }
        if (includeNotes) {
        noteFormat = WritableCellFormat.newInstance(metaFormat)
        info.note ? noteFormat.setBackground(Colour."$noteBgColor") : ""
            sheet.addCell(Label.newInstance(columnNum, count + 1, info.note, noteFormat))
            columnNum++
        }
        count = count + 1
        segments++
    }
}

w.write()
w.close()
fos.flush()
fos.close()
message=utils.format(resBundle("message", message), segments, spreadsheet)
echo(message)
if (autoopen in ["folder", "spreadsheet"]) {
    autoopen = evaluate("${autoopen}")
    echo("Opening " + autoopen)
    Desktop.getDesktop().open(new File(autoopen.toString()))
}
if (!cli) {
    mainWindow.statusLabel.setText(message)
    Timer timer = new Timer().schedule({mainWindow.statusLabel.setText(null); console.clear()} as TimerTask, 10000)
}
