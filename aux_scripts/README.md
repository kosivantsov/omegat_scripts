# Documentation

## Enabling Jython in OmegaT

To enable Jython as one of the script engines in OmegaT, it's necessary to add jython to the classpath. To do so, run OmegaT with this command:

```-cp "/path/to/OmegaT/installation/folder/OmegaT.jar:/path/to/OmegaT/installation/folder/lib/*:/path/to/jython.jar" org.omegat.Main```

Alternatively, OmegaT can be built with Jython support. To do so, edit the file `build.gradle` in OmegaT source folder. Add this line:

```runtimeOnly 'org.python:jython-standalone:2.7.3'```

somewhere after the lines that enable Groovy and JavaScript, and then build with gradlew:

```./gradlew installDist```

A sample jython script is provided [here](https://github.com/kosivantsov/omegat_scripts/blob/master/aux_scripts/jython_test.py).

## Customization script [`updateConfigBundle.groovy`]

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

### Remote requirements

The URL must point to a location in a file server, where the `index.html` (or equivalent) lists the following lines:

1. Customization version, including the an integer number followed by underscore followed by a three-character code (e.g. `csp`)
    * The first character stands for config files: it must always be `c`, which means some config files have been updated (at least the `version_notes.txt` file).
    * The second character stands for scripts: if it is `s`, that means that some scripts have been updated, otherwise it must be `0`
    * The second character stands for plugins: if it is `p`, that means that some plugins have been updated, otherwise it must be `0`
2. The URL to the `version_notes.txt`, including the history of updates and the version number in the third line
3. The URL to the three zip bundles (for config, plugins and scripts)

For example:
```
49_cs0
https://www.example.com/OmegaT/customization/config/version_notes.txt
https://www.example.com/OmegaT/customization/config.zip
https://www.example.com/OmegaT/customization/plugins.zip
https://www.example.com/OmegaT/customization/scripts.zip
``` 

In the example above, there are updated config files and scripts, but no plugins, so the script doesn't need to download plugins. 

The script compares the customization version in the remote `version_notes.txt` with the customization version in the local `version_notes.txt` (which is, if it exists, a copy of a previous customization), and download only what is new if the two versions are consecutive (45 vs 46). If they are far apart by more than 1, then all custom files are downloaded.

The version notes must be a markdown file that looks like this: 

```
# OmegaT (cApStAn) customization utility (or any other title)

## Update 49_c0p (2020-06-23)

* Plugin foo.jar updated
* Plugin bar.jar updated

## Update 48_cs0 (2020-06-23)

* Plugin foo.groovy updated
* Plugin bar.groovy updated
```
etc.

Every time the custom files are updated in the remote location (e.g. `https://www.example.com/OmegaT/customization`), a new line with the new version must be added to the remote `version_notes.txt` file and the three bundles must be updated with the new files. The script downloads the necessary bundle as required.

### Caveats

#### Plugins

To avoid conflicts, the script deletes all jar files from the **installation folder** (if that folder is writeable) or prompts the user do so manually (if the folder is read-only). 

Any jar files that the user might wish to keep can be saved under a subfolder in the `plugins` folder, e.g. `plugins\my_folder`. That way they will be protected and the customization script will not touch them. 

#### Restart

To make sure that the new settings are applied (in case there is more than instance running), OmegaT will close after the customization update. It needs to be restarted manually. Since the check for updates happens when OmegaT is launched and before any project is open, if there is any update available, it'll have to be started twice. 

### Locations
#### Where is the user configuration folder

The user can reach that folder in two ways:

* From OmegaT, go to **Tool > User Configuration Folder**
* From the operating system, (in Windows) press **Win+R** and type `%appdata%/OmegaT`.

#### Where is the installation folder

On a 64-bit machine under Windows 10, it is by default on path `C:\Program Files\OmegaT`.
