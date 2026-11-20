package com.kinnect.i230507_i230017
import android.content.Context
import android.content.Intent
import org.json.JSONObject
object SessionManager {
    fun goOnline(){PresenceManager.start(DataRepository.context)}
    fun logout(context: Context,discardUnsynced: Boolean=false) {
        DataRepository.io.execute {
            if(!discardUnsynced && DataRepository.db.actions().isNotEmpty()) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    androidx.appcompat.app.AlertDialog.Builder(context).setTitle("Unsynced changes")
                        .setMessage("Some changes have not reached the server. Sync them first, or discard them when logging out.")
                        .setPositiveButton("Sync"){_,_->DataRepository.schedule()}
                        .setNeutralButton("Discard and log out"){_,_->logout(context,true)}
                        .setNegativeButton("Cancel",null).show()
                };return@execute
            }
            try {
                DataRepository.request("logout",JSONObject().put("fcm_token",context.getSharedPreferences("push",Context.MODE_PRIVATE).getString("token","")?:""))
                synchronized(DataRepository){RecentLoginStore.clear(context);DataRepository.db.clear()}
                java.io.File(context.filesDir,"outbox").deleteRecursively()
                java.io.File(context.filesDir,"media").deleteRecursively()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    PresenceManager.stop();androidx.work.WorkManager.getInstance(context).cancelUniqueWork("kinnect-outbox")
                    context.startActivity(Intent(context,LoginActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK})
                }
            } catch(e:Exception){android.os.Handler(android.os.Looper.getMainLooper()).post{android.widget.Toast.makeText(context,"Connect to log out and revoke your session",android.widget.Toast.LENGTH_LONG).show()}}
        }
    }
}
