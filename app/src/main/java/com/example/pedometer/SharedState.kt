package com.example.pedometer

class SharedState  // Restrict the constructor from being instantiated
private constructor() {
    // sets the steps detected
    // gets the steps saved in the state
    var steps = 0
    private var goal = 0

    val remainingGoal: Int
        // gets the remaining steps
        get() = goal - steps

    // sets the step goal
    fun setGoal(goal: Int) {
        this.goal = goal
    }

    companion object {
        @get:Synchronized
        var instance: SharedState? = null
            // Singleton instance of the class.
            get() {
                if (field == null) {
                    field = SharedState()
                }
                return field
            }
            private set
    }
}