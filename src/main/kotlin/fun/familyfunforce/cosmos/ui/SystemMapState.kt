package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.Orbital
import `fun`.familyfunforce.cosmos.SpaceTraderApp
import `fun`.familyfunforce.cosmos.System
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.material.Material
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.shape.Sphere

/**
 * App state to view the current system and all the orbital bodies
 */
class SystemMapState : BaseAppState(){
    private val mapNode = Node("Map")
    private lateinit var mat:Material
    var system:System?=null
    set(value) {
        field=value
        generateSystemVisuals()
    }

    override fun initialize(app: Application) {
        mat = Material(app.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
    }

    override fun cleanup(app: Application?) {

    }

    override fun onEnable() {
        val app = application as SpaceTraderApp
        app.rootNode.attachChild(mapNode)
    }

    override fun onDisable() {
        mapNode.removeFromParent()
    }

    override fun update(tpf: Float) {

    }

    private fun generateSystemVisuals(){
        //clear previous system
        println("Generating system visuals")
        mapNode.detachAllChildren()
        system!!.orbitals.forEach {
            println("Generating $it")
            mapNode.attachChild(createOrbital(it))
        }
    }

    private fun createOrbital(orbital:Orbital): Spatial{
        val mesh = Sphere(12,12,orbital.size.toFloat())
        val geo = Geometry(orbital.name, mesh)
        geo.material = mat
        geo.localTranslation = orbital.position.toVector3f()
        return geo
    }
}