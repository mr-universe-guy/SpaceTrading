package `fun`.familyfunforce.cosmos

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState

/**
 * An app state that calls a method during the first frame the state is active
 * and then immediately removes itself from the state manager.
 * Useful for calling code exactly 1 frame after app states have been initialized.
 */
abstract class FirstFrameState: BaseAppState() {
    override fun initialize(app: Application?) {}

    override fun cleanup(app: Application?) {}

    override fun onEnable() {}

    override fun onDisable() {}

    override fun update(tpf: Float) {
        onFirstFrame()
        stateManager.detach(this)
    }

    /**
     * Called the first frame after this state has been initialized,
     * directly before this state detaches itself.
     */
    abstract fun onFirstFrame()
}