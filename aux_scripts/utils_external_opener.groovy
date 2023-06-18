/* :name=Utils - External Openers :description=
 * Zenity launcher for external programs and scripts
 * 
 * @author:  Kos Ivantsov
 * @date:    2023-06-17
 * @version: 0.3
 * @history: v.0.1 2013-03-15
 *           v.0.2 2017-06-01
 */

import javax.swing.JOptionPane
import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import java.util.Locale

// Warning dialogs
showDialog = {title, msg ->
    JOptionPane.showMessageDialog(null, msg, title,
    JOptionPane.INFORMATION_MESSAGE)
}
// Dialog title. It remains the same, only the message changes each time.
title = "External openers"

// gui() executes even if there are things that would terminate the script. If `exit` is true, gui() will terminate too 
exit = false

// Check the OS, exit if Windows. Maybe one day it'll work on Windows too, but not now
def os = System.getProperty('os.name').toLowerCase()
switch (os) {
    case ~/.*win.*/:
        // Windows
        msg = "Windows is not supported at the moment"
        showDialog(title, msg)
        exit = true
        return
        break
    case ~/.*nix.*/:
    case ~/.*nux.*/:
    case ~/.*sunos.*/:
    case ~/.*freebsd.*/:
    case ~/.*mac.*/:
        msg = "The script is running on ${System.getProperty("os.name")}"
        console.println(msg)
        break
    default:
        msg = "The script is running on ${System.getProperty("os.name")}"
        console.println(msg)
        break
}

// Exit if no project is open
prop = project.getProjectProperties()
if (prop == null) {
    msg = "Open a project and try again"
    showDialog(title, msg)
    exit = true
    return
}

// A small thingy to url-encode strings. We'll do it for source, target and selection, and will provide them in plain and encoded format
urlencode ={ string ->
    encoded = new java.net.URI(null, null, null, -1, string, null, null)
    encoded = encoded.toASCIIString()
    encoded = encoded.replaceAll("\"", "%22")
                     .replaceAll("&", "%26")
                     .replaceAll("'", "%27")
    return encoded
}

def gui() {
// If any of the above checks changed `exit` to true, terminate
    if (exit) {
        return
    }
// Location of the external launcher and the file with variables
// If these are edited, the external counterpart might need to be amended as well
    def openerFolder = StaticUtils.getConfigDir() + "external-openers" + File.separator
    if (! new File(openerFolder).exists()) {
        msg="<html>The folder <b>$openerFolder</b> is not found.<br/>The script will not continue</html>"
        showDialog(title, msg)
        return
    }
    def opener = openerFolder + "opener"
    if (! new File(opener).exists()) {
        msg="<html> The executable file <b>$opener</b> is not found.<br/>The script will not continue</html>"
        showDialog(title, msg)
        return
    }
    def varfile = new File(openerFolder + "opener.vars")
// here we export text
    def entry = editor.currentEntry
    def sourcetext = entry ? entry.getSrcText() : ""
    def targettext = editor.getCurrentTranslation() ? editor.getCurrentTranslation() : ""
    def selection = editor.selectedText ? editor.selectedText : sourcetext
    def urlselection = urlencode(selection)
    def urlsource = urlencode(sourcetext)
    def urltarget = urlencode(targettext)
// paths to different folders
    def projroot = prop.getProjectRoot()
    def projint = prop.getProjectInternal()
    def srcroot = prop.getSourceRoot()
    def targroot = prop.getTargetRoot()
    def tmroot = prop.getTMRoot()
    def glosroot = prop.getGlossaryRoot()
    def dictroot = prop.getDictRoot()
    def configdir = StaticUtils.getConfigDir()
    def scriptsDir = new File(Preferences.getPreference(Preferences.SCRIPTS_DIRECTORY))
// paths to files
    def curfile = prop.getSourceRoot() + editor.getCurrentFile()
    def writglos = prop.getWriteableGlossary()
    def binary = System.getProperty("sun.java.command").split(' ')[0]
// Project's languages
    def srclang = prop.getSourceLanguage()
    def targlang = prop.getTargetLanguage()
    def srclangcode = srclang.getLanguageCode()
    def targlangcode = targlang.getLanguageCode()
    def srccntrcode = srclang.getCountryCode()
    def targcntrcode = targlang.getCountryCode()
    def srclanghum = srclang.getDisplayName()
    def targlanghum = targlang.getDisplayName()
// OmegaT's locale
    def locale = Locale.getDefault() // Java VM's default locale.
    def language = locale.getLanguage()
    def country = locale.getCountry()
// write the file that holds the passed variables
    varfile.write("""\
selection=\'$selection\'
urlselection=\'$urlselection\'
targettext=\'$targettext\'
urltarget=\'$urltarget\'
sourcetext=\'$sourcetext\'
urlsource=\'$urlsource\'
projroot=\'$projroot\'
projint=\'$projint\'
srcroot=\'$srcroot\'
targroot=\'$targroot\'
tmroot=\'$tmroot\'
glosroot=\'$glosroot\'
dictroot=\'$dictroot\'
curfile=\'$curfile\'
writglos=\'$writglos\'
configdir=\'$configdir\'
srclangcode=\'$srclangcode\'
targlangcode=\'$targlangcode\'
srccntrcode=\'$srccntrcode\'
targcntrcode=\'$targcntrcode\'
srclanghum=\'$srclanghum\'
targlanghum=\'$targlanghum\'
language=\'$language\'
locale=\'$locale\'
country=\'$country\'
binary=\'$binary\'
scriptDir=\'$scriptsDir\'\
""", "UTF-8")
// run the opener and pass the varfile 
    [opener, varfile.toString()].execute()
}
return