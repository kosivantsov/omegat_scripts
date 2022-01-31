/* :name =    Write Table with Repeat/Alternative Marks :description=Exports the current file
 *            to HTML table
 * 
 * #Purpose:  Export current file into a HTML table
 * 
 * #Files:    Writes 'FILENAME.html' (filename is based on
 *            the actual current file name or the project folder name)
 *            in the 'script_output' subfolder
 *            The table will be created in this format:
 *            +--------------+--------------+--------------+
 *            |  Repeat/Alt  |  Source  LN  |  Target  LN  |
 *            +--------------+--------------+--------------+
 * 
 * @author:   Kos Ivantsov
 * @date:     2022-01-31
 * @version:  0.4.1
 */
import java.awt.Desktop
import org.omegat.core.Core
import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import org.omegat.util.StringUtil

import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

//// Script Options
all_project = true      //Set to true to export all project, only the current file will be exported
autoopen    = "none"    //Automatically open the table file upon creation ("folder"|"table"|"none")
skipUntran  = true      //Skip untranslated segments (true|false)
fillEmptTran= false     //Add custom string to empty translations, i.e. where translation is INTENTIONALLY set to empty (true|false)
EmptTranTxt = "<EMPTY>" //String to replace empty translations (in quotes, ignored if above is false)
markNonUniq = true      //Add color background to non-unique segments
markAlt     = true      //Add color frame around segments with alternative translation
addExtraCol = true      //Add column with info about uniqness and alternative translation
extraColStr = "XX"      //Uniq column caption (in quotes, ignored if above is false)
uniqStr     = ""        //Cell mark for uniq segments
firstStr    = "1"       //Cell mark for the 1st occurance of a repeated segment
repStr      = "+"       //Cell mark for further occurances of a repeated segment
altStr      = "a"       //Cell mark fo alternative translation of the segmnent
defStr      = ""        //Cell mark fo default translation of the segmnent
markFiles   = true      //First segments in files will have a different color for the top border
markPara    = true      //First segments in paragraphs will have a different color for the top border

//// Colors
normfg    = '#000000'   //foreground
curtagbg  = '#BCC6DD'   //tag background in segment's text
curtagfg  = 'green'     //tag foreground in segment's text
curtxtbg  = '#BCC6CC'   //font background in segment's text
nonuniqfg = '#595959'   //foreground for non-uniq segments
nonuniqbg = '#F4F6F7'   //background for non-uniq segments
alttxtbg  = '#C73A3A'   //background for alternative translations
fileborder= '#210620'   //border color to mark new files
paraborder= '#aab7b8'   //border color to mark paragraphs

//// UI Strings
name = "Export project to table"
noProject = "Please try again after you open a project."
countWritten = "{0} segments written to {1}."
tableWritten = "Table {0} created. {1} segments written."
noTable = "Nothing to export. {0} wasn't created."

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

//// Try to use external resources, or switch to the hardcoded strings
resBundle = { k,v ->
    try {
        v = res.getString(k)
    }
    catch (MissingResourceException e){
        v
    }
}

//// Here goes nothing, let's have some fun!
String.metaClass.confirm = { ->
    showConfirmDialog null, delegate, title, YES_NO_OPTION
}
String.metaClass.alert = { ->
    showMessageDialog null, delegate, title, INFORMATION_MESSAGE
    false
}

utils = (StringUtil.getMethods().toString().findAll("makeValidXML")) ? StringUtil : StaticUtils
if (!cli) {
    console.clear()
}
prop = project.projectProperties
if (!prop){
    title = resBundle("name", name)
    msg   = resBundle("noProject", noProject)
    msg.alert()
}

painttag = {x, tbg, tfg -> x.replaceAll(/(\&lt\;\/?\s?\w+\s?\/?\d+?\s?\/?\s?\/?\&gt\;)/, /\<sup\>\<font size=\"1\"  style=\"background-color:$tbg\; color:$tfg" \>$1\<\/font\>\<\/sup\>/)}

