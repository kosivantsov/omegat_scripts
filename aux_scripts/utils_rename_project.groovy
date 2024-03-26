/*:name=Utils - Rename Project :description=Renames the project accrording to omegat/project_name.txt
 * 
 * @author  Kos Ivantsov
 * @date    2024-03-25
 * @latest  2024-03-26
 * @version 0.0.2
 * 
 */

import org.omegat.gui.main.ProjectUICommands
import org.omegat.util.StaticUtils

prop = project.projectProperties
if (! prop) {
    console.println("No project open")
    return
}
configDir = StaticUtils.getConfigDir()
omtDir = prop.getProjectRoot() + "omegat"
projectDir = prop.getProjectRoot()
projectName = prop.projectName
omtEnding = projectName.find(/[_\.]?[oO][mM][tT]$/) ? projectName.find(/[_\.]?[oO][mM][tT]$/) : ""
parentDir =  (projectDir - projectName).replaceAll(/[\\\\\/]+$/, "") 
projectNameFile = new File(omtDir + File.separator + "project_name.txt")
timestamp = new Date().format("yyyyMMddHHmmss")
if (projectNameFile.exists()) {
    newProjectName = projectNameFile.text ? projectNameFile.readLines().get(0) : projectName
    omtEnding = newProjectName.find(/[_\.]?[oO][mM][tT]$/) ? "" : omtEnding
    newProjectName = newProjectName + omtEnding
    newProjectDir = new File(parentDir + File.separator + newProjectName)
    console.println newProjectDir
    if (newProjectName != projectName) {
        rename = true
        if (newProjectDir.exists()) {
            backupExisting = new File(newProjectDir.toString() + "." + timestamp)
            rename = newProjectDir.renameTo(backupExisting)
        }
        if (rename) {
            ProjectUICommands.projectClose()
            sleep 3000
            new File(projectDir).renameTo(newProjectDir)
            ProjectUICommands.projectOpen(newProjectDir, true)
        } else {
            console.println("✘ Failed to rename the project")
        }
    }
}
