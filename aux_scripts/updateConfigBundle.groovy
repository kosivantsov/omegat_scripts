/* :name = Update Customisation Bundle :description =
 *  Update OmegaT customisation from a remote repository
 * 
 * @author:  Kos Ivantsov
 * @date:    2020-03-06
 * @version: 0.4.6
 */

def customUrl = "" //insert URL between quotes or set to "" (empty) to ask the user on the 1st run, don't comment out
autoLaunch = false // true for <application_startup> folder, false for the regular scripts folder

import org.omegat.core.events.IApplicationEventListener

import groovy.swing.SwingBuilder
import groovy.util.XmlSlurper
//import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.FlowLayout
import java.awt.GridBagConstraints as GBC
import java.awt.GridBagLayout as GBL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JProgressBar
import javax.swing.WindowConstants
import org.apache.commons.io.FileUtils
import org.omegat.CLIParameters
import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import org.omegat.util.StringUtil
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

utils = (StringUtil.getMethods().toString().findAll("makeValidXML")) ? StringUtil : StaticUtils

String.metaClass.confirm = { ->
    showConfirmDialog null, delegate, title, YES_NO_OPTION
}

String.metaClass.alert = { ->
    showMessageDialog null, delegate, title, INFORMATION_MESSAGE
}

def projectAlert
if (autoLaunch) {
    runChunks = System.getProperty("sun.java.command").split(/\s+/)
    params = []
    for (i in 0 .. runChunks.length-1){
        for (j in i .. runChunks.length-1){
            item = runChunks[i .. j].join(' ')
            params.add(item)
        }
    }
    params = params as String[]
    launchProj = CLIParameters.parseArgs(params).get(CLIParameters.PROJECT_DIR)
    if (launchProj) {
        message = """OmegaT was launched with a project to be opened.
The customisation update script will not be able to run.
Please restart the application without passing a project path to it."""
        projectAlert = true
    } else {
        projectAlert = false
    }
} else {
    if ( project.projectProperties ) {
        message = "Close the project and run this script again."
        projectAlert = true
    } else {
        projectAlert = false
    }
}

title = "Customisation Update"
if (projectAlert) {
    console.println(message)
    message.alert()
    return
} else {
    message = """No project.
Proceeding..."""
    console.println(message)
}

omtPid = Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0])
date = new Date().format("YYYYMMddHHmm")
def update = 0 //initial update bit
confUpd = 1 //update config
scrpUpd = 2 //update scripts
plugUpd = 4 //update plugins
def success = 0 // to quit OmT on config update
def noFinMsg
def incomplUpd = 0 // to check for incomplete updates due to wrong links to .zip files
def contScript = true // to loop the URL input dialog
def readOnlyJars = "" //list of installed jars in read-only /plugins
def nonInstallJars = "" //list of jars not to be installed because older version resides in read-only /plugins
def nonInstallNames = "" //same, but only names
def winDel = false
def openInstPlugDir = false
def deleteJars = """@echo off
title OmegaT Customisation Update
mode 52,5
echo Waiting for OmegaT to quit.
echo Do not close this window.
echo It will close automatically when OmegaT exits.
echo You may need to switch to OmegaT and press OK there.
:loop
tasklist | find " $omtPid " >nul
if not errorlevel 1 (
    timeout /t 2 >nul
    goto :loop
) else (
    cls
    echo OmegaT is not running. Deleting files...
""" // for cmd file to delete after the script is finished on Windows
def finalMsg = "" //to report mistakes
tmpDir = System.getProperty("java.io.tmpdir")
tmpConfigZip = new File(tmpDir + File.separator + "config.zip")
tmpScriptsZip = new File(tmpDir + File.separator + "scripts.zip")
tmpPluginsZip = new File(tmpDir + File.separator + "plugins.zip")
tmpBundleDir = new File(tmpDir + File.separator + "bundleDir")
tmpConfigDir = new File(tmpBundleDir.toString() + File.separator + "config")
tmpScriptsDir = new File(tmpBundleDir.toString() + File.separator + "scripts")
tmpPluginsDir = new File(tmpBundleDir.toString() + File.separator + "plugins")
confDir = StaticUtils.getConfigDir()
logFile = new File(confDir.toString() + File.separator + "logs" + File.separator + "customisation_${date}.log")
installDir = new File(System.getProperty("java.class.path")).getAbsoluteFile().getParent()
instPlugDir = new File(installDir.toString() + File.separator + "plugins")
confPlugDir = new File(confDir.toString() + File.separator + "plugins")
scriptsDir = new File(Preferences.getPreference(Preferences.SCRIPTS_DIRECTORY))
newScriptsDir = new File(confDir.toString() + File.separator + "scripts")
javaCmd = System.getProperty("sun.java.command")
verFile = new File(confDir + "local_version_notes.txt")
propFile = new File(confDir + "customisation.properties")
bundlePrefFile = new File(tmpConfigDir.toString() + File.separator + "omegat.prefs")
localPrefFile = new File(confDir + File.separator + "omegat.prefs")
bundleAutoText = new File(tmpConfigDir.toString() + File.separator + "omegat.autotext")
localAutoText = new File(confDir + File.separator + "omegat.autotext")

