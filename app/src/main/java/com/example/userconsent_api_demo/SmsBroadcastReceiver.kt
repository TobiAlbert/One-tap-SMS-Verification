package com.example.userconsent_api_demo

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SmsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "SmsBroadcastReceiver"
        const val REQUEST_SMS_CONSENT = 1001
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
            val extras = intent.extras
            val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status

            when (smsRetrieverStatus.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)

                    try {
                        // Start Activity to show content dialog to user. Activity must be started
                        // within 5 minutes, otherwise you'll receive another TIMEOUT intent
                        context?.let { (it as AppCompatActivity).startActivityForResult(consentIntent!!, REQUEST_SMS_CONSENT) }

                    } catch (e: ActivityNotFoundException) {
                        Log.e(TAG, "No Activity Found: $e")
                    }
                }

                CommonStatusCodes.TIMEOUT -> {
                    // handle timeout
                }
             }
        }
    }
}