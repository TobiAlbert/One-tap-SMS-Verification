/*
 * MIT License
 *
 * Copyright (c) 2020 Tobi Daada.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.example.userconsent_api_demo

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.userconsent_api_demo.SmsBroadcastReceiver.Companion.REQUEST_SMS_CONSENT
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    companion object {
        const val CREDENTIAL_PICKER_REQUEST = 1003
        const val TAG = "MainActivity"
    }

    private val smsBroadcastReceiver: SmsBroadcastReceiver by lazy { SmsBroadcastReceiver() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // attempt to retrieve the user's phone number
        requestHint()
        btn_send_phone.setOnClickListener { sendPhoneNumberToRemoteServer() }

        // start listening for incoming messages
        // if expected phone number is known, you can pass it, else pass `null`.
        val task = SmsRetriever.getClient(this).startSmsUserConsent(null)
        task.addOnCompleteListener {
            task?.exception?.let {
                // handle possible exception
                Log.e(TAG, "Task Exception: $it")
                return@addOnCompleteListener
            }

            if (task.isSuccessful) {
                val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
                registerReceiver(smsBroadcastReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null)
            }
        }
    }

    private fun requestHint() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()
        val credentialsClient = Credentials.getClient(this)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)
        try {
            startIntentSenderForResult(
                intent.intentSender,
                CREDENTIAL_PICKER_REQUEST,
                null, 0, 0, 0
            )
        } catch (e: IntentSender.SendIntentException) {
            Log.e("MainActivity", "Could not start intent: $e")
        }
    }

    private fun sendPhoneNumberToRemoteServer() {
        // simulate sending phone to remote server
        val phoneNumber = et_phone.text.toString()
        Toast.makeText(this, "Sending Phone to Server... $phoneNumber", Toast.LENGTH_LONG)
            .show()
    }

    private fun parseMessageForOtp(message: String?) {
        message?.let {
            val pattern = Pattern.compile("\\d{4}")
            val matcher = pattern.matcher(message)

            if (matcher.find()) {
                val otp: String? = matcher.group()
                pin_otp.value = otp
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CREDENTIAL_PICKER_REQUEST -> {
                // Obtain the phone number from the result
                if (resultCode == Activity.RESULT_OK) {
                    val credential = data?.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                    et_phone.setText(credential?.id)
                } else {
                    // Result Code of 1002 shows that there are no hints available
                    Log.w("MainActivity", "Result Code not successful. Result Code: $resultCode")
                }
            }

            REQUEST_SMS_CONSENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.let {
                        val message = it.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                        parseMessageForOtp(message)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsBroadcastReceiver)
    }
}
