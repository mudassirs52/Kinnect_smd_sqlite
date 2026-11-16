package com.appVanta.i230017_i230507

object ChatManager {

    fun send(me: String, other: String, text: String, image: String, reply: Msg?, onDone: (Boolean) -> Unit = {}) {
        onDone(true)
    }

    fun edit(me: String, other: String, m: Msg, newText: String, onDone: (Boolean) -> Unit = {}) {
        onDone(true)
    }

    fun delete(me: String, other: String, m: Msg, onDone: (Boolean) -> Unit = {}) {
        onDone(true)
    }

    fun toggleHeart(me: String, other: String, m: Msg) {
        // Local fallback: no-op
    }
}
