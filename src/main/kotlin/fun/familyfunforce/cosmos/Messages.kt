package `fun`.familyfunforce.cosmos

import com.jme3.network.AbstractMessage
import com.jme3.network.serializing.Serializable

/*
 * All abstract messages need an empty constructor to be properly serialized!
 * All data must be stored in var's to allow for proper serialization! VAL's will fail to deserialize!
 */

@Serializable
data class TextMessage(var message: String): AbstractMessage(true){
    constructor() : this("")
}