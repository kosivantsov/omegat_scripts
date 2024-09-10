/* :name = Utils - Spellcheck Menu :description = Jump to the first misspelled word in the current segment
 *  
 * @author  Kos Ivantsov
 * @date    2023-10-27
 * @version 0.3
 */

import java.awt.Robot
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import org.omegat.gui.shortcuts.PropertiesShortcuts
import org.omegat.tokenizer.ITokenizer.StemmingMode
import org.omegat.util.Token

//UI strings
name          = "Spellcheck Menu" 
noTranslation = "Segment not translated"
noMisspelled  = "No misspelled words in this segment"
keyNotAvalable = " is not available"

//Report in the console which script is running
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
        "META": KeyEvent.VK_META,
        "WINDOWS": KeyEvent.VK_WINDOWS
    ]

    //Init a robot to perform key events
    robot = new Robot()

    //Get the position of the translation start, to be used to move the cursor
    int end = editor.editor.getOmDocument().getTranslationStart()

    //Get the position of the caret in the target. If it's before the target, start checking from the end
    pos = editor.getCurrentPositionInEntryTranslation() ? editor.getCurrentPositionInEntryTranslation() : target.size()

    //Get every word in the current translation
    targetTokens = Core.getProject().getTargetTokenizer().tokenizeWords(target, StemmingMode.NONE)
    misspelledWords = []
    for (Token tok in targetTokens) {
        String word = tok.getTextFromString(target)
        if (!Core.getSpellChecker().isCorrect(word)) {
            int start = tok.offset
            misspelledWords.add(start)
        }
    }

    //If there are misspelled words, get the offset of the last one, mover cursor/caret to the beginning of the word
    if (misspelledWords.size() > 0) {
        jumpPosition = (misspelledWords.size() == 1) ? misspelledWords[0] : misspelledWords.findAll { it <= pos }.last()
        editor.editor.setCaretPosition(end + jumpPosition)

        //Release all modifiers so that the robotized key combo is not blocked
        keyMap.each() {
            try {
                robot.keyRelease(it.value) 
            }
            catch (java.lang.IllegalArgumentException iae) {
                //If the modifier is not available, report it in the console
                console.println(it.key + resBundle("keyNotAvalable", keyNotAvalable))
            }
        }

        //Give it a bit of time, just in case
        sleep 50

        //Check if any modifiers are used in the shortcut, and if so, store their codes from the map above in an array
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
    } else {
        console.println(resBundle("noMisspelled", noMisspelled))
        statusMessage(resBundle("noMisspelled", noMisspelled))
    }
}
