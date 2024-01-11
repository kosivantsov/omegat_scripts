/* :name = Copy TMX for current source :description=
 *
 * @author    Kos Ivantsov
 * @creation  2024.01.11: First draft by Kos
*/

import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*

if (eventType == COMPILE) {
    prop = project.getProjectProperties()
    if (! prop) {
        msg = "Open a project and try again"
        exit = true
        return
    }

    projectRoot = prop.projectRoot
    targetRoot = prop.targetRoot
//    projectRootName = projectRoot.split(File.separator)[-1]
    projectRootName = new File(projectRoot).name
    tmxName = "${projectRootName}-omegat.tmx"
    tmxFile = new File(projectRoot + tmxName)
    tmxToCommit = new File(targetRoot, "${projectRootName}.tmx")
    tmxToCommit.text = tmxFile.text
    console.println("Export TMX copied to target folder")
}
