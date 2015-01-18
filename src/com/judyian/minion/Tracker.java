package com.judyian.minion;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

// Ref. http://developer.android.com/guide/topics/location/strategies.html#BestEstimate
public class Tracker {
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	private static final Object locationLock = new Object();

	private Context context;
	private double lastLatitude = 0.0;
	private double lastLongitude = 0.0;
	private LocationManager lm;
	private Location lastLocation;
	private FileWriter fileWriter;

	private final LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			synchronized(locationLock) {
				System.out.println("***************************ouchhhhhhhhhhhhhhh");
				if (lastLocation == null || isBetterLocation(location, lastLocation)) {
					lastLongitude = location.getLongitude();
					lastLatitude = location.getLatitude();
					lastLocation = location;
	
					try {
						fileWriter.write(System.currentTimeMillis() + ","
								+ lastLatitude + "," + lastLongitude + ";");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
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

	public Tracker(Context context, FileWriter fileWriter) {
		this.context = context;
		this.fileWriter = fileWriter;
		this.lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	public void startLocationTracking() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		// TODO Measure power requirements to see if we need to change this.
		criteria.setPowerRequirement(Criteria.POWER_LOW);

		//=============
		List<String> providers = lm.getProviders(true);

        Location l = null;
        for (int i = 0; i < providers.size(); i++) {
        	System.out.println("providers" + providers.get(i));
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null)
                break;
        }
        if (l != null) {
            System.out.println("***************last location: " + l.toString());

        }
        //================
        
		// TODO May need to change timeout to balance power with accuracy.
		// 2000 ms and 10 meters
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, locationListener);
		System.out.println("Started location tracking.");
	}

	public void sendCurrentLocationText() {
		String msg = "lat " + lastLatitude + ", lng " + lastLongitude;
		PhoneHome.sendSMSToParents(msg);
	}

	public double getLastLatitude() {
		return lastLatitude;
	}

	public double getLastLongitude() {
		return lastLongitude;
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param newLocation
	 *            The new Location that you want to evaluate
	 * @param currentLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	private boolean isBetterLocation(Location newLocation,
			Location currentLocation) {
		if (currentLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = newLocation.getTime() - currentLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (newLocation.getAccuracy() - currentLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
				currentLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
}
