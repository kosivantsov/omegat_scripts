import static javax.swing.JOptionPane.*
fi = project.projectFiles.subList(editor.@displayedFileIndex, editor.@displayedFileIndex + 1)[0]
if (fi.toString() != System.getProperty("OmegaTSourceFile")) {
    final def title = "New Source File Activated"
    final def msg   = "New source file activated:\n<html><b>${fi.filePath}</b></html>"
    console.println msg.replaceAll(/<\/?\w+>/, "")
    showMessageDialog null, msg, title, INFORMATION_MESSAGE
}
System.setProperty("OmegaTSourceFile", fi.toString())