logEcho = { msg ->
    if (logFile.exists()) {
        logFile.append(msg + "\n", "UTF-8")
        console.println(msg)
    } else {
        logFile.write(msg + "\n", "UTF-8")
        console.println(msg)
    }
}
console.clear()
logEcho("="*40 + "\n" +  " "*10 + "Customisation Update\n" + "="*40)
if (! propFile.exists()) {
    propFile.write("", "UTF-8")
}
updateURL = customUrl ? customUrl : propFile.text
propFile.write(updateURL, "UTF-8")

storeUrl = {
    updateURL = propFile.text
    if (! updateURL.find(/^(?i)(ftp|https?|file)\:\/+\w+/) ) {
        contScript = false
        logEcho("Showing URL entry dialog")
        logEcho("Please enter a URL to an actual file (expected php, html or txt).")
        swing = new SwingBuilder()
        url_string = ""
        urlGUI = swing.frame(
            id:"urlGUI",
            title:"Update Bundle Remote Location",
            show:true,
            pack:true,
            size:[350,100],
            preferredSize:[400,100],
            locationRelativeTo:null,
            defaultCloseOperation:WindowConstants.DISPOSE_ON_CLOSE
            ) {
                urlGUI.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent we) {
                        urlGUI.dispose()
                        contScript = "aborted"
                        logEcho("The script was aborted.")
                        return
                    }
                })
                flowLayout()
                label("<html><div style='text-align: center;'>Enter URL (with ftp, http, or https)</div></html>")
                textField(id:"name", columns:20)
                button("Save",
                   actionPerformed: {
                        if (name.text == "") {
                            logEcho("No URL entered.")
                            return
                        } else {
                            logEcho("Entered URL: ${name.text}")
                            propFile.write(name.text, "UTF-8")
                        }
                        contScript = false
                        urlGUI.dispose()
                        storeUrl()
                   }
                )
            }
    } else {
        contScript = true
    }
}

downloadZip = {
    url, dest ->
    FileUtils.copyInputStreamToFile(url.toURL().openStream(), dest)
}

unzipFile = {
    File file, dir ->
    if (! dir.exists()) {
        dir.deleteDir()
    }
    def zipFile = new ZipFile(file)
    zipFile.entries().each { it ->
        def path = Paths.get(dir.toString() + File.separator  + it.name)
        if(it.directory){
            new File(path.toString()).mkdirs()
        }
        else {
            def parentDir = path.getParent().toString()
            if (! new File(parentDir).exists()) {
                new File(parentDir).mkdirs()
            }
            Files.copy(zipFile.getInputStream(it), path)
        }
    }
}

