package com.judyian.minion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.StatFs;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.view.SurfaceView;
import android.widget.Toast;
import android.os.Process;

public class MainService extends Service {
	private final int MAX_PICTURE_TIME = 120 * 60 * 1000;
	
	// Runs every 10s.
	private Handler takePictureTimerHandler;
	private Runnable takePictureRunnable = new Runnable() {
		@Override
		public void run() {
			System.out.println("Thread id:" + Thread.currentThread().getName());
			if (battery.getLastBatteryLevel() <= 30) {
				System.out.println("Low battery, stop taking pictures.");
			} else if (System.currentTimeMillis() - Tracker.START_TIME > MAX_PICTURE_TIME) {
				System.out.println("More than 120 min.");
			} else if (megabytesAvailable() < 5) {
				takePictureTimerHandler.postDelayed(this, 1000 * 60 * 5);
				System.out.println("Ran out of space.");
			} else {
				takePicture();
				takePictureTimerHandler.postDelayed(this, 1000 * 10);
			}
		}
	};

	// Runs every 5 min or 10 seconds.
	private Handler uploadPictureTimerHandler;
	private Runnable uploadPictureRunnable = new Runnable() {
		@Override
		public void run() {
			System.out.println("Thread id:" + Thread.currentThread().getName());
			if (battery.getLastBatteryLevel() <= 30) {
				System.out.println("Low battery, stop taking pictures.");
			} else if (isNetworkAvailable() && networkClassSupportsData()) {
				uploadBestPicture();
				uploadPictureTimerHandler.postDelayed(this, 1000 * 10);
			} else {
				System.out.println("Upload picture waiting for 5 min.");
				uploadPictureTimerHandler.postDelayed(this, 1000 * 60 * 5);
			}
		}
	};

	// Runs every 1.5 min.
	private Handler txtLocationTimerHandler;
	private Runnable txtLocationRunnable = new Runnable() {
		@Override
		public void run() {
			System.out.println("Thread id:" + Thread.currentThread().getName());
			if (isNetworkAvailable()) {
				tracker.sendCurrentLocationTextWithBatteryLevel(battery.getLastBatteryLevel());
			}
			txtLocationTimerHandler.postDelayed(this, 1000 * 90);
		}
	};

	// File writers.
	private FileWriter locationFileWriter;
	private FileWriter altitudeFileWriter;
	private FileWriter photoInfoFileWriter;
	private FileWriter accelerometerFileWriter;

	// Sensors.
	private Tracker tracker;
	private Barometer barometer;
	private Accelerometer accel;
	private Battery battery;

	// Photo uploader.
	private Uploader uploader;
	private PhotoHeap photoHeap;

	private Camera camera;
	private SurfaceView surface;

