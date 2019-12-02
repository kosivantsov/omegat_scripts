/* :name=MT - Replace match with YandexMT :description=Yandex translation of the active match
 *
 * @author  Kos Ivantsov
 * @date    2019-12-02
 * @version 0.3
 */
import groovy.json.JsonSlurper
import org.omegat.util.WikiGet
APIkey = System.getProperty("yandex.api.key") ? System.getProperty("yandex.api.key") : null
if (! APIkey) {
    console.println("Yandex API key is not set")
    return
}
def gui() {
    match = org.omegat.core.Core.getMatcher()
    def matchtranslation
    if (match) {
        activeMatch = match.getActiveMatch() ? match.getActiveMatch() : null
    }
    if (! activeMatch.props) {
        console.println("Project match selected, no need to MT it.")
        return
    } else {
        rawTargLang = activeMatch.props.find {it =~ 'targetLanguage'}
        matchtranslation = activeMatch.translation
    }
    if (matchtranslation) {
        prop = project.getProjectProperties()
        ste = editor.currentEntry
        target = java.net.URLEncoder.encode(matchtranslation, "UTF-8")
        targlang = prop.getTargetLanguage()
        slcode = rawTargLang.toString().replaceAll('targetLanguage=', '').replaceAll(/\-\p{L}+$/, '').toLowerCase()
        tlcode = targlang.getLanguageCode().toLowerCase()
        if (slcode == tlcode) {
            console.println("Match is already in the target language, no need to MT it.")
            return
        }
        html = ('https://translate.yandex.net/api/v1.5/tr.json/translate?key=' + APIkey + '&lang=' + slcode + '&lang=' + tlcode + '&text=' + target)
        obj = new JsonSlurper().parseText(WikiGet.getURL(html))
        editor.replaceEditText(obj.text[0])
    }
}
