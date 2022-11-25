write_project2excel.groovy
==========================
This script writes an XLS file, with a separate sheet for each segment file. The output file is saved in `script_output` subfolder of the project folder. It's possible to specify what data should be included in the exported file.

write_project2tsv.groovy
========================
This script writes a TSV file containing all project's segments with additional metainfo. The script allow to configure what data and in what order is going to be exported. The output file is saved in `script_output` subfolder of the project folder.

write_table_for_TMX_alt.groovy
==============================
This script creates HTML table either for the current file, or for the whole project.

Lines 28-43 are the script options to change the output file.

Lines 46-54 are the color definitions used to style tags, visual paragraph marks etc.

The script can be run in the CLI mode, in which case it will export the whole project regarless the value of the corresponding variable.
The output html file is saved in `script_output` subfolder of the project folder. The filename is based either on the project folder name, or on the current filename.