	@Override
	public void onCreate() {
		System.out.println("Main service onCreate");
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// --------------- Create File Writers --------------------------------
		try {
			locationFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory() + "/location.txt",
					true /* append */);
			altitudeFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory() + "/altitude.txt",
					true /* append */);
			photoInfoFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory()
							+ "/photoInfo.txt", true /* append */);
			accelerometerFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory() + "/accel.txt",
					true /* append */);
		} catch (IOException e) {
			System.out.println("Cannot get location or altitude file");
			e.printStackTrace();
		}

		// --------------- Start Sensors --------------------------------------
		tracker = new Tracker(getBaseContext(), locationFileWriter);
		tracker.startLocationTracking();

		barometer = new Barometer(getBaseContext(), altitudeFileWriter);
		barometer.startRecordingAltitude();

		accel = new Accelerometer(getBaseContext(), accelerometerFileWriter);
		accel.startRecordingAccel();
		
		battery = new Battery(getBaseContext());

		// --------------- Start Photo Uploader -------------------------------
		uploader = new Uploader();
		photoHeap = new PhotoHeap();

		// --------------- Start Multiple Threads -----------------------------
		HandlerThread takePicutreThread = new HandlerThread(
				"takePictureThread", Process.THREAD_PRIORITY_BACKGROUND);
		takePicutreThread.start();
		takePictureTimerHandler = new Handler(takePicutreThread.getLooper());
		takePictureTimerHandler.postDelayed(takePictureRunnable, 5000);

		HandlerThread txtLocationThread = new HandlerThread("txtLocationThread");
		txtLocationThread.start();
		txtLocationTimerHandler = new Handler(txtLocationThread.getLooper());
		txtLocationTimerHandler.postDelayed(txtLocationRunnable, 5000);

		HandlerThread uploadPictureThread = new HandlerThread(
				"uploadPictureThread");
		uploadPictureThread.start();
		uploadPictureTimerHandler = new Handler(uploadPictureThread.getLooper());
		uploadPictureTimerHandler.postDelayed(uploadPictureRunnable, 5000);

		Notification notification = new Notification(R.drawable.ic_launcher,
				"Minion", System.currentTimeMillis());
		Intent notificationIntent = new Intent(this,
				MainActivityWithService.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, "Fake Minion", "Minion is on",
				pendingIntent);
		startForeground(1234567, notification);

		// surface = (SurfaceView) findViewById(R.id.surfaceView);
		surface = new SurfaceView(this);
		PhoneHome.sendSMSToParents("Initialized minion service.");
		System.out.println("Initialized minion service.");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		System.out.println("Main service starting.");
		// If we get killed, after returning from here, restart.
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null.
		return null;
	}

	@Override
	public void onDestroy() {
		System.out.println("Main service done.");
		// Close file writers.
		try {
			locationFileWriter.flush();
			altitudeFileWriter.flush();
			photoInfoFileWriter.flush();
			accelerometerFileWriter.flush();
			locationFileWriter.close();
			altitudeFileWriter.close();
			photoInfoFileWriter.close();
			accelerometerFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Clean up threads and handler queues.
		takePictureTimerHandler.removeCallbacksAndMessages(null);
		takePictureTimerHandler.getLooper().quit();
		txtLocationTimerHandler.removeCallbacksAndMessages(null);
		txtLocationTimerHandler.getLooper().quit();
		uploadPictureTimerHandler.removeCallbacksAndMessages(null);
		uploadPictureTimerHandler.getLooper().quit();
	}

	private void takePicture() {
		Toast.makeText(getApplicationContext(), "Image snapshot Started",
				Toast.LENGTH_SHORT).show();
		try {
			camera = Camera.open();

			Camera.Parameters params = camera.getParameters();
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			camera.setParameters(params);

			camera.setPreviewDisplay(surface.getHolder());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		camera.startPreview();
		camera.autoFocus(autoFocusCallback);
	}

	AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		}
	};

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
				double fullLat = tracker.getLastLatitude();
				double fullLng = tracker.getLastLongitude();
				double altitude = barometer.getEstimatedAltitudeInFeet();

				int lat = (int) (fullLat * 10000);
				int lng = (int) (fullLng * 10000);
				String timeStamp = new SimpleDateFormat("dd_HHmmss")
						.format(new Date());

				String fullPath = Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
						+ "/JPEG_" + timeStamp + "_" + lat + "_" + lng + ".jpg";
				System.out.println("Saving pic to " + fullPath);
				outStream = new FileOutputStream(fullPath);
				outStream.write(data);
				outStream.close();

				photoInfoFileWriter.write(timeStamp + "," + altitude + ","
						+ fullLat + "," + fullLng + ";");
				photoInfoFileWriter.flush();
				photoHeap.push(System.currentTimeMillis(), altitude, fullLat,
						fullLng, fullPath);
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
		if (fr == null) {
			return;
		}

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
		// TODO: remove unknown which was added for testing with wifi.
		return networkClass == "3G" || networkClass == "4G"
				|| networkClass == "Unknown";
	}

	@SuppressWarnings("deprecation")
	public static float megabytesAvailable() {
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
				.getPath());
		long bytesAvailable = (long) stat.getBlockSize()
				* (long) stat.getAvailableBlocks();
		return bytesAvailable / (1024.f * 1024.f);
	}
}