/* :name=Activate source text :description=Activate source text on the Editor with keyboard shortcut
 * 
 *  The workaround by script for RFE #821:
 *  Showing cursor on the original segment
 *  http://sourceforge.net/p/omegat/feature-requests/821/
 *
 * @author  Yu Tang
 * @author  Kos Ivantsov
 * @date    2019-10-25
 * @version 1.1.3
 */

// *******************
// BEGIN USER SETTINGS
//********************

// for the source text field in the editor
FONT_BOLD = true // or false
READ_ONLY = true // or false

// for the target text field in the create glossary dialog
SELECT_ALL = false // or true
CARET_AT_THE_END = true // or false (at the beginning)

@Field final int TRIGGER_KEY = KeyEvent.VK_F4 // see https://docs.oracle.com/javase/8/docs/api/java/awt/event/KeyEvent.html for full list

// *****************************************************
// END USER SETTINGS, NOTHING FURTHER NEEDS TO BE EDITED
// *****************************************************

import groovy.transform.Field
import org.omegat.core.data.ProjectProperties
import org.omegat.gui.glossary.GlossaryEntry
import org.omegat.gui.glossary.GlossaryReaderTSV
import org.omegat.util.Log
import org.omegat.util.StringUtil

import javax.swing.JTextPane
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.WindowFocusListener
import java.awt.Font
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.InputEvent

import javax.swing.border.EmptyBorder
import javax.swing.JDialog
import javax.swing.JEditorPane
import javax.swing.JRootPane
import javax.swing.JViewport
import javax.swing.SwingUtilities

import org.omegat.core.Core
import org.omegat.core.CoreEvents
import org.omegat.core.events.IProjectEventListener
import org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE
import org.omegat.core.search.SearchMode
import org.omegat.gui.dialogs.CreateGlossaryEntry
import org.omegat.gui.editor.SegmentBuilder
import org.omegat.gui.search.SearchWindowController
import org.omegat.util.gui.StaticUIUtils
import org.omegat.util.gui.Styles
import org.omegat.util.gui.UIThreadsUtil
import org.omegat.util.OStrings
import org.omegat.util.StaticUtils

@Field final String SCRIPT_NAME = 'activate_source_text'

KeyListener createEditorKeyListener() {
    [
            keyPressed  : { KeyEvent e ->
                if (StaticUtils.isKey(e, TRIGGER_KEY, 0)) {
                    showDialog()
                }
            }
    ] as KeyAdapter
}

