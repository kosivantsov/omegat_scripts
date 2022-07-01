/* :name=GoTo - Next Repetition in Project :description=Jump to the next repetition found in the project
 *  
 *  @author:   Kos Ivantsov
 *  @date:     2021-05-01
 *  @version:  0.1         
 */

import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

prop = project.projectProperties
exit = false
if (!prop) {
  final def title = 'Next non-unique'
  final def msg   = 'Please try again after you open a project.'
  showMessageDialog null, msg, title, INFORMATION_MESSAGE
  exit = true
  return
}

lastSegmentNumber = project.allEntries.size()
jump = false
def gui() {
    if (exit)
    return
    ste = editor.getCurrentEntry()
    currentSegmentNumber = startingSegmentNumber = ste.entryNum()
    //jump = false
    while (!jump) {
        nextSegmentNumber = currentSegmentNumber == lastSegmentNumber ? 1 : currentSegmentNumber + 1
        stn = project.allEntries[nextSegmentNumber -1]
        info = project.getTranslationInfo(stn)
        isDup = stn.getDuplicate()
        if ("$isDup" != "NONE") {
            jump = true
            editor.gotoEntry(nextSegmentNumber)
            return
        } else {
            jump = false
            currentSegmentNumber = nextSegmentNumber
        }
        if (nextSegmentNumber == startingSegmentNumber) {
            return
        }
    }
}
return
