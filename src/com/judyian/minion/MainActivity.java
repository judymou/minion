package com.judyian.minion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.view.Menu;
import android.view.SurfaceView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private Handler timerHandler = new Handler();
	private Runnable timerRunnable = new Runnable() {
		@Override
		public void run() {
			takePicture();
			timerHandler.postDelayed(this, 5000);
		}
	};

	private Tracker tracker;
	private Barometer barometer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tracker = new Tracker(getBaseContext());
		tracker.startLocationTracking();

		barometer = new Barometer(getBaseContext());
		barometer.startRecordingAltitude();

		timerHandler.postDelayed(timerRunnable, 0);

		System.out.println("Initialized minion.");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	SurfaceView surface;
	Camera camera;

	private void takePicture() {
		Toast.makeText(getApplicationContext(), "Image snapshot Started",
				Toast.LENGTH_SHORT).show();
		// here below "this" is activity context.
		surface = new SurfaceView(this);
		camera = Camera.open();
		try {
			camera.setPreviewDisplay(surface.getHolder());
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			try {
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
						.format(new Date());
				String imageFileName = "JPEG_" + timeStamp + "_";
				File storageDir = Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

				String fullPath = storageDir + "/" + imageFileName + ".jpg";
				System.out.println("Saving pic to " + fullPath);
				outStream = new FileOutputStream(fullPath);
				outStream.write(data);
				outStream.close();
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

}
