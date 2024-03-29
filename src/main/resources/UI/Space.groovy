package UI

import com.jme3.math.ColorRGBA
import com.simsilica.event.EventBus
import com.simsilica.lemur.*
import com.simsilica.lemur.component.QuadBackgroundComponent
import com.simsilica.lemur.component.TbtQuadBackgroundComponent
import fun.familyfunforce.cosmos.ui.UIAudioEvent

def outline = TbtQuadBackgroundComponent.create("UI/SimpleBorders.png",1f,6,6,27,27,0,false)
def bg = new QuadBackgroundComponent(color(1,1,1,1))
def brackets = TbtQuadBackgroundComponent.create("UI/Brackets.png",1f,4,4,8,8,0,false)
def activeColor = ColorRGBA.Orange
def inactiveColor = ColorRGBA.DarkGray

selector("space"){
    fontSize=16
    color= ColorRGBA.Yellow
    font=font("UI/Orbitron12.fnt")
}

Command<Button> clickSound = {if(it.isPressed()){EventBus.publish(UIAudioEvent.playAudio, UIAudioEvent.createUIAudioEvent("click.wav"))}}
Command<Button> clickShift = {if(it.isPressed()){it.move(1,-1,0)} else{it.move(-1,1,0)}}
Command<Button> buttonFade = {Button it ->it.background.color=inactiveColor}
Command<Button> buttonUnFade = {Button it ->it.background.color=activeColor}
def stdButtonCommands = [
        (Button.ButtonAction.Down):[clickSound,clickShift],
        (Button.ButtonAction.Up):[clickShift],
        (Button.ButtonAction.Enabled):[buttonUnFade],
        (Button.ButtonAction.Disabled):[buttonFade]
]
selector("button", "space"){
    background = outline.clone()
    background.setColor(ColorRGBA.DarkGray)
    buttonCommands=stdButtonCommands
}

selector("outline", "space"){
    background=bg.clone()
    background.setColor(color(0.5,0.5,0.5,0.5))
    border=outline.clone()
    border.setColor(ColorRGBA.Orange)
    insets = new Insets3f(6f,6f,6f,6f)
}

selector("bracket", "space"){
    background=brackets.clone()
    background.setColor(ColorRGBA.Green)
}