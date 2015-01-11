package com.judyian.minion;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class Tracking {
	private double latitude = 0.0;
	private double longitude = 0.0;
	private LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

	private final LocationListener locationListener = new LocationListener() {

		public void onLocationChanged(Location location) {
			longitude = location.getLongitude();
			latitude = location.getLatitude();

			// TODO notify us that location changed
		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub

		}
	};

	public Tracking() {
		startLocationTracking();
	}

	private void startLocationTracking() {
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10,
				locationListener);
	}
}
