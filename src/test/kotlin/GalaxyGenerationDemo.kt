import `fun`.familyfunforce.cosmos.Galaxy
import `fun`.familyfunforce.cosmos.System
import `fun`.familyfunforce.cosmos.generateGalaxy
import javafx.application.Application
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage

class GalaxyGenerationDemo: Application() {
    override fun start(primaryStage: Stage) {
        val format = "%.2f"
        //spacer
        val spacer = Region()
        VBox.setVgrow(spacer, Priority.ALWAYS)
        //box for all our options
        val optionBox = VBox(5.0)
        optionBox.children.add(Label("Galaxy Name:"))
        val nameField = TextField("New Galaxy")
        optionBox.children.add(nameField)
        optionBox.children.add(Label("Number of Systems:"))
        val numSystemsSpin = Spinner<Int>(1,1000,100)
        numSystemsSpin.isEditable = true
        optionBox.children.add(numSystemsSpin)
        val radiusSlider = Slider(10.0, 100.0, 1.0)
        val radiusLabel = Label("Radius: ${radiusSlider.value}")
        optionBox.children.add(radiusLabel)
        radiusSlider.valueProperty().addListener { _, _, newValue ->radiusLabel.text = "Radius: ${format.format(newValue)}" }
        optionBox.children.add(radiusSlider)

        val gravitySlider = Slider(1.0, 10.0, 5.0)
        val gravityLabel = Label("Gravity: ${gravitySlider.value}")
        gravitySlider.valueProperty().addListener { _, _, newValue -> gravityLabel.text = "Gravity: ${format.format(newValue)}"}
        optionBox.children.add(gravityLabel)
        optionBox.children.add(gravitySlider)
        //
        optionBox.children.add(spacer)
        val generateBut = Button("Generate")
        optionBox.children.add(generateBut)
        //image pane
        val width = 512.0
        val height = 512.0
        val canvas = Canvas(width,height)
        val gc = canvas.graphicsContext2D
        gc.fill = Color.DARKGRAY
        gc.fillRect(0.0,0.0,width,height)
        gc.translate(width/2, height/2)
        //group for the scene? not completely sure how this works
        val group = BorderPane()
        group.padding = Insets(10.0)
        group.center = canvas
        group.left = optionBox
        val scene = Scene(group,800.0,600.0)
        primaryStage.title = "Galaxy Generation Demo"
        primaryStage.scene = scene
        primaryStage.show()
        //
        generateBut.onAction = EventHandler {
            //gather data from all controls to generate and draw a galaxy
            val gName = nameField.text
            val size = numSystemsSpin.value
            val radius = radiusSlider.value
            val gravity = gravitySlider.value
            val galaxy = generateGalaxy(gName, size, radius, gravity)
            drawGalaxy(galaxy, gc)
        }
    }

    private fun drawGalaxy(galaxy: Galaxy, gc:GraphicsContext){
        println("Drawing Galaxy")
        gc.fill = Color.DARKGRAY
        val width = gc.canvas.width
        val height = gc.canvas.height
        gc.fillRect(-width/2, -height/2, width, height)
        val radius = galaxy.radius
        //scale factor = 1/2 width/radius
        val scalar = 0.5*width/radius
        println("Scale set to $scalar")
        //add a point for each star system
        galaxy.systems.forEach {
            drawSystem(it, gc, scalar)
        }
    }

    private fun drawSystem(sys:System, gc:GraphicsContext, scale:Double){
        gc.stroke = Color.GOLD
        gc.strokeOval(sys.position.x*scale, sys.position.z*scale, 2.0,2.0)
    }

    fun launch(){
        Application.launch()
    }
}

fun main(){
    val app = GalaxyGenerationDemo()
    app.launch()
}