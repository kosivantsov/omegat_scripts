/* :name=       Write Project (XLIFF) to Excel :description=
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
 *              It requires the JExcel library (http://jexcelapi.sourceforge.net/)
 *              in your scripts/ folder
 * 
 * @author:     Kos Ivantsov, Briac Pilpre
 * @date:       2019-06-21
 * @latest:	 2021-06-24
 * @version:    0.5
 */
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

import org.omegat.core.Core
import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import org.omegat.util.StringUtil

includeSegmentId = true
includeCreatedId = true
includeChangedId = true
includeNotes     = true

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
segmentID="Segment ID"
createdID="Created"
changedID="Changed"
n_a = "N/A"
note = "Note"
message="{0} segments written to {1}"

resBundle = { k,v ->
    try {
        v = res.getString(k)
    }
    catch (MissingResourceException e) {
        v
    }
}

@Grab(group='net.sourceforge.jexcelapi', module='jxl', version='2.6.12')
// jxlFile = new FileNameByRegexFinder().getFileNames(Preferences.getPreferenceDefault(Preferences.SCRIPTS_DIRECTORY, "."), /jxl.*\.jar/)
// def jxlJar  = new File(jxlFile[0]).toURI().toURL()
// Thread.currentThread().getContextClassLoader().addURL(jxlJar)
def Alignment          = Class.forName('jxl.format.Alignment')
def Border             = Class.forName('jxl.format.Border')
def BorderLineStyle    = Class.forName('jxl.format.BorderLineStyle')
def Colour             = Class.forName('jxl.format.Colour')
def Label              = Class.forName('jxl.write.Label')
def VerticalAlignment  = Class.forName('jxl.format.VerticalAlignment')
def Workbook           = Class.forName('jxl.Workbook')
def WorkbookSettings   = Class.forName('jxl.WorkbookSettings')
def WritableCellFormat = Class.forName('jxl.write.WritableCellFormat')
def WritableFont       = Class.forName('jxl.write.WritableFont')
def WritableHyperlink  = Class.forName('jxl.write.WritableHyperlink')

def prop = project.projectProperties
if (!prop)
    {
        final def title = resBundle("msgTitle", msgTitle)
        final def msg   = resBundle("msgNoProject", msgNoProject)
        showMessageDialog null, msg, title, INFORMATION_MESSAGE
        return
    }

utils = (StringUtil.getMethods().toString().findAll("format")) ? StringUtil : StaticUtils

srcCode = project.projectProperties.sourceLanguage.languageCode
tgtCode = project.projectProperties.targetLanguage.languageCode
projectname = new File(prop.getProjectRoot()).getName().toString()
xlsfilename = projectname + " ($srcCode - $tgtCode).xls"
console.println projectname
def folder = prop.projectRoot+'script_output'+File.separator
table_file = new File(folder+xlsfilename)

// create folder if it doesn't exist
if (! (new File (folder)).exists()) {
        (new File(folder)).mkdir()
}

def wbs = WorkbookSettings.newInstance()
wbs.setDrawingsDisabled(true)

def fos = new FileOutputStream(table_file)
def w = Workbook.createWorkbook(fos, wbs)
def mastersheet = w.createSheet(resBundle("masterSheet", masterSheet), 0)

boldFormat = WritableCellFormat.newInstance()
boldFormat.setFont(WritableFont.newInstance(WritableFont.ARIAL, 12, WritableFont.BOLD))
boldFormat.setAlignment(Alignment.CENTRE)
boldsFormat = WritableCellFormat.newInstance()
boldsFormat.setFont(WritableFont.newInstance(WritableFont.ARIAL, 10, WritableFont.BOLD))

segFormat = WritableCellFormat.newInstance()
segFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
segFormat.setAlignment(Alignment.RIGHT)
segFormat.setBorder(Border.LEFT, BorderLineStyle.THIN)
segFormat.setBorder(Border.TOP, BorderLineStyle.THIN)
segFormat.setBorder(Border.BOTTOM, BorderLineStyle.THIN)
segFormat.setBackground(Colour.GRAY_25)

sourceFormat = WritableCellFormat.newInstance()
sourceFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
sourceFormat.setBorder(Border.TOP, BorderLineStyle.THIN)
sourceFormat.setBorder(Border.RIGHT, BorderLineStyle.THIN)
sourceFormat.setWrap(true)

