package com.judyian.minion;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

// Ref. http://developer.android.com/guide/topics/location/strategies.html#BestEstimate
public class Tracking {
	private Context context;
	private double latitude = 0.0;
	private double longitude = 0.0;
	private LocationManager lm;

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

	public Tracking(Context context) {
		this.context = context;
		startLocationTracking();
	}

	private void startLocationTracking() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setPowerRequirement(Criteria.POWER_LOW); // TODO measure power
															// requirements
		String provider = lm.getBestProvider(criteria, true);
		System.out.println("Chose provider for location tracking: " + provider);
		lm = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(provider, 2000, 10, locationListener);
	}
}
