package com.kinnect.i230507_i230017

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.work.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object DataRepository {
    lateinit var context: Context; private set
    lateinit var db: OfflineStore; private set
    val io=Executors.newSingleThreadExecutor()
    private val network=Executors.newFixedThreadPool(4)
    private val inFlight=java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val main=Handler(Looper.getMainLooper())
    val listeners=CopyOnWriteArraySet<() -> Unit>()
    fun init(ctx: Context) {context=ctx.applicationContext;db=OfflineStore(context)}
    fun changed() {main.post {listeners.forEach {it()}}}
    fun schedule() {
        WorkManager.getInstance(context).enqueueUniqueWork("kinnect-outbox",ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,10,TimeUnit.SECONDS).build())
    }
    class ApiException(val status: Int,message: String): Exception(message)
    fun request(endpoint: String,params: JSONObject=JSONObject(),files: JSONArray=JSONArray(),get: Boolean=false,token: String?=RecentLoginStore.getToken(context)): JSONObject {
        fun encode(s: String)=URLEncoder.encode(s,"UTF-8")
        val encoded=params.keys().asSequence().joinToString("&") {encode(it)+"="+encode(params.optString(it))}
        val conn=URL(ApiClient.BASE_URL+endpoint+".php"+(if(get && encoded.isNotEmpty())"?$encoded" else "")).openConnection() as HttpURLConnection
        conn.connectTimeout=12000;conn.readTimeout=30000;conn.requestMethod=if(get)"GET" else "POST"
        token?.takeIf{it.isNotEmpty()}?.let{conn.setRequestProperty("Authorization","Bearer $it")}
        try {
            if(!get) {
                conn.doOutput=true
                if(files.length()==0) {
                    conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8")
                    val bytes=encoded.toByteArray(Charsets.UTF_8);conn.setFixedLengthStreamingMode(bytes.size);conn.outputStream.use{it.write(bytes)}
                } else {
                    val boundary="Kinnect"+UUID.randomUUID().toString().replace("-","")
                    conn.setRequestProperty("Content-Type","multipart/form-data; boundary=$boundary");conn.setChunkedStreamingMode(16384)
                    DataOutputStream(conn.outputStream).use {out ->
                        fun line(s: String){out.write(s.toByteArray(Charsets.UTF_8))}
                        for(k in params.keys())line("--$boundary\r\nContent-Disposition: form-data; name=\"$k\"\r\n\r\n${params.optString(k)}\r\n")
                        for(i in 0 until files.length()) {
                            val f=files.getJSONObject(i);val file=File(f.getString("path"))
                            line("--$boundary\r\nContent-Disposition: form-data; name=\"${f.getString("field")}\"; filename=\"${file.name}\"\r\nContent-Type: ${f.optString("mime","application/octet-stream")}\r\n\r\n")
                            file.inputStream().use{it.copyTo(out)};line("\r\n")
                        };line("--$boundary--\r\n")
                    }
                }
            }
            val code=conn.responseCode
            val body=(if(code in 200..299)conn.inputStream else conn.errorStream)?.bufferedReader()?.use{it.readText()}?:"{}"
            val json=try{JSONObject(body)}catch(e:Exception){throw ApiException(code,"Invalid server response")}
            if(code !in 200..299 || json.optString("status")!="success")throw ApiException(code,json.optString("message","Server request failed"))
            return json
        }finally{conn.disconnect()}
    }
    fun read(endpoint: String,params: JSONObject=JSONObject(),callback: (JSONObject)->Unit) {
        val owner=RecentLoginStore.getUserId(context);val key=owner+":"+endpoint+":"+params.toString()
        val ephemeral=endpoint in listOf("calls_incoming","call_get","presence_get")
        EspressoIdlingResource.increment()
        io.execute {
            db.expireStories()
            val cached=(if(ephemeral)null else db.snapshot(key))?:JSONObject().put("status","success")
            if (endpoint == "profile_get" && !cached.has("user")) {
                val uid = params.optString("user_id", params.optString("uid", owner))
                db.user(uid)?.let { cached.put("user", it) }
            }
            val projectedCached=project(endpoint,params,cached)
            main.post{try{if(RecentLoginStore.getUserId(context)==owner)callback(projectedCached)}finally{EspressoIdlingResource.decrement()}}
            if(!inFlight.add(key))return@execute
            network.execute {
                try {
                    val fresh=request(endpoint,params,get=true)
                    if(RecentLoginStore.getUserId(context)==owner) {
                        if(!ephemeral)db.cache(key,fresh);db.expireStories();val projected=project(endpoint,params,fresh)
                        main.post{if(RecentLoginStore.getUserId(context)==owner)callback(projected)}
                    }
                }catch(_:Exception){}finally{inFlight.remove(key)}
            }
        }
    }
    fun mutate(endpoint: String,params: JSONObject,uris: List<Uri> = emptyList(),callback: (Boolean)->Unit = {}) {
        val owner=RecentLoginStore.getUserId(context)
        if(owner.isEmpty()){callback(false);return}
        EspressoIdlingResource.increment()
        io.execute {
            val files=JSONArray();val uuid=UUID.randomUUID().toString()
            try {
                for((i,uri) in uris.withIndex()) {
                    val dir=File(context.filesDir,"outbox").apply{mkdirs()};val file=File(dir,"${uuid}_$i")
                    context.contentResolver.openInputStream(uri)?.use {input -> file.outputStream().use{output ->
                        val buffer=ByteArray(16384);var total=0L
                        while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=25L*1024*1024){"Maximum 25 MB per attachment"};output.write(buffer,0,n)}
                    }}?:error("Cannot open attachment")
                    files.put(JSONObject().put("field",if(endpoint=="posts_create")"files[$i]" else "file").put("path",file.absolutePath).put("mime",context.contentResolver.getType(uri)?:"application/octet-stream"))
                }
                params.put("action_uuid",uuid).put("_local_time",System.currentTimeMillis())
                if(endpoint=="message_send")params.put("client_msg_id",uuid)
                db.enqueue(uuid,owner,endpoint,params,files)
                main.post{try{callback(true);changed();schedule()}finally{EspressoIdlingResource.decrement()}}
            }catch(e:Exception){main.post{android.widget.Toast.makeText(context,e.message,android.widget.Toast.LENGTH_LONG).show();callback(false);EspressoIdlingResource.decrement()}}
        }
    }
    private fun project(endpoint: String,params: JSONObject,source: JSONObject): JSONObject = PendingProjection.apply(endpoint,params,source)
}
