package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.ClientState
import `fun`.familyfunforce.cosmos.ServerSystem
import `fun`.familyfunforce.cosmos.SpaceTraderApp
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.scene.Node
import com.simsilica.lemur.*
import com.simsilica.lemur.component.BoxLayout

/**
 * Menu for joining or hosting online games
 */
class OnlineState: BaseAppState() {
    private val menuNode = Node("Online Menu")
    lateinit var menus: TabbedPanel

    override fun initialize(_app: Application?) {
        val app = _app as SpaceTraderApp
        menus = TabbedPanel()
        val joinTab = Container(BoxLayout(Axis.Y, FillMode.None))
        val joinIp = TextField("")
        joinIp.preferredWidth=200f
        joinTab.addChild(joinIp)
        val joinButton = Button("Connect")
        joinButton.addClickCommands {
            val ip = joinIp.documentModel.text.trim()
            println("Connecting to $ip")
            app.serverManager.get(ClientState::class.java).connectTo(ip)
        }
        joinTab.addChild(joinButton)
        menus.addTab("Join", joinTab)
        val hostTab = Container(BoxLayout(Axis.Y, FillMode.None))
        val server = app.serverManager.get(ServerSystem::class.java)
        val hostButton = Button("Host")
        server.addServerStatusListener { hostButton.isEnabled = ServerSystem.ServerStatus.CLOSED == it }
        hostButton.addClickCommands {
            println("Hosting local server")
            app.serverManager.get(ServerSystem::class.java).startServer()
        }
        hostTab.addChild(hostButton)
        menus.addTab("Host", hostTab)
        menuNode.attachChild(menus)
    }

    override fun cleanup(app: Application?) {

    }

    override fun onEnable() {
        val app = application as SpaceTraderApp
        GuiGlobals.getInstance().popupState.centerInGui(menus)
        app.guiNode.attachChild(menuNode)
    }

    override fun onDisable() {
        menuNode.removeFromParent()
    }
}