paintseg = {
    x, fg -> 
        x.replaceAll(/(^.*$)/, /\<font color=\"$fg\"\>$1\<\/font\>/)
}

srclang = prop.getSourceLanguage()
targlang = prop.getTargetLanguage()
folder = prop.projectRoot+'script_output/'

extraWidth  = addExtraCol  ? 2 : 0
srcWidth  = (100 - extraWidth) / 2
targWidth = srcWidth
srcHead   = """<td style=\"border:1px solid black\" width=\"$srcWidth%\">$srclang</td>"""
targHead  = """<td style=\"border:1px solid black\" width=\"$targWidth%\">$targlang</td>"""
extraHead  = addExtraCol ? """<td style=\"border:1px solid black\" width=\"$extraWidth%\">$extraColStr</td>""" : ""
count = 0

table_contents = new StringWriter()
table_contents << """\
<html>
<head>
<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">
<body>
  <table border=\"1px\" style=\"margin-bottom:50px\" width=\"100%\">
    <tr>
      $extraHead
      $srcHead
      $targHead
    </tr>\n
"""
if (all_project || cli) {
    files = project.projectFiles
    filename = new File(prop.getProjectRoot()).getName()
} else {
    files = project.projectFiles.subList(editor.@displayedFileIndex, editor.@displayedFileIndex + 1)
    filename = Core.getEditor().getCurrentFile().replaceAll("[:\\\\/*\"?|<>' ]", "_")
    markFiles = false
}
for (i in 0 ..< files.size()) {
    fi = files[i]
    for (j in 0 ..< fi.entries.size()) {
        def cellbg
        def altbg
        def border = "border:1px solid transparent"
        def ignore = false
        ste = fi.entries[j]
        info = project.getTranslationInfo(ste)
        def isDup = ste.getDuplicate()
        def isAlt = info.defaultTranslation ? defStr : altStr
        def newPar = ste.paragraphStart ? ste.paragraphStart.toString() : null
        source = ste.getSrcText().replaceAll("\n", /zzzspacezzz/)
        if (source ==~ /^(<\/?[a-z]+[0-9]* ?\/?>)+$/) {
            ignore = true
        }
        seg_num = ste.entryNum()
        target = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null;
        if (target == null){
        target = "zzznullzzz"
            if (skipUntran) {
                ignore = true
            }
        }
        if (target.size() == 0) {
            if (fillEmptTran) {
                target = EmptTranTxt
            } else {
                target = ""
            }
        }
        target = target.replaceAll("\n", /zzzspacezzz/)
        source = utils.makeValidXML(source).replaceAll(/zzzspacezzz/, /<br\/>/)
        source = painttag(source, curtagbg, curtagfg)
        target = utils.makeValidXML(target).replaceAll(/zzznullzzz/, /&#8288;/).replaceAll(/zzzspacezzz/, /<br\/>/)
        target = painttag(target, curtagbg, curtagfg)
        if (isDup.toString() != 'NONE' && markNonUniq) {
            source = (isDup.toString() == 'FIRST') ? paintseg(source, normfg) : paintseg(source, nonuniqfg)
            target = (isDup.toString() == 'FIRST') ? paintseg(target, normfg) : paintseg(target, nonuniqfg)
            cellbg = "bgcolor=$nonuniqbg"
        } else {
            source = paintseg(source, normfg)
            target = paintseg(target, normfg)
        }
        if (isAlt == altStr && markAlt) {
            altbg = "bgcolor=$alttxtbg"
        }
        if (newPar) {
            if (j == 0 && markFiles) {
                border = "border:1px solid transparent; border-top:5px solid $fileborder"
            } else {
                markPara ? border = "border:1px solid transparent; border-top:5px solid $paraborder" : ""
            }
        }
        if (! ignore) {
            isDup = isDup.toString().toString().replaceAll(/NONE/, uniqStr).replaceAll(/FIRST/, firstStr).replaceAll(/NEXT/, repStr)
            extraCont = "$isDup $isAlt"
            extraCell  = addExtraCol ? """<td style=\"$border\" $altbg width=\"$extraWidth%\">$extraCont</td>""" : ""
            table_contents << """
    <tr>
      $extraCell
      <td $cellbg style=\"$border\" width=\"$srcWidth%\">$source</td>
      <td $cellbg style=\"$border\" width=\"$targWidth%\">$target</td>
    </tr>"""
            count++
        }
    }

}
table_contents << "\n  </table>\n"
table_contents << "</body>\n</html>"

// create folder if it doesn't exist
if (! (new File (folder)).exists()){
    (new File(folder)).mkdir()
    }

table = new File(folder + filename + '.html')
if (count > 0) {
    table.write(table_contents.toString().replaceAll("\\n\\s+\\n", '\n').trim(), 'UTF-8')
    echo(utils.format(resBundle("countWritten", countWritten), count, table))
    if (!cli) {
        mainWindow.statusLabel.setText(utils.format(resBundle("tableWritten", tableWritten), table, count))
        Timer timer = new Timer().schedule({mainWindow.statusLabel.setText(null)} as TimerTask, 10000)
    }

    if (autoopen in ["folder", "table"]) {
        autoopen = evaluate("${autoopen}")
        echo("Opening " + autoopen)
        Desktop.getDesktop().open(new File(autoopen.toString()))
    }
} else {
    echo(utils.format(resBundle("noTable", noTable), table))
    if (!cli) {
        mainWindow.statusLabel.setText(utils.format(resBundle("noTable", noTable), table))
        Timer timer = new Timer().schedule({mainWindow.statusLabel.setText(null)} as TimerTask, 5000)
    }
}
return
