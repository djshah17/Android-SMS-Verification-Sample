package com.example.smsverificationsample

import android.app.Activity
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        registerReceiver(smsVerificationBroadcastReceiver, intentFilter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPicker()
    }

    private val PHONE_NUMBER_PICKER_REQUEST = 1

    // show the phone number picker to request phone number for verification
    private fun requestPicker() {
        val phoneNumberRequest = HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build()
        val getCredentialsClient = Credentials.getClient(this)
        val pickerIntent = getCredentialsClient.getHintPickerIntent(phoneNumberRequest)
        startIntentSenderForResult(
                pickerIntent.intentSender,
                PHONE_NUMBER_PICKER_REQUEST,
                null, 0, 0, 0
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PHONE_NUMBER_PICKER_REQUEST ->
                // Get phone number from picker
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val getCredential = data.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
                    startSMSDetectListener()
                }
            SMS_VERIFICATION_REQUEST ->
                // Get the verification code
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Get SMS message content
                    val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                    // Extract verification code
                    val oneTimeCode = extractVerificationCode(message)
                    edt_verification_code.setText(oneTimeCode)
                } else {
                    // Sms user consent denied
                }
        }
    }

    private fun extractVerificationCode(message: String): String {
        return Regex("(\\d{6})").find(message)?.value ?: ""
    }

    private fun startSMSDetectListener() {
        SmsRetriever.getClient(this).also {
            it.startSmsUserConsent(null)
                .addOnSuccessListener {
                    Log.e("SMS Verification", "SMS Detect Started")
                }
                .addOnFailureListener {
                    Log.e("SMS Verification", "SMS Detect Failed")
                }
        }
    }

    private val SMS_VERIFICATION_REQUEST = 2

    private val smsVerificationBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                val extras = intent.extras
                val retrieveSMSStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status

                when (retrieveSMSStatus.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        // Retrieve sms consent intent
                        val smsConsentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                        try {
                            // Display sms consent dialog
                            startActivityForResult(smsConsentIntent, SMS_VERIFICATION_REQUEST)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        // Handle timeout error
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(smsVerificationBroadcastReceiver)
    }

}
