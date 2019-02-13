package com.royal.tenure.age.gold.girlfriend

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class GetBitmap : AsyncTask<String, Void, Bitmap>(){
    override fun doInBackground(vararg bitmapUri: String?): Bitmap? {
        var inputStream: InputStream? = null

        try {
            val url : URL = URL(bitmapUri[0])
            val urlConnection : HttpURLConnection = url.openConnection() as HttpURLConnection
            inputStream = BufferedInputStream(urlConnection.getInputStream())


            val bitmapOptions = BitmapFactory.Options()
            bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmap = BitmapFactory.decodeStream(inputStream, null, bitmapOptions)

            return bitmap

        } catch (e: IOException){
            return null
        } finally {
            inputStream!!.close()
        }
    }

    override fun onPostExecute(result: Bitmap?) {
        super.onPostExecute(result)
    }
}