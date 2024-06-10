package `fun`.familyfunforce.cosmos.ui

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import com.simsilica.lemur.*
import com.simsilica.lemur.text.DocumentModelFilter
import com.simsilica.lemur.text.TextFilters

/**
 * A popup that includes a slider to select a number value within the given range
 */
abstract class RangePopup(min:Double, max:Double, value:Double): OptionPanel("Orbit Range"){
    private val rangeDoc = DocumentModelFilter(TextFilters.numeric()) { s -> s }
    private val rangeModel = DefaultRangedValueModel(min, max, value)
    init {
        val rangeSlider = Slider(rangeModel, Axis.X)
        rangeDoc.text = rangeModel.value.toString()
        val rangeField = TextField(rangeDoc)
        rangeField.addControl(object: AbstractControl(){
            val rangeRef = rangeModel.createReference()
            val textRef = rangeDoc.createReference()
            override fun controlUpdate(tpf: Float) {
                if(textRef.update()){
                    val txt = rangeDoc.text.ifEmpty {"0"}
                    rangeModel.value = txt.toDouble()
                }
                if(rangeRef.update()){
                    rangeDoc.text = ("%.0f".format(rangeRef.get()))
                }
            }
            override fun controlRender(rm: RenderManager?, vp: ViewPort?) {}
        })

        container.addChild(rangeField)
        container.addChild(rangeSlider)

        val conf = object : Action("Confirm"){
            override fun execute(source: Button?) {
                //slider will have updated text range, so it's more reliable
                val textRange = rangeDoc.text.toDouble()
                accept(textRange)
            }
        }
        val cancel = object: Action("Cancel"){
            override fun execute(source: Button?) {

            }
        }
        super.setOptions(cancel, conf)
    }

    abstract fun accept(value:Double)
}