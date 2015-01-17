package com.judyian.minion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.SurfaceView;
import android.widget.Toast;

public class MainActivity extends Activity {
	// Runs every 5 seconds.
	private Handler timerHandler = new Handler();
	private Runnable timerRunnable = new Runnable() {
		@Override
		public void run() {
			takePicture();
			timerHandler.postDelayed(this, 5000);
		}
	};

	// Runs every 5 min.
	private Handler timerHandler5min = new Handler();
	private Runnable timerRunnable5min = new Runnable() {
		@Override
		public void run() {
			if (isNetworkAvailable()) {
				tracker.sendCurrentLocationText();
				if (networkClassSupportsData()) {
					uploadBestPicture();
				}
			}
			timerHandler5min.postDelayed(this, 1000 * 60 * 5);
		}
	};

	private FileWriter locationFileWriter;
	private FileWriter altitudeFileWriter;
	private FileWriter photoInfoFileWriter;

	private Tracker tracker;
	private Barometer barometer;
	private Uploader uploader;

	private PhotoHeap photoHeap;
	private Camera camera;
	private SurfaceView surface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		try {
			locationFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory()
							+ "/Text/location.txt", true /* append */);
			altitudeFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory()
							+ "/Text/altitude.txt", true /* append */);
			photoInfoFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory()
							+ "/Text/photoInfo.txt", true /* append */);
		} catch (IOException e) {
			System.out.println("Cannot get location or altitude file");
			e.printStackTrace();
		}
		tracker = new Tracker(getBaseContext(), locationFileWriter);
		tracker.startLocationTracking();

		barometer = new Barometer(getBaseContext(), altitudeFileWriter);
		barometer.startRecordingAltitude();

		uploader = new Uploader();
		photoHeap = new PhotoHeap();

		timerHandler.postDelayed(timerRunnable, 0);
		timerHandler5min.postDelayed(timerRunnable5min, 5000);

		System.out.println("Initialized minion.");
	}

	@Override
	protected void onStop() {
		// TODO: how to keep the app always running.
		try {
			locationFileWriter.flush();
			locationFileWriter.close();
			altitudeFileWriter.flush();
			altitudeFileWriter.close();
			photoInfoFileWriter.flush();
			photoInfoFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private void takePicture() {
		Toast.makeText(getApplicationContext(), "Image snapshot Started",
				Toast.LENGTH_SHORT).show();
		// Here below "this" is activity context.
		// TODO: do we need to reconstruct surface and camera every time?
		surface = new SurfaceView(this);
		camera = Camera.open();
		try {
			camera.setPreviewDisplay(surface.getHolder());
		} catch (IOException e) {
			e.printStackTrace();
		}
		camera.startPreview();
		camera.takePicture(shutterCallback, rawCallback, jpegCallback);
	}

	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		@SuppressLint("SimpleDateFormat")
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			try {
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
						.format(new Date());
				// TODO encode lat/lng/altitude in the filename so we will know
				// the detail
				// when we update the best photo.
				String fullPath = Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
						+ "/JPEG_" + timeStamp + ".jpg";
				System.out.println("Saving pic to " + fullPath);
				outStream = new FileOutputStream(fullPath);
				outStream.write(data);
				outStream.close();

				double altitude = barometer.getEstimatedAltitudeInFeet();
				photoInfoFileWriter.write(timeStamp + "," + altitude + ","
						+ tracker.getLastLatitude() + ","
						+ tracker.getLastLongitude() + ";");
				photoHeap.push(System.currentTimeMillis(), altitude,
						tracker.getLastLatitude(), tracker.getLastLongitude(),
						fullPath);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				camera.stopPreview();
				camera.release();
				camera = null;
				Toast.makeText(getApplicationContext(), "Image snapshot done",
						Toast.LENGTH_LONG).show();
				System.out.println("Snapshot saved");
			}
		}
	};

	private void uploadBestPicture() {
		PhotoRecord fr = photoHeap.pop();
		File f = new File(fr.imagePath);
		if (uploader.uploadFile(f)) {
			f.delete();
		} else {
			photoHeap.push(fr);
		}
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnected();
	}

	private String getNetworkClass() {
		TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		int networkType = mTelephonyManager.getNetworkType();
		switch (networkType) {
		case TelephonyManager.NETWORK_TYPE_GPRS:
		case TelephonyManager.NETWORK_TYPE_EDGE:
		case TelephonyManager.NETWORK_TYPE_CDMA:
		case TelephonyManager.NETWORK_TYPE_1xRTT:
		case TelephonyManager.NETWORK_TYPE_IDEN:
			return "2G";
		case TelephonyManager.NETWORK_TYPE_UMTS:
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
		case TelephonyManager.NETWORK_TYPE_HSDPA:
		case TelephonyManager.NETWORK_TYPE_HSUPA:
		case TelephonyManager.NETWORK_TYPE_HSPA:
		case TelephonyManager.NETWORK_TYPE_EVDO_B:
		case TelephonyManager.NETWORK_TYPE_EHRPD:
		case TelephonyManager.NETWORK_TYPE_HSPAP:
			return "3G";
		case TelephonyManager.NETWORK_TYPE_LTE:
			return "4G";
		default:
			return "Unknown";
		}
	}

	private boolean networkClassSupportsData() {
		String networkClass = getNetworkClass();
		return networkClass == "3G" || networkClass == "4G";
	}

	/*
	public static float megabytesAvailable(File f) {
		StatFs stat = new StatFs(f.getPath());
		long bytesAvailable = (long) stat.getBlockSizeLong()
				* (long) stat.getAvailableBlocksLong();
		return bytesAvailable / (1024.f * 1024.f);
	}
	*/
}
