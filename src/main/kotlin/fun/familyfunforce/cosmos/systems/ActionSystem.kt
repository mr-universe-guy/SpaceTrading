package `fun`.familyfunforce.cosmos.systems

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.network.Client
import com.jme3.network.service.rmi.RmiClientService
import com.jme3.network.service.rmi.RmiHostedService
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.event.EventBus
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.*
import `fun`.familyfunforce.cosmos.event.ApproachOrderEvent
import `fun`.familyfunforce.cosmos.event.EquipmentToggleEvent
import `fun`.familyfunforce.cosmos.event.OrbitOrderEvent
import `fun`.familyfunforce.cosmos.event.ThrottleOrderEvent

interface ActionRMI{
    fun setAction(id:EntityId, action: Action)
    fun setThrottle(id:EntityId, throttle:Double)
    fun setEquipmentPower(id:EntityId, powered:Boolean)
}

/**
 * Actions are the micro level AI decisions performed by units in a fleet. These will need to be very cheap as every fleet
 * that is in simulated space will run one action per unit.
 */
class ActionSystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private val unitActions = HashMap<EntityId, Action>()

    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        val server = getSystem(ServerSystem::class.java).server
        //register an rmi service for user input
        val rmi = object: ActionRMI {
            override fun setAction(id: EntityId, action: Action) {
                manager.enqueue { unitActions[id] = action }
            }

            override fun setThrottle(id: EntityId, throttle: Double) {
                manager.enqueue { unitActions[id]!!.setThrottle(throttle) }
            }

            override fun setEquipmentPower(id: EntityId, powered: Boolean) {
                println("Server is setting equipment for $id to power:$powered")
                data.setComponent(id, EquipmentPower(powered))
            }

        }
        server.services.getService(RmiHostedService::class.java).shareGlobal(rmi, ActionRMI::class.java)
    }

    override fun terminate() {

    }

    override fun update(time: SimTime) {
        unitActions.forEach { (entityId, action) ->
            //This may not be a great way of doing things when we have a massive number of ships
            val status = action.update(entityId, data, time)
            //if the status has changed assign a new status info
            if(status != action.getStatus()){
                action.setStatus(status)
                data.setComponent(entityId, ActionInfo(action.toString(), status))
            }
        }
    }

    fun setAction(id: EntityId, action: Action?){
        if(action == null){
            unitActions.remove(id)
            data.removeComponent(id, ActionInfo::class.java)
        } else{
            unitActions[id] = action
            data.setComponent(id,
                ActionInfo(
                    action.toString(),
                    ActionStatus.STARTING
                )
            )
        }
    }

    fun getAction(id: EntityId): Action?{
        return unitActions[id]
    }
}

/**
 * A class that listens to events intended to be forwarded to the server
 */
class ClientActionEventResponder: BaseAppState(){
    private lateinit var rmiHandler: ActionRMI
    private lateinit var client: Client

    override fun initialize(app: Application?) {
        client = getState(ClientState::class.java).client
        rmiHandler = client.services.getService(RmiClientService::class.java).getRemoteObject(ActionRMI::class.java)
    }

    fun orbitTarget(orb: OrbitOrderEvent){rmiHandler.setAction(orb.shipId, OrbitAction(orb.targetId, orb.range))}

    fun setThrottle(evt: ThrottleOrderEvent){rmiHandler.setThrottle(evt.shipId, evt.throttle)}

    fun approachTarget(evt: ApproachOrderEvent){rmiHandler.setAction(evt.shipId, ApproachAction(evt.targetId, evt.range))}

    fun setEquipmentPower(evt: EquipmentToggleEvent){
        rmiHandler.setEquipmentPower(evt.equipId, evt.powered)
    }

    override fun cleanup(app: Application?) {}

    override fun onEnable() {
        EventBus.addListener(this, OrbitOrderEvent.orbitTarget, ThrottleOrderEvent.setThrottle,
            ApproachOrderEvent.approachTarget, EquipmentToggleEvent.setEquipmentPower)
    }

    override fun onDisable() {}
}