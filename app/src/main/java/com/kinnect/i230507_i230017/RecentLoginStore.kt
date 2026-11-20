package com.kinnect.i230507_i230017
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object RecentLoginStore {
    private fun prefs(c: Context)=c.getSharedPreferences("kinnect_session",Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey("kinnect_token",null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("kinnect_token",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun save(context: Context,name: String,email: String,token: String="",userId: String="",expiresAt: Long=0L,profileComplete: Boolean=true) {
        val edit=prefs(context).edit().putString("name",name).putString("email",email)
        if(token.isNotEmpty()) {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
            edit.putString("token",Base64.encodeToString(cipher.doFinal(token.toByteArray()),Base64.NO_WRAP))
                .putString("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP))
                .putString("user_id",userId).putLong("expires_at",expiresAt).putBoolean("profile_complete",profileComplete)
        };edit.apply()
    }
    fun getToken(context: Context): String? = try {
        val p=prefs(context);val text=p.getString("token",null)
        if(text==null || p.getLong("expires_at",0)<=System.currentTimeMillis())null else {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(p.getString("iv",null),Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(text,Base64.NO_WRAP)))
        }
    }catch(e:Exception){null}
    fun getName(c: Context)=prefs(c).getString("name",null)
    fun getEmail(c: Context)=prefs(c).getString("email",null)
    fun getUserId(c: Context)=prefs(c).getString("user_id","")?:""
    fun profileComplete(c: Context)=prefs(c).getBoolean("profile_complete",false)
    fun completeProfile(c: Context){prefs(c).edit().putBoolean("profile_complete",true).apply()}
    fun clear(c: Context){prefs(c).edit().remove("token").remove("iv").remove("user_id").remove("expires_at").remove("profile_complete").apply()}
}
