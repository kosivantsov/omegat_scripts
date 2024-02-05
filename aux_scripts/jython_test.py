# :name = Jython Test :description =

from org.omegat.util import OStrings
from javax.swing import JOptionPane
from org.omegat.util import Platform

from org.omegat.core import Core
from org.omegat.util import StaticUtils
import sys

prop = project.projectProperties

if not prop:
    title = 'Test Jython script'
    msg = 'Please try again after you open a project.'
    JOptionPane.showMessageDialog(None, msg, title, JOptionPane.INFORMATION_MESSAGE)
    console.println(msg)
    try:
    	sys.exit(0)
    except:
    	pass
else:
	console.println("OmegaT version is {}".format(OStrings.VERSION))
	srcCode = project.projectProperties.sourceLanguage
	tgtCode = project.projectProperties.targetLanguage
	message = "Let\'s print source sentances in " + str(srcCode)
	console.println(message)
	console.println("=" * len(message))
	for ste in project.allEntries:
		info =  project.getTranslationInfo(ste)
		source = ste.getSrcText()
		console.println(source)
