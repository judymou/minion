package com.judyian.minion;

import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;

// Ref. http://developer.android.com/guide/topics/location/strategies.html#BestEstimate
public class Tracker {
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private Context context;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private LocationManager lm;
    private Location currentBestLocation;
    private FileWriter fileWriter;

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (isBetterLocation(location, currentBestLocation)) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();

                // TODO only send once every n minutes.
                String msg = "lat " + latitude + ", lng " + longitude;
                PhoneHome.sendSMSToParents(msg);

                currentBestLocation = location;
                try {
                    Long timestampSeconds = System.currentTimeMillis() / 1000;
                    fileWriter.write(timestampSeconds.toString() + "," + latitude + "," +
                            longitude + ";");
                } catch (IOException e) {
                    e.printStackTrace();
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
    }

    public void startLocationTracking() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // TODO Measure power requirements to see if we need to change this.
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = lm.getBestProvider(criteria, true);
        System.out.println("Chose provider for location tracking: " + provider);
        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // TODO May need to change timeout to balance power with accuracy.
        // 2000 ms and 10 meters
        lm.requestLocationUpdates(provider, 2000, 10, locationListener);
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
    private boolean isBetterLocation(Location newLocation, Location currentLocation) {
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
        int accuracyDelta = (int) (newLocation.getAccuracy() - currentLocation.getAccuracy());
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
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
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
