package `fun`.familyfunforce.cosmos.ui

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.audio.AudioData
import com.jme3.audio.AudioNode
import com.simsilica.event.EventBus
import com.simsilica.event.EventType

class UIAudioState:BaseAppState() {
    private val audioMap:MutableMap<String, AudioNode> = mutableMapOf()
    override fun initialize(app: Application) {
        //pre-load ui audio
        val am = app.assetManager
        audioMap["click.wav"] = AudioNode(am, "UI/click.wav", AudioData.DataType.Buffer)
    }

    override fun cleanup(app: Application?) {
    }

    override fun onEnable() {
        EventBus.addListener(this, UIAudioEvent.playAudio)
    }

    override fun onDisable() {
        EventBus.removeListener(this, UIAudioEvent.playAudio)
    }

    fun playAudio(evt:UIAudioEvent){
        application.enqueue { audioMap[evt.source]!!.playInstance() }
    }
}

class UIAudioEvent(val source:String){
    companion object{
        @JvmStatic fun createUIAudioEvent(source:String):UIAudioEvent{
            return UIAudioEvent(source)
        }
        @JvmField val playAudio: EventType<UIAudioEvent> = EventType.create("playAudio", UIAudioEvent::class.java)
    }
}