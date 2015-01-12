package com.judyian.minion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Barometer implements SensorEventListener {
	private SensorManager sensorManager;

	// Pressure in mBars
	private float lastPressure = -1;

	public Barometer(Context context) {
		// Get the reference to the sensor manager
		sensorManager = (SensorManager) context
				.getSystemService(Service.SENSOR_SERVICE);

		// Get the list of pressure sensors
		List<Sensor> sensorList = sensorManager
				.getSensorList(Sensor.TYPE_PRESSURE);

		List<Map<String, String>> sensorData = new ArrayList<Map<String, String>>();

		System.out.println("Available pressure sensors:");
		for (Sensor sensor : sensorList) {
			System.out.println("name", sensor.getName());
		}
		System.out.println("Using sensor: "
				+ sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
						.getName());
	}

	public void startRecordingAltitude() {
		// Look for barometer sensor
		Sensor pressureSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_PRESSURE);
		if (pressureSensor == null) {
			System.out.println("No pressure sensor on this device.");
		} else {
			sensorManager.registerListener(this, pressureSensor,
					SensorManager.SENSOR_DELAY_UI);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		lastPressure = event.values[0];
	}

	public double getEstimatedAltitudeInFeet() {
		if (lastPressure == -1) {
			return lastPressure;
		}
		// NOAA http://www.srh.noaa.gov/images/epz/wxcalc/pressureAltitude.pdf
		return (1 - Math.pow(lastPressure / 1013.25, 0.190284)) * 145366.45;
	}
}
