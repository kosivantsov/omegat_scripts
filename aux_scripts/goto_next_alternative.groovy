/* :name=GoTo - Next alternative :description=Jump to the next segment with an alternative translation
 *  
 *  @author:   Kos Ivantsov
 *  @date:     2023-02-02
 *  @version:  0.1         
 */
 
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

prop = project.projectProperties
exit = false
if (!prop) {
  final def title = 'Next alternative'
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
        if (nextSegmentNumber == startingSegmentNumber) {
            return
        }
        if (! info.defaultTranslation ) {
            jump = true
            editor.gotoEntry(nextSegmentNumber)
            return
        } else {
            jump = false
            currentSegmentNumber = nextSegmentNumber
        }
    }
}
return