targetFormat = WritableCellFormat.newInstance()
targetFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
targetFormat.setBorder(Border.BOTTOM, BorderLineStyle.THIN)
targetFormat.setBorder(Border.RIGHT, BorderLineStyle.THIN)
targetFormat.setWrap(true)

fileFormat = WritableCellFormat.newInstance()
fileFormat.setVerticalAlignment(VerticalAlignment.CENTRE)
fileFormat.setWrap(true)

//files = project.projectFiles.subList(editor.@displayedFileIndex, editor.@displayedFileIndex + 1)
def segments = 0
def sheetcount = 0
files = project.projectFiles
for (i in 0 ..< files.size())
{
    fi = files[i]
    curfilename = fi.filePath.toString()
    sheetname = (sheetcount+1).toString()
    sheet = w.createSheet(sheetname, sheetcount + 1)
    
    wh = WritableHyperlink.newInstance(0, sheetcount + 2, 2, 0, "", sheet, 1, 0, 0, 0)
    mastersheet.addHyperlink(wh)
    mastersheet.setColumnView(0, 100)
    mastersheet.addCell(Label.newInstance(0, 0, projectname + " ($srcCode > $tgtCode)", boldFormat))
    mastersheet.addCell(Label.newInstance(0, 1, utils.format(resBundle("projFiles", projFiles), files.size().toString()), boldsFormat))
    mastersheet.addCell(Label.newInstance(1, 1, resBundle("sheetName", sheetName), boldsFormat))
    mastersheet.addCell(Label.newInstance(0, sheetcount + 2, curfilename, fileFormat))
    mastersheet.addCell(Label.newInstance(1, sheetcount + 2, sheetname, fileFormat))

    mh = WritableHyperlink.newInstance(1, 0, 0, 0, "", mastersheet, 0, sheetcount + 2, 0, 0)
    sheet.addHyperlink(mh)

    def headerNum = 0
    sheet.setColumnView(headerNum, 10)
    headerNum++
    sheet.setColumnView(headerNum, 100)
    headerNum++
    sheet.setColumnView(2, 100)
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
    sheet.addCell(Label.newInstance(0, 0, resBundle("segmentNumber", segmentNumber), boldsFormat))
    sheet.addCell(Label.newInstance(1, 0, curfilename, boldFormat))
    sheet.mergeCells(1, 0, 4, 0)
    columnNum = 1
    sheet.addCell(Label.newInstance(columnNum, 1, srcCode, boldFormat))
    columnNum++
    sheet.addCell(Label.newInstance(columnNum, 1, tgtCode, boldFormat))
    columnNum++
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
        ste = fi.entries[j]
        info = project.getTranslationInfo(ste)
        def changeId = info.changer
        def changeDate = info.changeDate
        def creationId = info.creator
        def creationDate = info.creationDate
        seg_num = ste.entryNum().toString()
        segmentId = ste.key.id ? ste.key.id : resBundle("n_a", n_a)
        source = ste.getSrcText()
        target = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null
        if (target != null && target.size() == 0 )
        {
            target = resBundle("emptyTrans", emptyTrans)
        }
        sheet.addCell(Label.newInstance(0, count + 1, seg_num))
        //~ sheet.mergeCells(0, count + 1, 0, count + 2)
        columnNum = 1
        sheet.addCell(Label.newInstance(columnNum, count + 1, source))
        columnNum++
        sheet.addCell(Label.newInstance(columnNum, count + 1, target))
        columnNum++
        if (includeSegmentId) {
            sheet.addCell(Label.newInstance(columnNum, count + 1, segmentId))
            columnNum++
        }
        if (includeCreatedId) {
            sheet.addCell(Label.newInstance(columnNum, count + 1, creationId ))
            columnNum++
        }
        if (includeChangedId) {
            sheet.addCell(Label.newInstance(columnNum, count + 1, changeId))
            columnNum++
        }
        if (includeNotes) {
            sheet.addCell(Label.newInstance(columnNum, count + 1, info.note))
            columnNum++
        }
        //~ sheet.addCell(Label.newInstance(1, count + 3, ''))
        count = count + 1
        segments++
    }
}

w.write()
w.close()
fos.flush()
fos.close()
message=utils.format(resBundle("message", message), segments, table_file)
//mainWindow.statusLabel.setText(message)
console.println(message)
//Timer timer = new Timer().schedule({mainWindow.statusLabel.setText(null); console.clear()} as TimerTask, 10000)
