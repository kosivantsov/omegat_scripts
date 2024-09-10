import javax.swing.JMenu
import javax.swing.JMenuItem
import org.omegat.util.OStrings
toolsMenu = Core.getMainWindow().getMainMenu().getToolsMenu()
for (int i = 0; i < toolsMenu.getItemCount(); i++) {
    JMenuItem item = toolsMenu.getItem(i)
    if (item) {
        if (item.getText().replaceAll('&', '') == OStrings.getString("TF_MENU_TOOLS_SCRIPTING").replaceAll('&', '')) {
            item.doClick()
            break
        }
    }
}
