package com.royal.tenure.age.gold.girlfriend

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class GetBitmap : AsyncTask<String, Void, MutableList<Bitmap>>(){
    override fun doInBackground(vararg bitmapUris: String?): MutableList<Bitmap> {
        var inputStream: InputStream? = null
        var bitmaps : MutableList<Bitmap> = mutableListOf()

        for (bitmapUri in bitmapUris){

            try {
                val url : URL = URL(bitmapUri)
                val urlConnection : HttpURLConnection = url.openConnection() as HttpURLConnection
                inputStream = BufferedInputStream(urlConnection.inputStream)


                val bitmapOptions = BitmapFactory.Options()
                bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
                val bitmap = BitmapFactory.decodeStream(inputStream, null, bitmapOptions)
                bitmaps.add(bitmap)

            } catch (e: IOException){
                return mutableListOf()
            } finally {
                inputStream?.close()
            }
        }

        return bitmaps
    }

    override fun onPostExecute(result: MutableList<Bitmap>) {
        super.onPostExecute(result)
    }

}