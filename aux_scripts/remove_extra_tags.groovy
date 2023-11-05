/* :name =  Remove extraneous tags :description = Finds and removes all extra tags not present in the source
 * 
 * @author   Kos Ivantsov
 * @date     2023-11-05
 * @version  0.2
 */

import org.omegat.core.data.PrepareTMXEntry
import org.omegat.core.data.ProtectedPart
import org.omegat.core.data.TMXEntry
import org.omegat.gui.main.ProjectUICommands
import org.omegat.util.OConsts
import org.omegat.util.Preferences

/* UI strings */
name          = "Remove extraneous tags" 
noProjectOpen = "Open a project and try again."
scriptMarker  = "<tagFix>"  // will be appended to the changerID to mark changed segments
untranslated  = "<UNTRANSLATED>"

/* Report in the console which script is running */
console.clear()
console.println(resBundle("name", name) + "\n${"=" * name.size()}")

/* External resources hack (use hardcoded strings if .properties file isn't found) */
def resBundle(k,v) {
    try {
        v = res.getString(k)
    }
    catch (MissingResourceException e) {
        v
    }
}

/* Check if the project is open */
prop = project.projectProperties
if (!prop) {
    message = resBundle("noProjectOpen", noProjectOpen)
    final def msg = message
    final def title = resBundle("name", name)
    showMessageDialog null, msg, title, INFORMATION_MESSAGE
    console.println(message)
    return
}

/* Set a pattern to match extra tags */
tagPattern = /(\<\/?\w+\d+\/?\>)/

/* Get current entry number, then, if needed and possible, jump to a segment with no tags in the target
   This is to prevent a possible conflict resolution which will show up
   if the currently open segment was fixed
   
   Since the jump to an untranslated segment could cause automatic insertion of the source text or the best match,
   it's important to make sure those options are disabled*/
automaticallyInsertBestMatch = Preferences.getPreference("wf_insertBestMatch")
automaticallyInsertSource = Preferences.getPreference("wf_noSourceText")
Preferences.setPreference("wf_insertBestMatch", false)
Preferences.setPreference("wf_noSourceText", true)

curEntry = editor.getCurrentEntryNumber()
noTagsTarget = project.allEntries.collect { project.getTranslationInfo(it).translation ? project.getTranslationInfo(it).translation : "" }.findIndexOf {str -> !(str =~ tagPattern)} + 1
if (noTagsTarget !== curEntry) {
    /* This hack is needed not to raise errors in the Log when editor is addressed directly */
    org.omegat.util.gui.UIThreadsUtil.executeInSwingThread {
        editor.gotoEntry(noTagsTarget)
    } as Runnable 
}

/* BackUp current project_save */
timestamp = new Date().format("YYYYMMddHHmm")
project_save = new File(prop.projectInternal, OConsts.STATUS_EXTENSION)
project_save_bak = new File(project_save.toString() + "." + timestamp + ".preTagFix")
project_save_bak << project_save.text

/* Change the Translator's ID to reflect that the change has been done with the script */
originalAuthor = Preferences.getPreferenceDefault(Preferences.TEAM_AUTHOR, System.getProperty("user.name"))
newAuthor = "${originalAuthor} ${scriptMarker}".replaceAll(/(${scriptMarker}\s*){2,}/, /${scriptMarker}/)
Preferences.setPreference(Preferences.TEAM_AUTHOR, newAuthor)

/* Traverse the project and fix the translation */
count = 0
project.allEntries.each() { ste ->
    info = project.getTranslationInfo(ste)
    /* Creation/change ID/date can be read, but they are not needed */
    //changeId = info.changer ? info.changer : "" 
    //changeDate = info.changeDate ? info.changeDate : ""
    //creationId = info.creator ? info.creator : ""
    //creationDate = info.creationDate ? info.creationDate : ""
    def originalTarget = modifiedTarget = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null
    index = ste.entryNum()-1

    /* Collect source tags for the entry */
    sourceTags = []
    for (ProtectedPart pp : ste.getProtectedParts()) {
        sourceTags.add(pp.getTextInSourceSegment())
    }
    /* Collect target tags for the entry */
    targetTags = []
    matcher = (originalTarget =~ tagPattern)
    matcher.each() {match -> targetTags.add(match[0])}
    /* Get the extraneous tags into a separate array */
    diffTags = targetTags - sourceTags

    /* If the segment has extra tags in the target, remove them */
    if (diffTags.size() > 0) {
        diffTags.each() {
           modifiedTarget = modifiedTarget.replaceAll(it, "")
        }
        reportTarget = modifiedTarget ? modifiedTarget : untranslated
        console.println("${index}❘ ${originalTarget}\n" + "${" " * index.toString().size()}❘" + " ${reportTarget}\n")
        /* If the modified target is empty, keep it as untranslated (null) instead of empty ("") */
        if (modifiedTarget == "") {
            modifiedTarget = null
        }
        /* Now that modifiedTarget is populated, prepare a TMXEntry with the new target text */
        PrepareTMXEntry te = new PrepareTMXEntry()
        te.source = ste.getSrcText()
        te.translation = modifiedTarget
        if (info.hasNote()) {
            te.note = info.note
        }
        /* If the TU is written to the project's TMX, original creation ID/date is kept
        ChangerID and change date are the actual date and the Translator's ID set in Preferences */
        //te.changer = "<Tag Fix>${changeId}"
        //te.changeDate = timestamp.toLong()
        //te.creator = creationId
        //te.creationDate = creationDate ? creationDate : timestamp.toLong()
        if (info.defaultTranslation) {
            project.setTranslation(ste, te, true, TMXEntry.ExternalLinked.xAUTO)
        } else {
            project.setTranslation(ste, te, false, TMXEntry.ExternalLinked.xAUTO)
        }
        count++
    }
}
/* Set changed preferences back to what they were before the script was run */
Preferences.setPreference(Preferences.TEAM_AUTHOR, originalAuthor)
Preferences.setPreference("wf_insertBestMatch", automaticallyInsertBestMatch)
Preferences.setPreference("wf_noSourceText", automaticallyInsertSource)

/* Jump back to the entry where the script was run from */
if (noTagsTarget !== curEntry) {
    /* This hack is needed not to raise errors in the Log when editor is addressed directly */
    org.omegat.util.gui.UIThreadsUtil.executeInSwingThread {
        editor.gotoEntry(curEntry)
    } as Runnable 
}
sleep 500
if (count > 0) {
    org.omegat.gui.main.ProjectUICommands.projectReload()
}
return
