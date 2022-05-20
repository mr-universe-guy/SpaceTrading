package UI

import com.jme3.math.ColorRGBA
import com.simsilica.lemur.*
import com.simsilica.lemur.component.QuadBackgroundComponent

def bg = new QuadBackgroundComponent(color(1,1,1,1))

selector("space"){
    fontSize=16
    color= ColorRGBA.Yellow
    font=font("UI/Orbitron12.fnt")
}

selector("container", "space"){
    background=bg.clone()
    background.setColor(color(0.5,0.5,0.5,0.5))
}