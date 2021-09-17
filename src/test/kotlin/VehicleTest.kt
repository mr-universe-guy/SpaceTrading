import com.jme3.asset.DesktopAssetManager
import com.jme3.asset.plugins.ClasspathLocator
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import universe.*
import java.io.File
import javax.swing.JFileChooser

fun main(vararg args: String) {
    if(args.isNotEmpty()){
        when(args[0]){
            "Vehicle" -> testVehicle()
            "Loadout" -> testLoadout()
            "Asset" -> testAssetLoad()
        }
    } else{
        val lines = arrayOf(
            "Must pass an argument:",
            "'Vehicle' - save a test vehicle file in json format",
            "'Loadout' - save a test loadout file in json format"
        )
        lines.forEach { println(it) }
    }
}

fun testAssetLoad() {
    //create an asset manager
    val am = DesktopAssetManager()
    am.registerLoader(VehicleLoader::class.java, "ship")
    am.registerLocator("", ClasspathLocator::class.java)
    val vehicle = am.loadAsset(VehicleKey("TestShip/test.ship"))
    println(vehicle)
}

fun buildVehicle(): Vehicle {
    //first create a few sections
    val fuseSect = Section("Fuselage", 9, 10, listOf(Bay(3, setOf(EquipmentType.ENGINE))))
    val lWingSect = Section("Left Wing", 4, 5, listOf(Bay(2, setOf(EquipmentType.WEAPON))))
    val rWingSect = Section("Right Wing", 4, 5, listOf(Bay(2, setOf(EquipmentType.WEAPON))))
    val sections = arrayOf(fuseSect, lWingSect, rWingSect)
    //attach some things
    return Vehicle("Test", "T-001", 100, 1.0, "TestShip.Insurgent.gltf", Category.SHIP, sections)
}

fun testLoadout() {
    println("Testing loadout save/load")
    val testVic = buildVehicle()
    //attach some equipment to this vic
    val engine = EngineEquip("Engine1","TestEngine", 3,30, 100.0, 10.0)
    val cargoHold = CargoEquip("CargoPod","TestCargoHold", 3, 10, 10.0)
    val energyGrid = EnergyGridEquip("Reactor","TestEnergyGrid", 3, 30, 100, 10, 3.0)
    val loadout = Loadout("Test Loadout", testVic)
    loadout.attachEquipment("Fuselage", engine)
    loadout.attachEquipment("Fuselage", cargoHold)
    loadout.attachEquipment("Fuselage", energyGrid)
    val file: File
    val chooser = JFileChooser("")
    if(chooser.showSaveDialog(null) == JFileChooser.CANCEL_OPTION){
        println("Closing loadout test")
        return
    }
    file = chooser.selectedFile
    file.writeText(VEHICLE_FORMAT.encodeToString(loadout))
    //read back
    val readLoadout: Loadout = VEHICLE_FORMAT.decodeFromString(file.readText())
    println(readLoadout)
}

fun testVehicle(){
    println("Testing vehicle save/load")
    val file: File
    val chooser = JFileChooser("")
    if(chooser.showOpenDialog(null) == JFileChooser.CANCEL_OPTION) {
        println("Closing vehicle save/load test, no file chosen")
        return
    }
    file = chooser.selectedFile
    //write this vehicle to file
    val testVic = buildVehicle()
    file.writeText(VEHICLE_FORMAT.encodeToString(testVic))
    //read back values
    val readVic = VEHICLE_FORMAT.decodeFromString<Vehicle>(file.readText())
    println(readVic)
}