/**
 * Space Trader App is a 4x/rts game drawing many parallels to classics such as Eve Online, Elite and X-series, while focusing
 * on single player or small server (approx up to 8 players).
 * Design document available at https://docs.google.com/document/d/1dasutcHd1nyoX8vdQg7cWGhgYvk-QtIAebWO0qliPBo/edit
 */
package universe

import com.jme3.app.SimpleApplication
import com.simsilica.sim.GameLoop
import com.simsilica.sim.GameSystemManager
import io.tlf.jme.jfx.JavaFxUI

open class SpaceTraderApp(private val initSystems:Boolean): SimpleApplication(null){
    lateinit var manager: GameSystemManager
    lateinit var loop: GameLoop

    override fun simpleInitApp() {
        //jfx initialization
        JavaFxUI.initialize(this)
        //Game Systems
        manager = GameSystemManager()
        loop = GameLoop(manager)
        if(initSystems){
            //Turn this off to test individual systems
        }
    }

    //Cleanly destroy multi threading
    override fun destroy() {
        loop.stop()
        super.destroy()
    }
}

fun main(){
    println("Space trading app")
    SpaceTraderApp(true).start()
}