/*:name = Utils - Custom Tags Definitions :description=The script loads and saves per-project custom tags/flagged text
 * 
 * @author:  Kos Ivantsov
 * @date:    2024-03-26
 * @latest:  2024-04-04
 * @version: 1.2
 * 
 */

import org.omegat.gui.main.ProjectUICommands
import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*

omtPid = Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0])
currentTags = Preferences.getPreference("tagValidation_customPattern")
currentFlaggedText = Preferences.getPreference("tagValidation_removePattern")
if (System.Properties.getProperty("${omtPid}_appliedTagFile")) {
    appliedTagFile = new File(System.Properties.getProperty("${omtPid}_appliedTagFile"))
    appliedTagFile.write(currentTags, "UTF-8")
}
if (System.Properties.getProperty("${omtPid}_appliedFlaggedFileProperty")) {
    appliedFlaggedFile = new File(System.Properties.getProperty("${omtPid}_appliedFlaggedFileProperty"))
    appliedFlaggedFile.write(currentFlaggedText, "UTF-8")
}

prop = project.projectProperties
if (! prop) {
//    console.println("No project open")
    return
}

configDir = StaticUtils.getConfigDir()
omtDir = prop.getProjectRoot() + "omegat" + File.separator
tagFileName = "omegat.customtags"
flaggedFileName = "omegat.flaggedtext"
globalTagFile = new File(configDir + tagFileName)
globalFlaggedFile = new File(configDir + flaggedFileName)
projectTagFile = new File(omtDir + tagFileName)
projectFlaggedFile = new File(omtDir + flaggedFileName)
reload = false
switch (eventType) {
    case LOAD:
        if (! globalTagFile.exists()) {
            globalTagFile.write(currentTags, "UTF-8")
        }
        if (! globalFlaggedFile.exists()) {
            globalFlaggedFile.write(currentFlaggedText, "UTF-8")
        }
        appliedTagFile = projectTagFile.exists() ? projectTagFile : globalTagFile
        appliedFlaggedFile = projectFlaggedFile.exists() ? projectFlaggedFile : globalFlaggedFile
        System.Properties.setProperty("${omtPid}_appliedTagFile", appliedTagFile.toString())
        System.Properties.setProperty("${omtPid}_appliedFlaggedFile", appliedFlaggedFile.toString())
        System.Properties.setProperty("${omtPid}_appliedTagValue", currentTags)
        System.Properties.setProperty("${omtPid}_appliedFlaggedValue", currentFlaggedText)
        Preferences.setPreference("tagValidation_customPattern", appliedTagFile.text)
        Preferences.setPreference("tagValidation_removePattern", appliedFlaggedFile.text)
        Preferences.save()
        currentSavedTags = Preferences.getPreference("tagValidation_customPattern")
        currentSavedFlaggedText = Preferences.getPreference("tagValidation_removePattern")
        if (currentSavedTags != System.Properties.getProperty("${omtPid}_appliedTagValue")) {
            reload = true
        }
        if (currentSavedFlaggedText != System.Properties.getProperty("${omtPid}_appliedFlaggedValue")) {
            reload = true
        }
        if (reload) {
            sleep 500
            ProjectUICommands.projectReload()
        }
}