delDir = {
    File dir ->
    if (dir.exists()) {
        dir.deleteDir()
    }
}

updInstPlugs = {
    tmpDir, instDir ->
    new File(tmpDir.toString()).eachFileRecurse(groovy.io.FileType.FILES) {
        installCount = 0
        if (it.name.endsWith(".jar")) {
            def bundleJar = it
            def baseName = bundleJar.getName()
            def jarPath = bundleJar.getAbsoluteFile().getParent()
            def libName = baseName.minus(~/-\d+.*\.jar$/)
            new File(instDir.toString()).eachFileRecurse(groovy.io.FileType.FILES) {
                if (it.name.contains(libName) && it.name.endsWith(".jar")) {
                    def foundJar = it
                    def foundPath = foundJar.getAbsoluteFile().getParent()
                    def foundBaseName = foundJar.getName()
                    if ((Files.isWritable(instDir.toPath()))) {
                        if (foundBaseName < baseName) {
                            if (foundPath != instDir.toString()) {
                                new File(foundPath).deleteDir()
                            } else {
                                switch (osType) {
                                    case [OsType.WIN64, OsType.WIN32]:
                                        deleteJars += "    del " + "\"" + foundJar.toString() + "\"" + "\n"
                                        success++
                                        winDel = true
                                        break
                                    case [OsType.MAC64, OsType.MAC32]:
                                        foundJar.delete()
                                        success++
                                        break
                                    default:
                                        foundJar.delete()
                                        success++
                                        break
                                }
                            }
                            if (jarPath != tmpDir.toString()) {
                                def plugSubDir = instDir.toString() + File.separator + new File(jarPath).getName()
                                delDir(new File(plugSubDir))
                                if (installCount < 1) {
                                    FileUtils.moveDirectoryToDirectory(new File(jarPath), instDir, true)
                                    installCount++
                                }
                            } else {
                                if (installCount < 1) {
                                    FileUtils.moveFileToDirectory(bundleJar, instDir, true)
                                    installCount++
                                }
                            }
                        } else {
                            if (bundleJar.exists()) {
                                bundleJar.delete()
                                installCount++
                            }
                        }
                    } else {
                        readOnlyJars += "    " + foundJar.toString() + "\n"
                        nonInstallJars += bundleJar.toString() + "\n"
                        nonInstallNames += "  " + libName + "\n"
                    }
                }
            }
        }
    }
}

printSep = {
    logEcho("-"*40)
}
printDone = {
    logEcho(" "*4 + "-- Done --")
}

if (! customUrl) {
    storeUrl()
    while (! contScript ) {
    }
    if (contScript == "aborted") {
        return
    }
}

try {
    propFile.text.toURL().openStream()
} catch (IOException e) {
    e.printStackTrace()
    message = """Update URL is not accessible.
You may wish to check your internet connection.
Make sure that
  $propFile
contains the accurate update URL.
The script will finish now."""
    logEcho(message)
    printSep()
    message.alert()
    return
}

///// PROGRESS BAR /////
sb = new SwingBuilder()
c = new GBC()

progGUI =  sb.frame(
  title: title,
  resizable: false,
  pack: true,
  //preferredSize:[450,150],
  defaultCloseOperation: WindowConstants.EXIT_ON_CLOSE,
  layout: new GBL(),
//  locationRelativeTo: null,
)

frameText = new JLabel(
    text: "<html><p align=\"center\"><b>Please wait for OmegaT customisation bundle update to finish.<br/>Do not close this window.</b></p></html>"
)
c.fill = GBC.BOTH
c.anchor = GBC.PAGE_START
c.gridx = 0
c.gridy = 0
c.ipady=20
c.weightx = 0
progGUI.add(frameText, c)

