Scripts in this folder will run when a project is created, loaded, saved, modified, closed, or when target files are compiled. Scripts can specify which particular even should trigger their execution.

## `utils_custom_tags.groovy`

<details>
This script runs each time the project loads. It checks if there are two files inside the <code>omegat</code> subfolder of the currently open project:
  <code>omegat.customtags</code> and <code>omegat.flaggedtext</code>
  
If either or both files are found, RegEx in them will be used in the project.
  
While the project is open, changing custom tags or flagged text should be done in a normal way, through OmegaT preferences (**Preferences** > **Tag Processing**).
If the definitions in a newly open project are different from the ones used in the project that was open before, the project will reload once upon initial loading.
  
Global custom tags and flagged text definitions are stored in <code>omegat.customtags</code> and <code>omegat.flaggedtext</code> inside the OmegaT configuration folder.

There are a few minor drawbacks with this approach:
- It is impossible to edit global definitions while no project is open. And if the project is open, it needs to contain no project-specific custom tags and flagged text definitions if global definitions are to be edited.
- Project-specific files with the definitions need to be copied to the project manually. The GUI doesn't indicate in any way whether these are global or project-specific.
  If the global definitions are a passable starting point, those two files can be copied from the config folder (they will be created there automatically and will be populated with whatever RegEx was saved in OmegaT when the script was run for the very first time, or if those files were deleted).
</details>
