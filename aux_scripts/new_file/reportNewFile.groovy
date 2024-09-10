import static javax.swing.JOptionPane.*
final def title = "New Source File Activated"
final def msg   = "New source file activated:\n<html><b>${activeFileName}</b></html>"
console.println msg.replaceAll(/<\/?\w+>/, "")
showMessageDialog null, msg, title, INFORMATION_MESSAGE