KeyListener createDialogEditorKeyListener() {
    [
            keyPressed : { KeyEvent e ->
                String selection = e.source.selectedText
                int ctrl = KeyEvent.CTRL_DOWN_MASK
                int ctrl_shift = KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK
                switch(true) {
                    // press an Enter key or any trigger key by user settings
                    case StaticUtils.isKey(e, KeyEvent.VK_ENTER, 0):
                    case StaticUtils.isKey(e, TRIGGER_KEY, 0):
                        // insert selected text to the editor
                        if (selection) {
                            editor.insertText(selection)
                        }
                        // close this dialog
                        closeDialog e.source
                        break

                    case StaticUtils.isKey(e, KeyEvent.VK_F, ctrl):
                        // open search window
                        SearchWindowController search = new SearchWindowController(SearchMode.SEARCH)
                        mainWindow.addSearchWindow search
                        search.makeVisible selection
                        break

                    case StaticUtils.isKey(e, KeyEvent.VK_F, ctrl_shift):
                        // open search window
                        List<SearchWindowController> windows = mainWindow.getSearchWindows()
                        if (windows.size() == 0) {
                            SearchWindowController search = new SearchWindowController(SearchMode.SEARCH)
                            mainWindow.addSearchWindow search
                            search.makeVisible selection
                        } else {
                            int i = windows.size() - 1
                            SearchWindowController search = windows.get(i)
                            if (search.getMode() == SearchMode.SEARCH) {
                                search.makeVisible selection
                            }
                        }
                        break

                    case StaticUtils.isKey(e, KeyEvent.VK_G, ctrl_shift):
                        // DevNote: the signature of below method is changed.
                        // OmegaT 3.x: void Core.glossary.showCreateGlossaryEntryDialog()
                        // OmegaT 4.0: void Core.glossary.showCreateGlossaryEntryDialog(final Frame parent)

                        // open add glossary term dialog
                        Closure<CreateGlossaryEntry> getCreateGlossaryEntryDialog = { ->
                            Core.glossary.showCreateGlossaryEntryDialog(mainWindow.applicationFrame)
                            ((org.omegat.gui.glossary.GlossaryTextArea)Core.glossary).createGlossaryEntryDialog
                        }
                        if (selection) {
                            def dlg = getCreateGlossaryEntryDialog()
                            if (dlg.targetText.text) {
                                // do nothing
                            } else {
                                dlg.targetText.text = editor.editor.selectedText
                            }
                            SwingUtilities.invokeLater {
                                dlg.targetText.requestFocus()
                                if (SELECT_ALL) {
                                    if (CARET_AT_THE_END) {
                                        dlg.targetText.selectAll()
                                    } else {
                                        dlg.targetText.caret.setDot dlg.targetText.text.size()
                                        dlg.targetText.caret.moveDot 0
                                    }
                                } else if (!CARET_AT_THE_END) {
                                    dlg.targetText.setCaretPosition(0)
                                }
                            } as Runnable
                        } else {
                            closeDialog e.source
                            getCreateGlossaryEntryDialog()
                        }
                        break

                    case StaticUtils.isKey(e, KeyEvent.VK_G, ctrl):
                        // Direct adding a glossary term without dialog (Ctrl+G)
                        String src = StringUtil.normalizeUnicode(selection)
                        String loc = StringUtil.normalizeUnicode(editor.editor.getSelectedText())
                        String com = ""
                        if (!StringUtil.isEmpty(src) && !StringUtil.isEmpty(loc)) {
                            try {
                                ProjectProperties props = Core.project.projectProperties
                                File out = new File(props.writeableGlossary)
                                GlossaryReaderTSV.append out, new GlossaryEntry(src, loc, com, true, '')
                            } catch (Exception ex) {
                                Log.log ex
                            }
                        }
                        closeDialog e.source
                        break
                }
            }
    ] as KeyAdapter
}

JDialog getDialogAncestor(Component comp) {
    def win = SwingUtilities.getWindowAncestor(comp)
    while (win == null || !win instanceof JDialog) {
        win = SwingUtilities.getWindowAncestor(win)
    }
    win
}

void showDialog() {
    try {
        def ste = editor.currentEntry
        if (ste == null) {
            return
        }

        JDialog dialog = createDialog(ste.srcText)
        StaticUIUtils.setEscapeClosable dialog
        setLostFocusClosable dialog
        UIThreadsUtil.executeInSwingThread { dialog.setVisible true } as Runnable
    } catch(ex) {
        console.println "$SCRIPT_NAME >> $ex"
    }
}

void closeDialog(Component comp) {
    JDialog dialog = getDialogAncestor(comp)
    def closeAction = dialog.rootPane.actionMap.get("ESCAPE")
    def action = new ActionEvent(comp, ActionEvent.ACTION_PERFORMED, "ESCAPE")
    closeAction.actionPerformed action
}

void setLostFocusClosable(JDialog dialog) {
    def closeAction = dialog.rootPane.actionMap.get("ESCAPE")
    def focusListener = [
            windowGainedFocus : {},
            windowLostFocus   : {
                def action = new ActionEvent(it.source, ActionEvent.ACTION_PERFORMED, "ESCAPE")
                closeAction.actionPerformed action
            }
    ] as WindowFocusListener
    dialog.addWindowFocusListener focusListener
}

JDialog createDialog(String text) {
    JDialog dialog = new JDialog(mainWindow)
    dialog.setUndecorated true
    // dialog.rootPane.setWindowDecorationStyle JRootPane.PLAIN_DIALOG
    dialog.rootPane.setWindowDecorationStyle JRootPane.NONE    // Fix for Metal LAF by Kos Ivantsov
    JEditorPane pane = createEditorPane(text)
    pane.addKeyListener createDialogEditorKeyListener()
    dialog.add pane
    dialog.pack()
    dialog.setBounds sourceSegmentRect
    dialog
}

