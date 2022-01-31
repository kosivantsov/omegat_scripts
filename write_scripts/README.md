write_table_for_TMX_alt.groovy
==============================
This script creates HTML table either for the current file, or for the whole project.
Lines 28-43 are the script options to change the output file.
Lines 46-54 are the color definitions used to style tags, visual paragraph marks etc.

The script can be run in the CLI mode, in which case it will export the whole project regarless the value of the corresponding variable.
The output html file is saved in `script_output` subfolder of the project folder. The filename is based either on the project folder name, or on the current filename. 