pb = new JProgressBar(
    indeterminate: true,
)
c.fill = GBC.HORIZONTAL
c.anchor = GBC.CENTER
c.gridx = 0
c.gridy = 1
//c.gridwidth = 3
c.ipadx=340
c.ipady=0
progGUI.add(pb, c)    

frameProgress = new JLabel(
    text: "OmegaT customisation bundle is being updated.\nPlease wait",
)
c.fill = GBC.BOTH
c.anchor = GBC.PAGE_END
c.gridx = 0
c.gridy = 2
c.ipadx=0
c.ipady=40
c.weightx = 0.1
c.weighty = 0.9
progGUI.add(frameProgress, c)
progGUI.setSize(450,150)
progGUI.setLocationRelativeTo(null)
///// END OF PROGRESS BAR /////

updateURLS = propFile.text.toURL().text.readLines()
try {
    configURL = updateURLS.find { it =~ "config.zip" }
    configURL.toURL().openStream()
} catch (IOException | NullPointerException e) {
    configURL = null
}
try {
    scriptsURL = updateURLS.find { it =~ "scripts.zip" }
    scriptsURL.toURL().openStream()
} catch (IOException | NullPointerException e) {
    scriptsURL = null
}
try {
    pluginsURL = updateURLS.find { it =~ "plugins.zip" }
    pluginsURL.toURL().openStream()
} catch (IOException | NullPointerException e) {
    pluginsURL = null
}
try {
    verURL = updateURLS.find { it =~ "version_notes.txt" }
    verURL.toURL().openStream()
} catch (IOException | NullPointerException e) {
    verURL = null
}

