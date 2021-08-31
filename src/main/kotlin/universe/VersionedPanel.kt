package universe

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
        val uic = panel.getControl(GuiControl::class.java)
        uic.addListener(this)
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
        version++
    }

    override fun setPreferredSize(size: Vector3f?) {
        super.setPreferredSize(size)
        version++
    }

    override fun focusGained(source: GuiControl?) {}

    override fun focusLost(source: GuiControl?) {}

    override fun setTransformRefresh() {
        super.setTransformRefresh()
        version++
    }

}