# Documentation

## updateConfigBundle.groovy

### Customization

OmegaT can be customized by means of:

* plugins (and their properties files)
* scripts (and their properties files)
* other configuration files

All these custom files will be saved in the **user configuration folder**. Plugins and scripts are saved in their respective folders, `plugins` and `scripts`, and the rest of the configuration files are written directly to the user configuration folder.

### Caveats

To avoid conflicts, the script deletes all jar files from the **installation folder** (if that folder is writeable) or prompts the user do so manually (if the folder is read-only). Those files should be backed up if that folder is writeable and they are to be kept.

### Where is the user configuration folder

The user can reach that folder in two ways:

* From OmegaT, go to **Tool > User Configuration Folder**
* From the operating system, (in Windows) press **Win+R** and type `%appdata%/OmegaT`.

### Where is the installation folder

On a 64-bit machine under Windows 10, it is by default on path `C:\Program Files\OmegaT`.
