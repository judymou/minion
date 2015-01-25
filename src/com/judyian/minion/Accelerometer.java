package com.judyian.minion;

import java.io.FileWriter;
import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Accelerometer implements SensorEventListener {
	private SensorManager sensorManager;
	private FileWriter fileWriter;

	private float lastX = -1;
	private float lastY = -1;
	private float lastZ = -1;
	private long prevRecordTime = System.currentTimeMillis();

	public Accelerometer(Context context, FileWriter fileWriter) {
		sensorManager = (SensorManager) context
				.getSystemService(Service.SENSOR_SERVICE);
		this.fileWriter = fileWriter;
	}

	public void startRecordingAccel() {
		Sensor sensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (sensor == null) {
			System.out.println("No accelerometer sensor on this device.");
			return;
		}

		boolean success = sensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_UI);
		if (success) {
			System.out.println("Started recording accelerometer.");
		} else {
			System.out
					.println("Could not start accelerometer - accelerometer sensor not registered.");
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.values.length < 3) {
			System.out.println("Invalid accelerometer data.");
			return;
		}
		long currentTime = System.currentTimeMillis();
		// At most once per 10 seconds.
		if (currentTime - prevRecordTime < 10000) {
			return;
		}
		lastX = event.values[0];
		lastY = event.values[1];
		lastZ = event.values[2];
		prevRecordTime = currentTime;
		try {
			fileWriter.write(currentTime + ","
					+  lastX + "," + lastY + "," + lastZ + ";");
			fileWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
