/* :name = Utils - Spellcheck Menu :description = Jump to the first misspelled word in the current segment
 *  
 * @author  Kos Ivantsov
 * @date    2023-10-27
 * @version 0.1
 */

import java.awt.Robot
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import org.omegat.gui.shortcuts.PropertiesShortcuts
import org.omegat.tokenizer.ITokenizer.StemmingMode
import org.omegat.util.Token

name          = "Spellcheck Menu" 
noTranslation = "Segment not translated"
noMisspelled  = "No misspelled words in this segment"
console.clear()
console.println(resBundle("name", name) + "\n${"=" * name.size()}")

//External resources hack (use hardcoded strings if .properties file isn't found)
def resBundle(k,v) {
    try {
        v = res.getString(k)
    }
    catch (MissingResourceException e) {
        v
    }
}

//Print messages in the status bar of the main window
def statusMessage(message) {
	   oldStatusMessage = mainWindow.statusLabel.getText()
        mainWindow.statusLabel.setText(message)
        Timer timer = new Timer().schedule({mainWindow.statusLabel.setText(oldStatusMessage)} as TimerTask, 800)
}

def gui() {
    //Check if the target text exists
    try {
        target = editor.getCurrentTranslation()
    }
    catch (java.lang.NullPointerException npe) {
        target = null
    }

    //If no target is available, exit
    if (! target) {
        console.println(resBundle("noTranslation", noTranslation))
        statusMessage(resBundle("noTranslation", noTranslation))
        return
    }

    //Get the shortcut into an array of modifiers and the actual key 
    menuShortcut = PropertiesShortcuts.getEditorShortcuts().getKeyStroke("editorContextMenu")
        .toString().replaceAll("pressed", "").toUpperCase().tokenize(/ /)

    //Get the actual key only, and then get its key code
    menuKey = KeyStroke.getKeyStroke(menuShortcut[menuShortcut.size() - 1])
    menuCode = menuKey.getKeyCode()

    //Keep modifiers only in a array, create a map with possible strings and corresponding key events
    menuModifiers = menuShortcut.dropRight(1)
    def keyMap = [
        "SHIFT": KeyEvent.VK_SHIFT,
        "CTRL": KeyEvent.VK_CONTROL,
        "CONTROL": KeyEvent.VK_CONTROL,
        "ALT": KeyEvent.VK_ALT,
        "META": KeyEvent.VK_META
    ]

    //Init a robot to perform key events
    robot = new Robot()

    //Get the position of the translation start, to be used to move the cursor
    int end = editor.editor.getOmDocument().getTranslationStart()

    //This variable (ignore) is going to be used later to print status message if no misspelled words are found
    ignore = true

    //Get every word in the current translation
    //If it's misspelled, get its offset, mover cursor/caret to the beginning of the word 
    for (Token tok in Core.getProject().getTargetTokenizer().tokenizeWords(target, StemmingMode.NONE)) {
        String word = tok.getTextFromString(target)
        if (!Core.getSpellChecker().isCorrect(word)) {
            int start = tok.offset
            editor.editor.setCaretPosition(end + start)
            //Release all modifiers so that the robotized key combo is not blocked
            robot.keyRelease(KeyEvent.VK_SHIFT)
            robot.keyRelease(KeyEvent.VK_CONTROL)
            robot.keyRelease(KeyEvent.VK_ALT)
            robot.keyRelease(KeyEvent.VK_META)
            //Give it a bit of time, just in case
            sleep 50
            //Check if any modifiers are used, and if so, store their codes from the map above in an array
            //Then press each modifer
            keyEventModifiers = []
            if (menuModifiers.size() > 0) {
                menuModifiers.each() {
                    if (keyMap.containsKey(it)) {
                        keyEventModifiers.add(keyMap[it])
                        robot.keyPress(keyMap[it])
                    }
                }
            }
            //Now press and release the actual key
            robot.keyPress(menuCode)
            robot.keyRelease(menuCode)
            //And now release each modifier in the reversed order
            if (keyEventModifiers.size() > 0) {
                keyEventModifiers.reverse().each() {
                    robot.keyRelease(it)
                }
            }
            //Break here, so the menu pops up on the first misspelled word only
            //Set ignore to false 
            ignore = false
            return
        }
    }

    //If ignore is true, show a message in the status bar about no misspelled words 
    if (ignore) {
        console.println(resBundle("noMisspelled", noMisspelled))
        statusMessage(resBundle("noMisspelled", noMisspelled))
    }
}
