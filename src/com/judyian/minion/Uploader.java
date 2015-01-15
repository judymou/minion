package com.judyian.minion;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;

import java.io.File;

public class Uploader {

	// Work only for Dedicated IP
	static final String FTP_HOST = "12.34.56.78";
	static final String FTP_USER = "XXXXXX";
	static final String FTP_PASS = "XXXXXXX";

	public Uploader() {
	}

	public boolean uploadFile(File fileName) {
		// TODO verify that this definitely returns false if image fails to
		// upload. Is there a timeout?
		FTPClient client = new FTPClient();
		try {
			client.connect(FTP_HOST, 21);
			client.login(FTP_USER, FTP_PASS);
			client.setType(FTPClient.TYPE_BINARY);
			client.changeDirectory("/minionupload/");
			client.upload(fileName, new MyTransferListener());
		} catch (Exception e) {
			e.printStackTrace();
			try {
				client.disconnect(true);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			return false;
		}
		return true;
	}

	// Used to file upload and show progress.
	public class MyTransferListener implements FTPDataTransferListener {
		public void started() {
			System.out.println(" Upload Started ...");
		}

		public void transferred(int length) {
			// Yet other length bytes has been transferred since the last time
			// this method was called.
			System.out.println(" transferred ..." + length);
		}

		public void completed() {
			// Transfer completed.
			System.out.println(" completed ...");
		}

		public void aborted() {
			System.out.println(" aborted ...");
		}

		public void failed() {
			// Transfer failed.
			System.out.println(" failed ...");
		}

	}
}