JEditorPane createEditorPane(String text) {
    //JEditorPane pane = new JEditorPane('text/plain', text)
    JTextPane pane = new JTextPane()    //Fix word wrapping issue
    pane.text = text
    pane.with {
        // For read-only settings by Kos Ivantsov
        if (READ_ONLY) {
            setEditable false
            caret.visible = true
        }

        setDragEnabled true
        setComponentOrientation sourceOrientation
        setFont FONT_BOLD ? editor.font.deriveFont(Font.BOLD) : editor.font
        setForeground Styles.EditorColor.COLOR_ACTIVE_SOURCE_FG.color
        setCaretColor Styles.EditorColor.COLOR_ACTIVE_SOURCE_FG.color
        setBackground Styles.EditorColor.COLOR_ACTIVE_SOURCE.color
        setCaretPosition 0
        def b = editor.editor.border
        def border = new EmptyBorder(0, b.left, 0, b.right) // top and bottom = 0
        setBorder border
    }
    pane
}

ComponentOrientation getSourceOrientation() {
    editor.sourceLangIsRTL \
        ? ComponentOrientation.RIGHT_TO_LEFT
            : ComponentOrientation.LEFT_TO_RIGHT
}

Rectangle getSourceSegmentRect() {
    int activeSegment = editor.displayedEntryIndex
    JViewport viewport = editor.scrollPane.viewport
    Rectangle viewRect = viewport.viewRect

    SegmentBuilder sb = editor.m_docSegList[activeSegment]
    int startSourcePosition = sb.startSourcePosition
    int startTranslationPosition = sb.startTranslationPosition
    if (startTranslationPosition == -1) {
        startTranslationPosition = startSourcePosition + sb.sourceText.size() + 1 // + 1 for line break
    }
    Point sourceLocation = editor.editor.modelToView(startSourcePosition).location
    Point transLocation =  editor.editor.modelToView(startTranslationPosition).location

    if (!viewRect.contains(sourceLocation)) {  // location is NOT viewable
        throw new RuntimeException("Source segment must be viewable");
    }

    // create new Rectangle for source segment
    int x = viewRect.x
    int y = sourceLocation.y
    int width = viewRect.width
    int height = transLocation.y - y
    Point point = new Point(x, y)
    SwingUtilities.convertPointToScreen point, viewport.view
    Rectangle rect = new Rectangle(point.@x, point.@y, width, height)
    rect
}

boolean isAvailable() {
    // OmegaT 4.x or later
    (OStrings.VERSION =~/^\d+/)[0].toInteger() >= 4
}

// controller class
class ActivateSourceTextController implements IProjectEventListener {

    KeyListener _listener

    ActivateSourceTextController(KeyListener listener) {
        _listener = listener
        if (Core.project.isProjectLoaded()) {
            installKeyListener()
        }
    }

    void onProjectChanged(PROJECT_CHANGE_TYPE eventType) {
        switch(eventType) {
            case PROJECT_CHANGE_TYPE.CREATE:
            case PROJECT_CHANGE_TYPE.LOAD:
                // Lazy adding listener for waiting the opening documents process will be complete.
                Runnable doRun = { installKeyListener() } as Runnable
                SwingUtilities.invokeLater doRun
                break
            case PROJECT_CHANGE_TYPE.CLOSE:
                uninstallKeyListener()
                break
        }
    }

    void installKeyListener() {
        Core.editor.editor.addKeyListener _listener
    }

    void uninstallKeyListener() {
        Core.editor.editor.removeKeyListener _listener
    }
}

//======================
// Main routine
//======================

// verify OmegaT version
if (! isAvailable()) {
    return "$SCRIPT_NAME >> This script is not available before OmegaT 4."
}

CoreEvents.registerProjectChangeListener new ActivateSourceTextController(createEditorKeyListener())
"${SCRIPT_NAME}.groovy is available in the current session."
