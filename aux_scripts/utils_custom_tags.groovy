/*:name = Utils - Custom Tags Definitions :description=Reads custom tags and flagged text regex per project if defined
 * 
 * @author:  Kos Ivantsov
 * @date:    2024-03-26
 * @version: 0.0.1 
 */

import org.omegat.util.Preferences
import org.omegat.util.StaticUtils

prop = project.projectProperties
if (! prop) {
    console.println("No project open")
    return
}
console.println("Custom tags and flagged text")
configDir = StaticUtils.getConfigDir()
omtDir = prop.getProjectRoot() + "omegat" + File.separator
tagFileName = "omegat.customtags"
flaggedFileName = "omegat.flaggedtext"
globalTagFile = new File(configDir + tagFileName)
globalFlaggedFile = new File(configDir + flaggedFileName)
projectTagFile = new File(omtDir + tagFileName)
projectFlaggedFile = new File(omtDir + flaggedFileName)
currentTags = Preferences.getPreference("tagValidation_customPattern")
currentFlaggedText = Preferences.getPreference("tagValidation_removePattern")
if (! globalTagFile.exists()) {
    globalTagFile.write(currentTags, "UTF-8")
}
if (! globalFlaggedFile.exists()) {
    globalFlaggedFile.write(currentFlaggedText, "UTF-8")
}
appliedTagFile = projectTagFile.exists() ? projectTagFile : globalTagFile
appliedFlaggedFile = projectFlaggedFile.exists() ? projectFlaggedFile : globalFlaggedFile
Preferences.setPreference("tagValidation_customPattern", appliedTagFile.text)
Preferences.setPreference("tagValidation_removePattern", appliedFlaggedFile.text)
Preferences.save()
