package universe

import com.jme3.math.Transform
import com.jme3.math.Vector3f
import com.simsilica.lemur.Panel
import com.simsilica.lemur.core.GuiControl
import com.simsilica.lemur.core.GuiControlListener
import com.simsilica.lemur.core.VersionedObject
import com.simsilica.lemur.core.VersionedReference

class VersionedPanel: Panel(), VersionedObject<Panel>, GuiControlListener {
    private val panel = Panel()
    private var version = 0L

    init{
        panel.getControl(GuiControl::class.java).addListener(this)
    }

    override fun getVersion(): Long {
        return version
    }

    override fun getObject(): Panel {
        return this
    }

    override fun createReference(): VersionedReference<Panel> {
        return VersionedReference(this)
    }

    override fun reshape(source: GuiControl?, pos: Vector3f?, size: Vector3f?) {
        println("Reshape event")
        version++
    }

    override fun focusGained(source: GuiControl?) {

    }

    override fun focusLost(source: GuiControl?) {

    }

    override fun setLocalTransform(t: Transform?) {
        super.setLocalTransform(t)
        version++
    }

    override fun setLocalTranslation(localTranslation: Vector3f?) {
        super.setLocalTranslation(localTranslation)
        version++
    }

    override fun setLocalTranslation(x: Float, y: Float, z: Float) {
        super.setLocalTranslation(x, y, z)
        version++
    }

    override fun setSize(size: Vector3f?) {
        super.setSize(size)
        version++
    }

    override fun setPreferredSize(size: Vector3f?) {
        super.setPreferredSize(size)
        version++
    }
}