if (! verURL) {
    message = "The provided URL does not point to a valid customisation repository.\nEdit $propFile.\nThe script will finish now."
    logEcho(message)
    message.alert()
    printSep()
    return
}
configZipErr  = "You config files could not be updated. Get in touch with support for assistance."
scriptsZipErr = "You scripts could not be updated. Get in touch with support for assistance."
pluginsZipErr = "You plugins files could not be updated. Get in touch with support for assistance."
remVer = verURL.toURL().text.find(/Update \d+_\w{3}/).minus(/Update /)
if (! verFile.exists()) {
    logEcho("Fresh customisation bundle installation (v.$remVer):")
    update = 7
    if (! configURL) {
        update -= 1
        incomplUpd++
        finalMsg += configZipErr
        logEcho("* Config needs to be installed.\n  - Invalid URL for config installation.")
    } else {
        logEcho("* Config needs to be installed.")
    }
    if (! scriptsURL) {
        update -= 2
        incomplUpd++
        finalMsg += scriptsZipErr
        logEcho("* Scripts need to be installed.\n  - Invalid URL for scripts update.")
    } else {
        logEcho("* Scripts need to be installed.")
    }
    if (! pluginsURL) {
        update -= 4
        incomplUpd++
        finalMsg += pluginsZipErr
        logEcho("* Plugins needs to be updated.\n  - Invalid URL for plugin update.")
    } else {
        logEcho("* Plugins needs to be updated.")
    }
    localVer = "0_000"
    printSep()
} else {
    localVer = verFile.text.find(/Update \d+_\w{3}/)
    localVer = localVer ? localVer.minus(/Update /) : "0_000"
    logEcho("Local customisation version is $localVer.\nRemote customisation version is $remVer.")
    if (localVer >= remVer) {
        logEcho("No customisation update needed.")
        finalMsg += "\nNo files needed to be updated."
        printSep()
        success = -2
        if (autoLaunch) {
            noFinMsg = true
        }
    } else {
        if (Integer.parseInt(localVer.tokenize("_")[0]) < Integer.parseInt(remVer.tokenize("_")[0]) - 1)
        remVer = remVer.tokenize("_")[0] + "_csp"
        logEcho("Customisation update available:")
        if (remVer.tokenize("_")[1][0] != "0") {
            logEcho("* Config needs to be updated.")
            update += 1
            if (! configURL) {
                logEcho("  - Invalid URL for config update.")
                update -= 1
                incomplUpd++
                finalMsg += configZipErr
            }
        }
        if (remVer.tokenize("_")[1][1] != "0") {
            logEcho("* Scripts need to be updated.")
            update += 2
            if (! scriptsURL) {
                logEcho("  - Invalid URL for scripts update.")
                update -= 2
                incomplUpd++
                finalMsg += scriptsZipErr
            }
        }
        if (remVer.tokenize("_")[1][2] != "0") {
            logEcho("* Plugins need to be updated.")
            update += 4
            if (! pluginsURL) {
                logEcho("  - Invalid URL for plugins update.")
                update -= 4
                incomplUpd++
                finalMsg += pluginsZipErr
            }
        }
        printSep()
    }
}
if (update != 0) {
    progGUI.show()
    if (tmpBundleDir.exists()) {
        tmpBundleDir.deleteDir()
        tmpBundleDir.mkdirs()
    }
    if (((update & confUpd) != 0)) {
        logEcho("config.zip is being downloaded...")
        frameProgress.setText("Updating config...")
        downloadZip(configURL, tmpConfigZip)
        logEcho("config.zip is being unpacked...")
        unzipFile(tmpConfigZip, tmpConfigDir)
        logEcho("Customisation is being updated...")
        bakDir = new File(confDir + File.separator + "customisation_backup" + File.separator + date)
        bakDir.mkdirs()
        if (bundlePrefFile.exists()) {
            if (localPrefFile.exists()) {
                bundlePrefs = new XmlSlurper().parse(bundlePrefFile)
                localPrefs = new XmlSlurper().parse(localPrefFile)
                bundleMap = bundlePrefs.preference.children().collectEntries {n->[(n.name()):(n.text())]}
                localMap = localPrefs.preference.children().collectEntries {n->[(n.name()):(n.text())]}
                bundleMap.each {
                    localMap << it
                }
                writePref = """<?xml version="1.0" encoding="UTF-8" ?>
<omegat>
  <preference version="1.0">
"""
                localMap.each {
                    localMap[it.key] = utils.makeValidXML(it.value) 
                    writePref += "    <${it.key}>${it.value}</${it.key}>\n"
                }
                writePref += """  </preference>
</omegat>
"""
                FileUtils.copyFileToDirectory(localPrefFile, bakDir)
                localPrefFile.write(writePref, "UTF-8")
                bundlePrefFile.delete()
                success++
            } else {
                FileUtils.moveFile(bundlePrefFile, localPrefFile)
                success++
            }
        }
        if (bundleAutoText.exists()) {
            if (localAutoText.exists()) {
                bundleAutoText = new File(tmpConfigDir.toString() + File.separator + "omegat.autotext")
                localAutoText = new File(confDir + File.separator + "omegat.autotext")
                newAutoText = (bundleAutoText.text + localAutoText.text).tokenize("\n").unique().join("\n")
                FileUtils.copyFileToDirectory(localAutoText, bakDir)
                localAutoText.write(newAutoText, "UTF-8")
                bundleAutoText.delete()
                success++
            } else {
                FileUtils.moveFile(bundleAutoText, localAutoText)
                success++
            }
        }
        new File(tmpConfigDir.toString()).eachFileRecurse(groovy.io.FileType.FILES) {
            bundleFile = it.getName()
            localFile = new File(confDir + bundleFile)
            if (localFile.exists()) {
                FileUtils.copyFileToDirectory(localFile, bakDir)
            }
        }
        FileUtils.copyDirectory(tmpConfigDir, new File(confDir))
        delDir(tmpConfigDir)
        success++
        finalMsg += "\nYour config files have been updated."
        frameProgress.setText("Config updated.")
        logEcho("OmegaT will need to be restarted.")
        printDone()
        printSep()
    }
    if (((update & scrpUpd) != 0)) {
        def setScriptsFolder = 0
        logEcho("scripts.zip is being downloaded...")
        frameProgress.setText("Updating scripts...")
        downloadZip(scriptsURL, tmpScriptsZip)
        logEcho("scripts.zip is being unpacked...")
        unzipFile(tmpScriptsZip, tmpScriptsDir)
        logEcho("Scripts are being installed...")
        if (! Files.isWritable(scriptsDir.toPath())) {
            setScriptsFolder = 1
            message = """Scripts folder
  $scriptsDir
is not writable.
This customisation update utility
will copy all the installed scripts into
 $newScriptsDir
and set set it as a new Scripts folder."""
        } else {
            if (scriptsDir != newScriptsDir){
                setScriptsFolder = 2
                message = """Scripts folder
  $scriptsDir
is writable,
but it is not
  $newScriptsDir
This customisation update utility
will copy all the installed scripts into
 $newScriptsDir
and set set it as a new Scripts folder."""
            }
        }
        if (setScriptsFolder > 0) {
            logEcho(message)
            if (! newScriptsDir.exists()) {
                newScriptsDir.mkdirs()
                logEcho("$newScriptsDir is created.")
            }
            if (scriptsDir.exists()) {
                FileUtils.copyDirectory(scriptsDir, newScriptsDir)
                logEcho("Script files copied from \n  $scriptsDir \nto \n  $newScriptsDir.")
            } else {
                logEcho("The folder \n  $scriptsDir \nis specified as the Scripts folder \nbut it does not actually exist.")
            }
            if (setScriptsFolder == 2) {
                delDir(scriptsDir)
            }
            logEcho("Scripts folder is set to \n  ${newScriptsDir}.")
        }
        FileUtils.copyDirectory(tmpScriptsDir, newScriptsDir)
        logEcho("Scripts provided \nin the customisation bundle \ncopied to \n  $newScriptsDir.")
        delDir(tmpScriptsDir)
        newSDText = newScriptsDir.toString().replaceAll('\\\\', "\\\\\\\\")
        Preferences.setPreference(Preferences.SCRIPTS_DIRECTORY, newScriptsDir)
        if (! localPrefFile.exists()) {
            writePref = """<?xml version="1.0" encoding="UTF-8" ?>
<omegat>
<preference version="1.0">
<scripts_dir>${newSDText}</scripts_dir>
</preference>
</omegat>
"""
        } else {
            writePref = localPrefFile.text.findAll(/<scripts_dir>.+<\/scripts_dir>/) ?
            localPrefFile.text.replaceAll(/<scripts_dir>.+<\/scripts_dir>/, "\\<scripts_dir\\>${newSDText}\\<\\/scripts_dir\\>") :
            localPrefFile.text.replaceAll(/>\n  <\/preference>/, "\\>\n    \\<scripts_dir\\>${newSDText}\\<\\/scripts_dir\\>\n  \\<\\/preference\\>")
        }
        localPrefFile.write(writePref, "UTF-8")
        success++
        finalMsg += "\nYour scripts have been updated."
        frameProgress.setText("Scripts updated.")
        printDone()
        printSep()
    }
    if (((update & plugUpd) != 0 )) {
        logEcho("plugins.zip is being downloaded...")
        frameProgress.setText("Updating plugins...")
        downloadZip(pluginsURL, tmpPluginsZip)
        logEcho("plugins.zip is being unpacked...")
        unzipFile(tmpPluginsZip, tmpPluginsDir)
        logEcho("Plugins are being installed...")
        if (instPlugDir.exists()) {
            new File(tmpPluginsDir.toString()).eachFileRecurse(groovy.io.FileType.FILES) {
                def bundleJar = it
                def baseName = bundleJar.getName()
                def jarPath = bundleJar.getAbsoluteFile().getParent()
                def libName = baseName.minus(~/-\d+.*\.jar$/)
                new File(instPlugDir.toString()).eachFileRecurse(groovy.io.FileType.FILES) {
                    if (it.name.contains(libName) && it.name.endsWith(".jar")) {
                        def foundJar = it
                        def foundPath = foundJar.getAbsoluteFile().getParent()
                        def foundBaseName = foundJar.getName()
                        deleteJars += "    del " + "\"" + foundJar.toString() + "\"" + "\n"
                        readOnlyJars += "    " + foundJar.toString() + "\n"
                        nonInstallJars += bundleJar.toString() + "\n"
                        nonInstallNames += "  " + libName + "\n"
                    }
                }
            }
        }
        
        if (readOnlyJars.readLines().size() > 0) {
            if (! Files.isWritable(instPlugDir.toPath())) {
                message = """  --
Folder
  $instPlugDir
is not writable, but it contains file(s)
which should be updated by this
customisation update utility:

${readOnlyJars}
Make sure the listed files are deleted
before you start OmegaT again.
The newer versions of these files
will be installed
into user's configuration folder.
  --"""
            } else {
                switch (osType) {
                case [OsType.WIN64, OsType.WIN32]:
                    success++
                    winDel = true
                    break
                default:
                    readOnlyJars.tokenize("\n").each {
                        new File(it.replaceAll(/^\s+/, "")).delete()
                    }
                    success++
                    break
                }
                message = """  --
Folder
  $instPlugDir
is writeable and contains file(s)
which should be updated by this
customisation update utility:

${readOnlyJars}
This utility will try to remove
the listed files.
The newer versions of these files
will be installed
into user's configuration folder.
  --"""
            }
            logEcho(message)
            finalMsg += "\n$message"
            openInstPlugDir = true
        }
        if (! confPlugDir.exists()) {
            confPlugDir.mkdirs()
        }
        updInstPlugs(tmpPluginsDir, confPlugDir)
        if (tmpPluginsDir.exists()) {
            tmpPluginsDir.listFiles().each {
                try {
                    if (it.isDirectory()) {
                        def subfiles = it.listFiles()
                        if (it.directorySize() == 0) {
                            it.deleteDir()
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace()
                }
            }
        }
        FileUtils.copyDirectory(tmpPluginsDir, confPlugDir, true)
        finalMsg += "\nYour plugins have been updated."
        frameProgress.setText("Plugins updated.")
        delDir(tmpPluginsDir)
        printDone()
        printSep()
    }
}
     if (incomplUpd == 0) {
         FileUtils.copyInputStreamToFile(verURL.toURL().openStream(), verFile)
     } else {
         logEcho("Update was incomplete.\nVersion number remains $localVer\n  to enable further updates.")
         finalMsg += "\n\n\nYou might want to take a screenshot of this dialog for future reference."
     }
logEcho("="*40 + "\n" + " "*5 + "Customisation Update Finished\n" + "="*40)
def batFile
progGUI.dispose()
if (success > 0) {
    message = "<html><b>Customisation update $remVer finished!</b><br/>OmegaT will have to be restarted.</html>"
    if (winDel) {
        deleteJars += ")"
        batFile = new File(tmpBundleDir.toString() + File.separator + "DeleteJars.cmd")
        batFile.write(deleteJars, "UTF-8")
        java.awt.Desktop.desktop.open(batFile)
    }
    logEcho("Shutting down OmegaT")
    //if (openInstPlugDir) {
    //    java.awt.Desktop.desktop.open(instPlugDir)
    //}
    message.alert()
    System.exit(0)
} else {
    if (success == -2) {
        if (noFinMsg) {
            return
        } else {
            message = "<html><b>Your customisation $remVer is up to date!</b><br/>No update needed.</html>"
            message.alert()
        }
    } else {
        message = "<html><b>Customisation update $remVer finished!</b><br/>OmegaT does not need to be restarted.</html>"
        message.alert()
    }
}
return
