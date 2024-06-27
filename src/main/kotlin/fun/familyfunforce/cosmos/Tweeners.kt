package `fun`.familyfunforce.cosmos

import com.jme3.math.ColorRGBA
import com.simsilica.lemur.anim.AbstractTween

class ColorTweener(private val startColor: ColorRGBA, private val endColor: ColorRGBA, val color: ColorRGBA, length: Double): AbstractTween(length){
    override fun doInterpolate(t: Double) {
        color.interpolateLocal(startColor, endColor, t.toFloat())
    }
}