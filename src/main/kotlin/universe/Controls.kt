package universe

import com.simsilica.lemur.input.Axis
import com.simsilica.lemur.input.Button
import com.simsilica.lemur.input.FunctionId
import com.simsilica.lemur.input.InputMapper

const val CAM_INPUT_GROUP = "CamInput"
val CAM_INPUT_HORIZONTAL = FunctionId(CAM_INPUT_GROUP, "horiz_cam")
val CAM_INPUT_VERTICAL = FunctionId(CAM_INPUT_GROUP, "vert_cam")
val CAM_INPUT_ZOOM = FunctionId(CAM_INPUT_GROUP, "zoom_cam")

fun registerDefaults(mapper: InputMapper){
    //CAMERA
    mapper.map(CAM_INPUT_HORIZONTAL, Axis.MOUSE_X, Button.MOUSE_BUTTON2)
    mapper.map(CAM_INPUT_VERTICAL, Axis.MOUSE_Y, Button.MOUSE_BUTTON2)
    mapper.map(CAM_INPUT_ZOOM, Axis.MOUSE_WHEEL, Button.MOUSE_BUTTON2)
}