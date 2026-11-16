package com.appVanta.i230017_i230507

object FriendManager {

    enum class State { NONE, SENT, RECEIVED, FRIENDS }

    private fun me(): String? = "local_user_123"

    private fun commit(updates: HashMap<String, Any?>, onDone: (Boolean) -> Unit) {
        onDone(true)
    }

    fun sendRequest(other: String, onDone: (Boolean) -> Unit = {}) {
        onDone(true)
    }

    fun cancelRequest(other: String, onDone: (Boolean) -> Unit = {}) {
        onDone(true)
    }

    fun deleteRequest(other: String, onDone: (Boolean) -> Unit = {}) {
        onDone(true)
    }

    fun confirm(other: String, onDone: (Boolean) -> Unit = {}) {
        onDone(true)
    }

    fun unfriend(other: String, onDone: (Boolean) -> Unit = {}) {
        onDone(true)
    }

    fun observeState(other: String, onState: (State) -> Unit): () -> Unit {
        onState(State.NONE)
        return {}
    }
}
