package `fun`.familyfunforce.cosmos

import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.mathd.Vec3d
import `fun`.familyfunforce.cosmos.loadout.*

fun spawnLoadout(data: EntityData, name: String, position: Vec3d, loadout: Loadout, simTime:Long): EntityId{
    val vehicle = getVehicleFromId(loadout.vehicleId)!!
    val stats = loadout.getStats().toMutableMap()
    val id = data.createEntity()
    data.setComponents(id,
        //vehicle specific data
        Name(name),
        Destroyed(false),
        Position(position),
        Velocity(0.0,0.0,0.0),
        Drag(0.01),
        VisualAsset(vehicle.asset),
        ObjectCategory(Category.SHIP),
        EngineDriver(Vec3d(0.0,0.0,0.0)),
        //temporary hp stuff, should be expanded in the future
        HealthPoints(10,10),
        Damage(0,0,0),
        //loadout specific data
        Mass(stats[EMPTY_MASS] as Double? ?: 1.0),
        Engine(stats[MAX_SPEED] as Double? ?: 1.0, stats[MAX_THRUST] as Double? ?: 1.0),
        EnergyGridInfo(stats[EN_STORAGE] as Long? ?: 10, stats[EN_RECHARGE] as Long? ?: 10,
            stats[EN_CYCLE_TIME] as Double? ?: 1.0),
        Sensors(stats[SEN_RANGE_MAX] as Double? ?: 100.0),
        HeatLimit(20),
        Heat(0),
        Overheated(false),
        CargoHold(stats[CARGO_VOLUME] as Double? ?: 0.0),
        Cargo(0.0)
    )
    //spawn entities for all of the loadouts active equipment
    //TODO: Section for each equipment needs to be accounted for
    loadout.getEquipment().forEach { loc ->
        //val location = loc.key
        //TODO: Fix this!!! ComponentEquipment should always be a new entity I think? much though should be given here
        loc.value.forEach{
            var equipId: EntityId? = null
            if(it is PoweredEquipment){
                equipId = data.createEntity()
                //default components
                data.setComponents(equipId,
                    Name(it.name),
                    CycleTimer(simTime, it.duration),
                    EquipmentPower(false),
                    Parent(id),
                    Name(it.name),
                    Activated(false),
                    ActivationConsumed(false),
                    Heat(it.heat)
                )
            }
            if(it is ComponentEquipment){
                if(equipId == null) equipId=data.createEntity()
                data.setComponents(equipId, *it.components.toTypedArray())
            }
        }
    }
    return id
}

fun spawnTestShip(data: EntityData, name: String, position: Vec3d): EntityId{
    val id = data.createEntity()
    data.setComponents(id,
        Name(name),
        Position(position),
        Mass(1.0),
        Velocity(0.0,0.0,0.0),
        CargoHold(10.0),
        Cargo(0.0),
        Engine(100.0, 10.0),
        EngineDriver(Vec3d(0.0,0.0,0.0)),
        VisualAsset("TestShip/Insurgent.gltf"),
        EnergyGridInfo(100, 10, 3.0),
        ObjectCategory(Category.SHIP),
        Sensors(75.0)
    )
    return id
}

fun spawnTestAsteroid(data: EntityData, position: Vec3d): EntityId{
    val id = data.createEntity()
    data.setComponents(id,
        Name("Asteroid"),
        Position(position),
        Velocity(0.0,0.0,0.0),
        VisualAsset("ASTEROID"),
        ObjectCategory(Category.ASTEROID),
        Mineral(ItemId("ORE"))
    )
    return id
}
