package UI

import com.jme3.math.ColorRGBA
import com.simsilica.event.EventBus
import com.simsilica.lemur.Button
import com.simsilica.lemur.Command
import com.simsilica.lemur.Insets3f
import com.simsilica.lemur.component.QuadBackgroundComponent
import com.simsilica.lemur.component.TbtQuadBackgroundComponent
import fun.familyfunforce.cosmos.ui.UIAudioEvent

def outline = TbtQuadBackgroundComponent.create("UI/SimpleBorders.png",1f,6,6,27,27,0,false)
def bg = new QuadBackgroundComponent(color(1,1,1,1))
def brackets = TbtQuadBackgroundComponent.create("UI/Brackets.png",1f,4,4,8,8,0,false)
def activeColor = ColorRGBA.Orange
def inactiveColor = ColorRGBA.DarkGray
def primaryColor = ColorRGBA.Red

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
    background.setColor(inactiveColor)
    border=outline.clone()
    border.setColor(ColorRGBA.Orange)
    insets = new Insets3f(6f,6f,6f,6f)
}

selector("windowpane", "space"){
    background=bg.clone()
    background.setColor(ColorRGBA.DarkGray)
    border=outline.clone()
    border.setColor(ColorRGBA.DarkGray)
}

selector("hudrow", "space"){
    background=bg.clone()
    background.setColor(inactiveColor.clone())
    insets = new Insets3f(2f,2f,2f,2f)
}
selector("hudrow", "cell", "space"){
//    background=bg.clone()
//    background.setColor(ColorRGBA.Magenta)
    insets = new Insets3f(0f,5f,0f,5f)
}

selector("windowpane", "header", "space"){
    color = ColorRGBA.Pink
}

selector("bracket", "space"){
    background = brackets.clone()
    background.setColor(inactiveColor.clone())
}

selector("hudcolors", "space"){
    defaultColor = inactiveColor.clone()
    focusColor = activeColor.clone()
    targetColor = primaryColor.clone()
}

/*
selector("active", "bracket", "space"){
    background.setColor(activeColor)
}
 */