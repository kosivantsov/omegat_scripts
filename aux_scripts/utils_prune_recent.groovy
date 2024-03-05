/*:name = Utils - Prune recent projects :description=
 * 
 * @author:  Kos Ivantsov
 * @date:    2024-03-04
 * @version: 0.0.1
 * 
 */

import org.omegat.util.Preferences
import org.omegat.util.RecentProjects

recentProjects = RecentProjects.getRecentProjects()
if (recentProjects.size() != 0) {
    existingProjects = []
    RecentProjects.clear()
    sleep 500
    recentProjects.each() {
        if (new File(it).exists()) {
            existingProjects.add(it)
        }
    }
    if (existingProjects.size() > 0) {
        for (index in (existingProjects.size() - 1)..0) {
            path = existingProjects[index]
            RecentProjects.add(path)
            //Preferences.setPreference(Preferences.MOST_RECENT_PROJECTS_PREFIX + index, path)
            Preferences.save()
        }
    }
    Preferences.save()

} else {
    console.println("Recent project menu is empty")
    return
}
