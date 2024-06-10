package `fun`.familyfunforce.cosmos.ui

import com.jme3.input.event.MouseButtonEvent
import com.jme3.input.event.MouseMotionEvent
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import com.simsilica.lemur.*
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.BoxLayout
import com.simsilica.lemur.core.GuiControl
import com.simsilica.lemur.event.*
import com.simsilica.lemur.style.ElementId

/**
 * A panel that has a header that can be dragged and closed
 * TODO: style stuff
 */
class WindowPane private constructor(headerText:String, content:Panel?, applyStyles: Boolean, elementId: ElementId?, style: String?):
    Panel(applyStyles, elementId, style) {
    companion object{
        val ELEMENT_ID = ElementId("windowpane")
        val HEADER_ID: ElementId = ELEMENT_ID.child("header")
    }
    constructor(headerText: String, content: Panel?): this(headerText, content, true, ELEMENT_ID, null)

    constructor(headerText: String): this(headerText, null, true, ELEMENT_ID, null)

    private val headerTextLabel = Label(headerText, HEADER_ID)
    var headerText: String
        get() {return headerTextLabel.text}
        set(value) {headerTextLabel.text = value}

    private val headerContainer: Container = Container(BoxLayout(Axis.X, FillMode.None))

    var content: Panel? = content
        set(value) {
            getControl(GuiControl::class.java).getLayout<BorderLayout>().addChild(value, BorderLayout.Position.Center)
            field = value
        }

    init {
        val gui = getControl(GuiControl::class.java)
        val layout = BorderLayout()
        gui.setLayout(layout)
        headerTextLabel.text = headerText
        headerContainer.addChild(headerTextLabel)
        headerContainer.addMouseListener(WindowDragHandler(this))
        val closeButton = Button("X")
        closeButton.addClickCommands {
            this.removeFromParent()
        }
        headerContainer.addChild(closeButton)
        layout.addChild(BorderLayout.Position.North, headerContainer)
        if(content != null){
            layout.addChild(BorderLayout.Position.Center, content)
        }
    }
    private class WindowDragHandler(val dragTarget: Spatial): MouseListener {
        private var drag = false
        private var origin: Vector3f? = null
        private var offset: Vector3f? = null

        override fun mouseButtonEvent(event: MouseButtonEvent, target: Spatial?, capture: Spatial?) {
            drag = event.isPressed
            if(drag){
                if(offset == null){
                    origin = dragTarget.localTranslation
                    offset = Vector3f(event.x.toFloat(), event.y.toFloat(), 0f)
                }
            } else {
                origin = null
                offset = null
            }
        }

        override fun mouseEntered(event: MouseMotionEvent?, target: Spatial?, capture: Spatial?) {
        }

        override fun mouseExited(event: MouseMotionEvent?, target: Spatial?, capture: Spatial?) {
        }

        override fun mouseMoved(event: MouseMotionEvent, target: Spatial?, capture: Spatial?) {
            if(!drag) return
            val pos = Vector3f(event.x.toFloat(), event.y.toFloat(), 0f)
            val delta = pos.subtract(offset)
            dragTarget.localTranslation = origin!!.add(delta.x, delta.y, 0f)
            offset = pos
        }
    }
}