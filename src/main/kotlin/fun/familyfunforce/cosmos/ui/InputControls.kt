package `fun`.familyfunforce.cosmos.ui

import com.jme3.input.KeyInput
import com.simsilica.lemur.input.*

const val CAM_INPUT_GROUP = "CamInput"
val CAM_INPUT_YAW = FunctionId(CAM_INPUT_GROUP, "yaw_cam")
val CAM_INPUT_PITCH = FunctionId(CAM_INPUT_GROUP, "pitch_cam")
val CAM_INPUT_ZOOM = FunctionId(CAM_INPUT_GROUP, "zoom_cam")
val CAM_INPUT_HOLDTOROTATE = FunctionId(CAM_INPUT_GROUP, "hold_to_rot_cam")

const val SHIP_INPUT_GROUP = "ShipInput"
val SHIP_NEXT_TARGET = FunctionId(SHIP_INPUT_GROUP, "next_target")

fun registerDefaults(mapper: InputMapper){
    //CAMERA
    mapper.map(CAM_INPUT_YAW, Axis.MOUSE_X)
    mapper.map(CAM_INPUT_PITCH, Axis.MOUSE_Y)
    mapper.map(CAM_INPUT_ZOOM, Axis.MOUSE_WHEEL)
    mapper.map(CAM_INPUT_HOLDTOROTATE, Button.MOUSE_BUTTON2)
    //ship shortcuts
    mapper.map(SHIP_NEXT_TARGET, InputState.Positive, KeyInput.KEY_T)
}