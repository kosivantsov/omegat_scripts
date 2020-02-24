/* :name=Restore files order :description=Restores the default file order and reloads the project
 *
 * This script is supposed to be located in
 * <scriptsFoler>/project_changed
 *
 * @author   Kos Ivantsov, Manuel Souto Pico
 * @date     2020-02-24
 * @version  0.1
 */

import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*

switch (eventType) {
    case LOAD:
        prop = project.projectProperties
        orderFile = new File (prop.getProjectRoot() + "omegat" + File.separator + "files_order.txt")
        if (orderFile.exists()) {
            orderFile.delete()
            console.println("Files order restored, reloading the project...")
            javax.swing.SwingUtilities.invokeLater({
                org.omegat.gui.main.ProjectUICommands.projectReload()
            } as Runnable)
            console.println("Project reloaded")
        } else {
            return
        }
        break
    default:
        return null // No output
}

