package com.royal.tenure.age.gold.girlfriend

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BandWidthService : JobService() {
    override fun onStopJob(p0: JobParameters?): Boolean {
        jobFinished(p0, false)
        return false
    }

    override fun onStartJob(p0: JobParameters?): Boolean {
        jobFinished(p0, false)
        return false
    }

}