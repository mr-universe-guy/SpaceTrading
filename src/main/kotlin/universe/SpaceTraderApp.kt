/**
 * Space Trader App is a 4x/rts game drawing many parallels to classics such as Eve Online, Elite and X-series, while focusing
 * on single player or small server (approx up to 8 players).
 * Design document available at https://docs.google.com/document/d/1dasutcHd1nyoX8vdQg7cWGhgYvk-QtIAebWO0qliPBo/edit
 */
package universe

import com.jme3.app.SimpleApplication

class SpaceTraderApp: SimpleApplication(){
    override fun simpleInitApp() {

    }
}

fun main(){
    println("Space trading app")
    SpaceTraderApp().start()
}