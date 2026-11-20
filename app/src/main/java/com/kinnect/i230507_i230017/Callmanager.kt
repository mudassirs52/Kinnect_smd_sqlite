package com.kinnect.i230507_i230017

import android.content.Intent

data class CallInfo(
    var callId: String = "",
    var callerUid: String = "",
    var callerName: String = "",
    var calleeUid: String = "",
    var calleeName: String = "",
    var type: String = "voice",      // "voice" or "video"
    var status: String = "ringing",  // ringing | accepted | declined | ended | missed
    var channel: String = "",
    var createdAt: Long = 0L,
    var acceptedAt: Long = 0L,
    var endedAt: Long = 0L
)

fun CallInfo.isVideo(): Boolean = type == "video"

fun Intent.putCall(call: CallInfo): Intent {
    putExtra("callId", call.callId)
    putExtra("callerUid", call.callerUid)
    putExtra("callerName", call.callerName)
    putExtra("calleeUid", call.calleeUid)
    putExtra("calleeName", call.calleeName)
    putExtra("type", call.type)
    putExtra("channel", call.channel)
    return this
}

fun Intent.readCall(): CallInfo? {
    val id = getStringExtra("callId") ?: return null
    return CallInfo(
        callId = id,
        callerUid = getStringExtra("callerUid") ?: "",
        callerName = getStringExtra("callerName") ?: "",
        calleeUid = getStringExtra("calleeUid") ?: "",
        calleeName = getStringExtra("calleeName") ?: "",
        type = getStringExtra("type") ?: "voice",
        channel = getStringExtra("channel") ?: ""
    )
}

object CallManager {

    const val RING_TIMEOUT_MS = 45_000L
    val FINAL = setOf("declined", "ended", "missed")

    /** Id of the call this device is currently in (null when idle). */
    @Volatile
    var activeCallId: String? = null

    fun start(
        peerUid: String,
        peerName: String,
        type: String,
        onReady: (CallInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val ctx=DataRepository.context
        val main=android.os.Handler(android.os.Looper.getMainLooper())
        DataRepository.io.execute {
            try {
                val result=DataRepository.request("call_create",org.json.JSONObject().put("callee_id",peerUid).put("type",type).put("action_uuid",java.util.UUID.randomUUID().toString()))
                val call=parse(result.getJSONObject("call"));main.post{onReady(call)}
            }catch(e:Exception){main.post{onError(e.message?:"Call failed")}}
        }
    }
    fun accept(call: CallInfo,onDone:()->Unit,onError:(String)->Unit) {
        DataRepository.io.execute {
            try{DataRepository.request("call_update",org.json.JSONObject().put("call_id",call.callId).put("status","accepted").put("action_uuid",java.util.UUID.randomUUID().toString()));android.os.Handler(android.os.Looper.getMainLooper()).post{onDone()}}
            catch(e:Exception){android.os.Handler(android.os.Looper.getMainLooper()).post{onError(e.message?:"Cannot answer")}}
        }
    }
    fun finish(callId: String,wantedStatus: String,durationSec: Long=0) {
        DataRepository.mutate("call_update",org.json.JSONObject().put("call_id",callId).put("status",wantedStatus).put("duration_sec",durationSec))
    }
    private fun parse(c: org.json.JSONObject)=CallInfo(c.getString("id"),c.getString("caller_id"),c.optString("caller_name"),c.getString("callee_id"),c.optString("callee_name"),c.optString("type"),c.optString("status"),c.optString("channel"),c.optLong("created_at"),c.optLong("accepted_at"),c.optLong("ended_at"))
    private fun poll(endpoint: String,params: org.json.JSONObject,onChange:(CallInfo?)->Unit):()->Unit {
        val handler=android.os.Handler(android.os.Looper.getMainLooper());var alive=true
        val task=object:Runnable{override fun run(){if(!alive)return;DataRepository.read(endpoint,params){j->if(alive)onChange(j.optJSONObject("call")?.let{parse(it)})};handler.postDelayed(this,3000)}}
        handler.post(task);return{alive=false;handler.removeCallbacks(task)}
    }
    fun observe(callId: String,onChange:(CallInfo?)->Unit):()->Unit=poll("call_get",org.json.JSONObject().put("call_id",callId),onChange)
    fun observeIncoming(onCall:(CallInfo)->Unit):()->Unit=poll("calls_incoming",org.json.JSONObject()){it?.let(onCall)}
}
