package com.kinnect.i230507_i230017

import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

open class VolleyMultipartRequest(
    method: Int,
    url: String,
    private val headers: Map<String, String>?,
    private val mParams: Map<String, String>?,
    private val mByteData: Map<String, DataPart>?,
    private val listener: Response.Listener<NetworkResponse>,
    errorListener: Response.ErrorListener
) : Request<NetworkResponse>(method, url, errorListener) {

    private val boundary = "apiclient-" + System.currentTimeMillis()
    private val mimeType = "multipart/form-data;boundary=$boundary"

    override fun getHeaders(): Map<String, String> {
        return headers ?: super.getHeaders()
    }

    override fun getBodyContentType(): String {
        return mimeType
    }

    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        try {
            mParams?.let {
                for ((key, value) in it) {
                    buildTextPart(dos, key, value)
                }
            }
            mByteData?.let {
                for ((key, value) in it) {
                    buildDataPart(dos, key, value)
                }
            }
            dos.writeBytes("--$boundary--\r\n")
            return bos.toByteArray()
        } catch (e: IOException) {
            VolleyLog.e(e, "IOException writing to ByteArrayOutputStream")
        }
        return ByteArray(0)
    }

    @Throws(IOException::class)
    private fun buildTextPart(dos: DataOutputStream, parameterName: String, parameterValue: String) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"$parameterName\"\r\n\r\n")
        dos.writeBytes("$parameterValue\r\n")
    }

    @Throws(IOException::class)
    private fun buildDataPart(dos: DataOutputStream, parameterName: String, dataFile: DataPart) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"$parameterName\"; filename=\"${dataFile.fileName}\"\r\n")
        dos.writeBytes("Content-Type: ${dataFile.type}\r\n\r\n")
        dos.write(dataFile.content)
        dos.writeBytes("\r\n")
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: NetworkResponse) {
        listener.onResponse(response)
    }

    class DataPart(val fileName: String, val content: ByteArray, val type: String = "image/jpeg")
}
