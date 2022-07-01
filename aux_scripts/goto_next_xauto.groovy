/* :name=GoTo - Next x-auto :description=Jump to next segment with x-auto translation
 *  
 *  @author:   Kos Ivantsov
 *  @date:     2021-07-01
 *  @version:  0.1         
 */
 
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

prop = project.projectProperties
exit = false
if (!prop) {
  final def title = 'Next x-auto'
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
        isXAuto = info.linked.toString()
        if (nextSegmentNumber == startingSegmentNumber) {
            return
        }
        if (isXAuto == "xAUTO") {
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
