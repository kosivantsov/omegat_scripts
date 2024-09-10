/*:name =  Utils - Toggle "Show Project Files" :description = Sets "project_files_show_on_load" to 'true' or 'false'
 * 
 * @author:  Kos Ivantsov
 * @date:    2024-09-10
 */
import org.omegat.util.Preferences
showFiles = Preferences.getPreference("project_files_show_on_load")
switch (showFiles) {
    case "":
    case "true":
        Preferences.setPreference("project_files_show_on_load", false)
        break
    case "false":
        Preferences.setPreference("project_files_show_on_load", true)
        break
}
console.println("Show Project Files on startup: ${Preferences.getPreference("project_files_show_on_load")}")
