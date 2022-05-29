package UI

import com.jme3.math.ColorRGBA
import com.simsilica.lemur.*
import com.simsilica.lemur.component.InsetsComponent
import com.simsilica.lemur.component.QuadBackgroundComponent
import com.simsilica.lemur.component.TbtQuadBackgroundComponent

def outline = TbtQuadBackgroundComponent.create("UI/SimpleBorders.png",1f,6,6,27,27,0,false)
def bg = new QuadBackgroundComponent(color(1,1,1,1))

selector("space"){
    fontSize=16
    color= ColorRGBA.Yellow
    font=font("UI/Orbitron12.fnt")
}

selector("button", "space"){
    background = bg.clone()
    background.setColor(ColorRGBA.DarkGray)
    background.setAlpha(0.5f)
    border=outline.clone()
    border.setColor(ColorRGBA.Orange)
}

selector("outline", "space"){
    background=bg.clone()
    background.setColor(color(0.5,0.5,0.5,0.5))
    border=outline.clone()
    border.setColor(ColorRGBA.Orange)
    insets = new Insets3f(6f,6f,6f,6f)
}