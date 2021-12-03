package `fun`.familyfunforce.cosmos

import `fun`.familyfunforce.cosmos.loadout.*
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.mathd.Vec3d
import `fun`.familyfunforce.cosmos.loadout.getVehicleFromId

fun spawnLoadout(data: EntityData, name: String, position: Vec3d, loadout: Loadout): EntityId{
    val vehicle = getVehicleFromId(loadout.vehicleId)!!
    val stats = loadout.getStats()
    val id = data.createEntity()
    data.setComponents(id,
        //vehicle specific data
        Name(name),
        Position(position),
        Velocity(0.0,0.0,0.0),
        VisualAsset(vehicle.asset),
        ObjectCategory(Category.SHIP),
        EngineDriver(Vec3d(0.0,0.0,0.0)),
        //loadout specific data
        Mass(stats[EMPTY_MASS] as Double? ?: 1.0),
        Engine(stats[MAX_SPEED] as Double? ?: 1.0, stats[MAX_THRUST] as Double? ?: 1.0),
        EnergyGridInfo(stats[EN_STORAGE] as Long? ?: 10, stats[EN_RECHARGE] as Long? ?: 10,
            stats[EN_CYCLE_TIME] as Double? ?: 1.0),
        Sensors(stats[SEN_RANGE_MAX] as Double? ?: 100.0)
    )
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
        Cargo(arrayOf()),
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
        ObjectCategory(Category.ASTEROID)
    )
    return id
}
