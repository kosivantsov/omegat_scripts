/* :name=GoTo - Next repetition :description=Jump to the next repetition
 *  
 *  @author:   Kos Ivantsov
 *  @date:     2022-05-05
 *  @version:  0.1         
 */

prop = project.projectProperties
exit = false
if (!prop) {
  final def title = 'Next repetition'
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
    currentSegmentNumber = ste.entryNum()
    //jump = false
    while (!jump) {
        nextSegmentNumber = currentSegmentNumber == lastSegmentNumber ? 1 : currentSegmentNumber + 1
        stn = project.allEntries[nextSegmentNumber -1]
        startSource = ste.getSrcText()
        checkSource = stn.getSrcText()
        if (startSource == checkSource) {
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