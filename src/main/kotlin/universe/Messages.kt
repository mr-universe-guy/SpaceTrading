package universe

import com.jme3.network.AbstractMessage
import com.jme3.network.serializing.Serializable

@Serializable
data class TextMessage(var message: String): AbstractMessage(true){
    constructor() : this("")
}