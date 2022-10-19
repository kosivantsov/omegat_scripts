// :name=Utils - Select Source :description=Select all source text in the current segment

def gui() {
    try {
        editor.editor.getOmDocument().getTranslationStart()
    }
    catch(IOException | NullPointerException e) {
        return
    }
    end = editor.editor.getOmDocument().getTranslationStart() - 1
    start = end - editor.currentEntry.getSrcText().size()
    editor.editor.setSelectionStart(start)
    editor.editor.setSelectionEnd(end)
}
