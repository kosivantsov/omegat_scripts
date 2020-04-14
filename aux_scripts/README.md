# Documentation

## updateConfigBundle.groovy

### Customization

OmegaT can be customized by means of:

* plugins (and their properties files)
* scripts (and their properties files)
* other configuration files

All these custom files will be saved in the **user configuration folder**. Plugins and scripts are saved in their respective folders, `plugins` and `scripts`, and the rest of the configuration files are written directly to the user configuration folder.

### Options 

| Option | Description |
|:-------|:------------|
| `customUrl` | insert URL between quotes or set to "" (empty) to ask the user on the first run. | 
| `autoLaunch` | If set to `true` and the script is saved at `scrips\application_startup`, the script will run when OmegaT is started. |
| `removeExtraPlugins` | Lets you delete any loose jar files in the `plugins` folder under the **installation folder**. |
| `deletePlugVerbose` | If set to true, makes the script list the jar files to be removed manually. If set to false, it makes the script remind the user to remove plugins from the `plugins` folder under the **installation folder**. |

### Caveats

To avoid conflicts, the script deletes all jar files from the **installation folder** (if that folder is writeable) or prompts the user do so manually (if the folder is read-only). 

Any jar files that the user might wish to keep can be saved under a subfolder in the `plugins` folder, e.g. `plugins\my_folder`. That way they will be protected and the customization script will not touch them. 

### Where is the user configuration folder

The user can reach that folder in two ways:

* From OmegaT, go to **Tool > User Configuration Folder**
* From the operating system, (in Windows) press **Win+R** and type `%appdata%/OmegaT`.

### Where is the installation folder

On a 64-bit machine under Windows 10, it is by default on path `C:\Program Files\OmegaT`.
