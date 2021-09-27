import com.jme3.system.AppSettings
import universe.*

class LoadoutEditorDemo: SpaceTraderApp(false){
    override fun simpleInitApp() {
        super.simpleInitApp()
        //create some test vehicles
        buildTestVehicle()
        //build some example equipment
        cacheEquipment(EngineEquip("Eng-001", "Basic Engine", 2, 20, 100.0, 10.0))
        cacheEquipment(CargoEquip("Hld-001", "Basic Cargo Hold", 2, 20, 25.0))
        stateManager.attach(LoadoutEditorState())
    }
}

fun main(){
    val app = LoadoutEditorDemo()
    app.isShowSettings = false
    val settings = AppSettings(true)
    settings.setResolution(1280, 720)
    app.setSettings(settings)
    app.start()
}