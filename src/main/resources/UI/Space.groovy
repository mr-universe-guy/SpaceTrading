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
def clickSound = new Command<Button>(){
    void execute(Button source){
        if(source.isPressed()){
            EventBus.publish(UIAudioEvent.playAudio, UIAudioEvent.createUIAudioEvent("click.wav"))
        }
    }
}
def clickShift = new Command<Button>(){
    void execute(Button source){
        if(source.isPressed()){
            source.move(1,-1,0)
        } else{
            source.move(-1,1,0)
        }
    }
}
def stdButtonCommands = [
        (Button.ButtonAction.Down):[clickSound,clickShift],
        (Button.ButtonAction.Up):[clickShift]
]

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
    buttonCommands=stdButtonCommands
}

selector("outline", "space"){
    background=bg.clone()
    background.setColor(color(0.5,0.5,0.5,0.5))
    border=outline.clone()
    border.setColor(ColorRGBA.Orange)
    insets = new Insets3f(6f,6f,6f,6f)
}

selector("brackets", "space"){
    background=brackets.clone()
    background.setColor(ColorRGBA.Green)
}