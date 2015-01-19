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
import android.os.PowerManager;
import android.os.StrictMode;
import android.os.PowerManager.WakeLock;
import android.os.StatFs;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
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
	        if (megabytesAvailable() < 5) {
	            System.out.println("Ran out of space.");
	            return;
	        }
			takePicture();
			timerHandler.postDelayed(this, 5000);
		}
	};

	// Runs every 5 min.
	private Handler timerHandlerLocation5min = new Handler();
	private Runnable timerRunnableLocation5min = new Runnable() {
		@Override
		public void run() {
			if (isNetworkAvailable()) {
				tracker.sendCurrentLocationText();
			}
			timerHandlerLocation5min.postDelayed(this, 1000 * 60 * 5);
		}
	};
	
	// Runs every 5 min or 5 seconds.
	private Handler timerHandlerPicture5min = new Handler();
	private Runnable timerRunnablePicture5min = new Runnable() {
		@Override
		public void run() {
			if (isNetworkAvailable() && networkClassSupportsData()) {
				uploadBestPicture();
				timerHandlerPicture5min.postDelayed(this, 1000 * 5);
			} else {
				timerHandlerPicture5min.postDelayed(this, 1000 * 60 * 5);
			}
		}
	};

	private FileWriter locationFileWriter;
	private FileWriter altitudeFileWriter;
	private FileWriter photoInfoFileWriter;
	private FileWriter accelerometerFileWriter;

	private Tracker tracker;
	private Barometer barometer;
	private Accelerometer accel;
	private Uploader uploader;

	private PhotoHeap photoHeap;
	private Camera camera;
	private SurfaceView surface;
	
	private WakeLock wakeLock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		wakeLock.acquire();
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 

		try {
			locationFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory()
							+ "/location.txt", true /* append */);
			altitudeFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory()
							+ "/altitude.txt", true /* append */);
			photoInfoFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory()
							+ "/photoInfo.txt", true /* append */);
			accelerometerFileWriter = new FileWriter(
					Environment.getExternalStorageDirectory()
					+ "/accel.txt", true /* append */);
		} catch (IOException e) {
			System.out.println("Cannot get location or altitude file");
			e.printStackTrace();
		}
		tracker = new Tracker(getBaseContext(), locationFileWriter);
		tracker.startLocationTracking();

		barometer = new Barometer(getBaseContext(), altitudeFileWriter);
		barometer.startRecordingAltitude();
		
		accel = new Accelerometer(getBaseContext(), accelerometerFileWriter);
		accel.startRecordingAccel();

		uploader = new Uploader();
		photoHeap = new PhotoHeap();

		timerHandler.postDelayed(timerRunnable, 5000);
		timerHandlerLocation5min.postDelayed(timerRunnableLocation5min, 5000);
		timerHandlerPicture5min.postDelayed(timerRunnablePicture5min, 5000);

		surface = (SurfaceView) findViewById(R.id.surfaceView);

		PhoneHome.sendSMSToParents("Initialized minion.");
		System.out.println("Initialized minion.");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		// TODO: how to keep the app always running.
		try {
			locationFileWriter.flush();
			altitudeFileWriter.flush();
			photoInfoFileWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		wakeLock.release();
		try {
			locationFileWriter.close();
			altitudeFileWriter.close();
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
		try {
			camera = Camera.open();
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

				int lat = (int)(fullLat * 10000);
				int lng = (int)(fullLng * 10000);
				String timeStamp = new SimpleDateFormat("dd_HHmmss")
						.format(new Date());
				
				String fullPath = Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
						+ "/JPEG_" + timeStamp + "_" + lat + "_" + lng+ ".jpg";
				System.out.println("Saving pic to " + fullPath);
				outStream = new FileOutputStream(fullPath);
				outStream.write(data);
				outStream.close();

				photoInfoFileWriter.write(timeStamp + "," + altitude + ","
						+ fullLat + "," + fullLng + ";");
				photoHeap.push(System.currentTimeMillis(), altitude, fullLat, fullLng,
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
		return networkClass == "3G" || networkClass == "4G";
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
