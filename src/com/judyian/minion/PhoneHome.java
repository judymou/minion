package com.judyian.minion;

import android.telephony.SmsManager;

public class PhoneHome {
	public static void sendSMSToParents(String message) {
		sendSMS("9147727429", message);
		sendSMS("4086279246", message);
	}

	public static void sendSMS(String phoneNumber, String message) {
		SmsManager sms = SmsManager.getDefault();
		// TODO option to queue messages, resend them if not sent
		sms.sendTextMessage(phoneNumber, null, message, null, null);
	}
}
