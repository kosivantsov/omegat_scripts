/* :name=MT - Replace match with YandexMT :description=Yandex translation of the active match
 *
 * @author  Kos Ivantsov
 * @date    2020-04-24
 * @version 0.4.1
 */
import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder
import groovy.transform.Field
import java.awt.GridBagConstraints as GBC
import java.awt.GridBagLayout as GBL
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.WindowConstants
import org.omegat.util.WikiGet
showPopUp = true // set to false to disable popups
showStatusBar = true // set to false to disable statusbar messages
showSuccessPopUp = true // set to false to disable popups on successful operation
// Timeouts
int popupTimeout = 850 //info in popup
int statusTimeout = 5000 //info in the status bar
tmpDir = System.getProperty("java.io.tmpdir")
// Images
yandex64="iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH5AQXEhYP6srligAABqRJREFUWMOlln9sU9cVxz/3+ZkE28TBxcEMLEYaxNDoEpImZYVI/DGVJjBFiO2fKSFMIpQJ2Iq6ahMpfyIEW8YkJERgE2qkDjbUIUSrRCqoJBPqSAIirTql/FYErgmYOImNm/fse/aHHTcEpzPsSFdP7917zvd7frxzj+IFpaKiglmzZiEiHsMwqoD1wJvA0syRMHAZ6AIuJZPJO06nM6m1pre3N2tHvQh4TU1NWlmpWqVUG/AjoOA7VEaALq3128CwUore3l5E5PkJVFdXY5omWutNSqn3Affknoig1MwmReS+iPzc5XJ9lkgkuHz58vMRqKqqwjRNlFLNwDFg1uSeZVmMj4/j9/vRWj+lZ9s2qVSKwsJCgIjW+qfAZ8+VgsrKSgzDwDCMMsMw/gUELMtCa000GqWtrY36+nr8fj/BYBCXy4XP5yMYDLJ06VJcLhcnT56cGolKwzCGzXwJKKUwDAPgd0DAtm0aGxvx+XxEIhGCwSAiwokTJygtLcXtduN2u/H5fMyePZv79+/T2dlJNBpFKbVQKfVn27Y35x2BVatWoZRaorW+rZRibGyMjo4OysvLEREMw8DhcDAxMYFlWcRiMeLxOOFwmLNnz9LZ2YnH45laIyMi8lreERARRGTXpIE5c+Zw8OBBSkpKAFi3bh0bNmxg+/btWJZFJBJheHiYjRs3Eg6Hcblc0wt0LrA6bwKpVKrQNM2VU1MyNDTE0NAQiUSCsrIyABKJBB6PhxUrVqCUYu/evRw7doyBgQHcbvd0s2/mTcBhGLOB7+XaM02TUCiEbdscOnSI4uJiTNNERACora3l6NGjaK0n62jSideMqYacwH8AAWwgCTyCghjM3f/4ceAHqZR3ntbYIljfRoby8nKam5txOp14PB56eno4cOAA4XAY27YpKyvj9OnTeDye6WkNPBWBMUADqfQzOAt+44NKYOHaW7e8r7pc/phpMuZ2c87r5URRESuXL2ffvn14PB6i0SgjIyO0trZSXV3NvHnz6O/vZ3BwkKamJmpqarhw4QIOh2MyAmIaGVCd8diAoAN+64BfP9UoRJgTjzMHWDA6yrJQiDpgdNs25nq97H7nHTZt2sSaNWtYvXo1jY2N2RooLCxky5YtlJSUkEqlsgRE5KY5CZwBr3fAEWBxPnXxQ4CODs5/+SWnL13i7t271NbW0trais/no729nVAoRENDAyLC+Pj4UzUAdJlPMuAmrHbA3wDvM0ivvw6LFsHAAHz11dN73d38pLub9yorCb3yCg8ePCAYDNLX18fx48fx+/00NzcTi8W4ceMGppnN+gTwselIE5hfAP/ICQ7Q1gYVFdDRAW+9lfPIDoBdu0h4vcTjcQKBAKWlpfj9fhYvXkxXVxeDg4NTCXyutb7ieBsML/wVWJXTckMDtLSAxwMLF6bJ5BDj66+5HgqxeP9+AqbJ/Pnz6erq4vbt24TDYdrb2ykoyN7YcRHZDNxUNiwz4RLwUk7Lhw/Dzp1w5w4sWQJHjsCOHTmPaqCxvJxBp5OXiosZHx9HKYVt2zidzuzFKSK/EJEPLcvCAH48IzhAfT1MTMCePfDkCdTVQSCQOwrA1rExUqZJLBbLtt4p4GFgm2EYHwJcu3YNw4S6GcH37IHSUvjkEzh1CkIhWLAgnZYZ5PuxGN5p84CIoLX+i9a6VkTeTyaT2bHMAF6d0dqWLWBZcP58+v2jj6CwENatm1GlKJHQJSIPgOvARRE5ZBhGKdCitb6ZTCbp6+v7th0LfJNznmtqgqNH4eFDWL4cEolMEMNQVAQuV+46UGq45+WX1/7e5wuntE44HI5vJutARLh69eozhfNEQJ5ZHR0iIiKRiMjFiyLd3SKffioSDqe/f/CB5NLTcH8C/HnPeQKf5yQQjYqkUiKPHqVJTK7Hj0W0Frl3T2TlShGlphP4YmymfjLDr/OHZ8APH057eeZMTi9lcFDEtkXefTfX/pl74MgX30jCx5m2mJYlS2D9erBt6OnJrXXuHJgmvPHGs4MLnAqkL9T8JApuDf/OerB1q8joqMitWyJVVbkjAOl0JBIiixZNDf8VOzNP5CumG+JJ+JUT+gGD/n7YvRsiEbhyZWbNzZvTrXlKe03BTisz2OQtY6T/wxT8TCA+o8ffvVIa3hPITkp5y58yIXsIyoZ6gdHnBLdsaJF0OrPd7bmkKEMiDiQgqOGfAiP/A3hCw9Uk1D0BYvyf0gI8ThPgOjhtWGbBLwX+LnBHIJFpWl+k4I8TsDYGRRNTis54Adz/Ag2wjNFQdTjEAAAAAElFTkSuQmCC"
error64="iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAC9klEQVRYw8XWTWgdVRgG4Oece1MCpRiqV1oCrVYJlqLVUoVaaBDbRUGyaKWVBCtdqKgguGxLEXHvTkRDQWpSXKi46Fb8gYoEwYULQdCA7jSIXQg1zZ1xMecO09tJOtOf+MFwz/l+3u+dO9+8Z4KW9uIkH7yMnJWs8HUjAi+9z+zX7fBCWwL5haI5duK15H4XPwmE6TtEIAaWz9OJ4AlcRC+F/8QzWOhnbDhBljfEbUogy+mMgk3pjnuVcC/5NnVGmzdvR2AOy+AY9tSk7MExyyn3dhK4awOheFhb8dYqdTHFtoZQ1Nw2An9/WOa+ifE1UsdTTkw1t05g5aNy6nfjeAPM49gtT7W3QmAkllMPZzBWCf+DBXyX1gMbS7k6scC4aQJXs3I5jaPVEF7AJJ7CieQb2NFUU8VoR+CdafJ5sBmnhsJf4VNcSddn+HIo5xQ25/MFVmsCb0yVyxnsGgov1ZT8NbTflWqrWC0eQR9sx9kaxXyopuLBGpU9i+0JqxmBkVgKSQdvDynewO7H3VWpwH01eb2E0cnm6gfyOte/50vR2YsjqxAfw47KfhvuWSX3CPaGUGCvSaCieCEx37jGAD9SWe9cI29jwgp1Chmrp93SudL/PA7dQEMeXmVdZ4cSpqVzRa/rCGR5GegNhOQGtiPNSawZwDo7g14M156WcTB4+XxJ4CQmGgBuQTdhjDfIn8DJGIpeg4HsDqnVBE43PEm34XVkQwO5lp3G5/h50DNA/jH6RnABz7b4orqSfkdb1HyCaR1Xw3PEbK4UnX043AJoFo+ma7ZF3WHs0y/0JiS97+ILHGgI8kPSiawyS9/jsYb13+BprAzegldbNIcfkYUZwkzxEiVfUzuQegr5vC241GKQ4Bc8iT/S/l58iwdaYPyK/V1MtWwuNbqI99L+lZbNBzoy1cVBN2ePp+tW7GDEZf+fXY7pFfq9/PRcH8tTz9muaEFmEvuTusU73DzDb7gkWuxmfWKwiMX1/v+zvmuPxvW2GPgPFIGog2WqU3kAAAAASUVORK5CYII="
outImg = { base64, name ->
    outFile = new File(tmpDir + File.separator + name + ".png")
    if (! outFile.exists()){
        outFile.createNewFile()
    }
    imageOutFile = new FileOutputStream(outFile)
    imageByteArray = Base64.getDecoder().decode(base64)
    imageOutFile.write(imageByteArray)
}
outImg(yandex64, "yandex")
outImg(error64, "error")
// Info for the user
showGui = { msg, img ->
    swing = new SwingBuilder()
    popUp = swing.frame(
        title:"Yandex MT",
        pack:true,
        size:[400,100],
        preferredSize:[400,100],
        locationRelativeTo:null,
        defaultCloseOperation:WindowConstants.DISPOSE_ON_CLOSE,
        layout: new GBL()
        )
    image = new ImageIcon(tmpDir + File.separator + img + ".png")
    c = new GBC()
    c.fill = GBC.BOTH
    c.anchor = GBC.PAGE_START
    c.gridx = 0
    c.gridy = 0
    popUp.add(new JLabel(image), c)
    c.fill = GBC.HORIZONTAL
    c.anchor = GBC.CENTER
    c.gridx = 0
    c.gridy = 1
    popUp.add(new JLabel(msg), c)
    if (showPopUp) {
        popUp.show()
        Timer timer = new Timer().schedule(
            { popUp.dispose()
        } as TimerTask, popupTimeout)
    }
    if (showStatusBar) {
        mainWindow.statusLabel.setText(msg)
        Timer stimer = new Timer().schedule(
            { mainWindow.statusLabel.setText(null)
        } as TimerTask, statusTimeout)
    }
}
// API
APIkey = System.getProperty("yandex.api.key") ? System.getProperty("yandex.api.key") : null
// The actual functional part
def gui() {
    // Quit if no API key set
    if (! APIkey) {
        msg = "Yandex API key is not set. Nothing to be done."
        image="error"
        showGui(msg, image)
        console.println(msg)
        return
    }
    // Grab the active match
    match = org.omegat.core.Core.getMatcher()
    def matchtranslation
    if (match) {
        activeMatch = match.getActiveMatch() ? match.getActiveMatch() : null
    }
    // If match doesn't have properties, it must come from project_save.tmx
    if (! activeMatch.props) {
        msg = "Project match selected, no need to MT it."
        image = "error"
        showGui(msg, image)
        console.println(msg)
        return
    } else {
        rawTargLang = activeMatch.props.find {it =~ 'targetLanguage'}
        matchtranslation = activeMatch.translation
    }
    if (matchtranslation) {
        prop = project.getProjectProperties()
        target = java.net.URLEncoder.encode(matchtranslation, "UTF-8")
        targlang = prop.getTargetLanguage()
        slcode = rawTargLang.toString().replaceAll('targetLanguage=', '').replaceAll(/\-\p{L}+$/, '').toLowerCase()
        tlcode = targlang.getLanguageCode().toLowerCase()
        // If the match is already in target language, no need to send it to MT
        if (slcode == tlcode) {
            msg="Match is already in the target language, no need to MT it."
            image = "error"
            showGui(msg, image)
            console.println(msg)
            return
        }
        html = ('https://translate.yandex.net/api/v1.5/tr.json/translate?key=' + APIkey + '&lang=' + slcode + '&lang=' + tlcode + '&text=' + target)
        obj = new JsonSlurper().parseText(WikiGet.getURL(html))
        editor.replaceEditText(obj.text[0])
        msg = "Translated match inserted"
        image = "yandex"
        if (!showSuccessPopUp) {
            showPopUp = false
        }
        showGui(msg, image)
        console.println(msg)
        return
    }
}
return
