package universe

import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.mathd.Vec3d

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
        EnergyGridInfo(100, 10, 3.0)
    )
    return id
}

fun spawnTestAsteroid(data: EntityData, position: Vec3d): EntityId{
    val id = data.createEntity()
    data.setComponents(id,
        Name("Asteroid"),
        Position(position),
        Velocity(0.0,0.0,0.0),
        VisualAsset("ASTEROID")
    )
    return id
}
