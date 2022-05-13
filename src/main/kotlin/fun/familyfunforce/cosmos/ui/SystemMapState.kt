package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.*
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.material.Material
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import com.jme3.scene.shape.Cylinder
import com.jme3.scene.shape.Sphere
import com.simsilica.es.Entity
import com.simsilica.es.EntityContainer
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.mathd.Vec3d

/**
 * App state to view the current system and all the orbital bodies
 */
class SystemMapState : BaseAppState(){
    private val mapNode = Node("Map")
    private lateinit var mat:Material
    private lateinit var container:StellarContainer
    var system:System?=null
    set(value) {
        field=value
        generateSystemVisuals()
    }

    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        mat = Material(app.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        val data = app.manager.get(DataSystem::class.java).getPhysicsData()
        container = StellarContainer(data)
    }

    override fun cleanup(app: Application?) {
        container.stop()
    }

    override fun onEnable() {
        container.start()
        val app = application as SpaceTraderApp
        app.rootNode.attachChild(mapNode)
    }

    override fun onDisable() {
        mapNode.removeFromParent()
    }

    override fun update(tpf: Float) {
        container.update()
    }

    private fun generateSystemVisuals(){
        //clear previous system
        println("Generating system visuals")
        //mapNode.detachAllChildren()
        system!!.orbitals.forEach {
            println("Generating $it")
            mapNode.attachChild(createOrbital(it))
        }
    }

    private fun createOrbital(orbital:Orbital): Spatial{
        val mesh = Sphere(12,12,orbital.size.toFloat())
        val geo = Geometry(orbital.name, mesh)
        geo.material = mat
        geo.localTranslation = orbital.globalPos.toVector3f()
        geo.addControl(object : AbstractControl(){
            override fun controlUpdate(tpf: Float) {
                spatial.localTranslation = orbital.globalPos.toVector3f()
            }
            override fun controlRender(rm: RenderManager?, vp: ViewPort?) {}
        })
        return geo
    }

    private inner class Stellar(val id:EntityId, val radius:Double, var sysId:Int, var pos:Vec3d){
        val geo = Geometry("$id", Cylinder(8,8,radius.toFloat(),radius.toFloat()))
        init {
            geo.material=mat
            geo.localTranslation=pos.toVector3f()
        }
        fun update(){
            geo.localTranslation=pos.toVector3f()
        }
    }

    private inner class StellarContainer(ed:EntityData):EntityContainer<Stellar>(ed,StellarObject::class.java,StellarPosition::class.java){
        override fun addObject(e: Entity): Stellar {
            val sobj = e.get(StellarObject::class.java)
            val spos = e.get(StellarPosition::class.java)!!
            val stellar = Stellar(e.id, sobj.radius, spos.systemId, spos.position)
            mapNode.attachChild(stellar.geo)
            println("Stellar object added")
            return stellar
        }

        override fun updateObject(stellar: Stellar, e: Entity) {
            //assume we are updating object position only
            val spos = e.get(StellarPosition::class.java)
            stellar.pos=spos.position
            stellar.update()
        }

        override fun removeObject(stellar: Stellar, e: Entity?) {
            //remove the spat
            stellar.geo.removeFromParent()
        }
    }
}