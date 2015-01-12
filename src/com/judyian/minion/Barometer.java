package com.judyian.minion;

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
		sensorManager = (SensorManager) context
				.getSystemService(Service.SENSOR_SERVICE);
	}

	public void startRecordingAltitude() {
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
