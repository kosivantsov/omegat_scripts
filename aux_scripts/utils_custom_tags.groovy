/*:name = Utils - Custom Tags Definitions :description=The script loads and saves per-project custom tags/flagged text
 * 
 * @author:  Kos Ivantsov
 * @date:    2024-03-26
 * @latest:  2024-03-28
 * @version: 1.0
 * 
 */

import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*


prop = project.projectProperties
if (! prop) {
    console.println("No project open")
    return
}
currentTags = Preferences.getPreference("tagValidation_customPattern")
currentFlaggedText = Preferences.getPreference("tagValidation_removePattern")
configDir = StaticUtils.getConfigDir()
omtDir = prop.getProjectRoot() + "omegat" + File.separator
tagFileName = "omegat.customtags"
flaggedFileName = "omegat.flaggedtext"
globalTagFile = new File(configDir + tagFileName)
globalFlaggedFile = new File(configDir + flaggedFileName)
projectTagFile = new File(omtDir + tagFileName)
projectFlaggedFile = new File(omtDir + flaggedFileName)
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
        System.Properties.setProperty("applideTagProperty", appliedTagFile.toString())
        System.Properties.setProperty("applideFlaggedProperty", appliedFlaggedFile.toString())
        Preferences.setPreference("tagValidation_customPattern", appliedTagFile.text)
        Preferences.setPreference("tagValidation_removePattern", appliedFlaggedFile.text)
        Preferences.save()

    case CLOSE:
        appliedTagFile = new File(System.Properties.getProperty("applideTagProperty"))
        appliedFlaggedFile = new File(System.Properties.getProperty("applideFlaggedProperty"))
        if (appliedTagFile.text != currentTags) {
            appliedTagFile.write(currentTags, "UTF-8")
        }
        if (appliedFlaggedFile.text != currentFlaggedText) {
            appliedFlaggedFile.write(currentFlaggedText, "UTF-8")
        }
}
