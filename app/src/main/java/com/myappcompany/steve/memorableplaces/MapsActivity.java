package com.myappcompany.steve.memorableplaces;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    LocationManager locationManager;
    LocationListener locationListener;
    private final int minUpdateTime = 0;
    private final int minUpdateDist = 5;
    Marker currentLocationMarker;

    private GoogleMap mMap;

    /**
     * Helper method to center the map on a given location, add a marker there, and give it a label
     * @param location the Location location to place the marker
     * @param title the String label for the marker
     */
    public void centerMapOnLocation(Location location, String title) {
        if(location != null) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            if(currentLocationMarker != null){
                currentLocationMarker.remove();
            }
            currentLocationMarker = mMap.addMarker(new MarkerOptions().position(userLocation).title(title));
            MainActivity.locations.set(0, currentLocationMarker.getPosition());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13));
        }
    }

    //Handle the location permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Check (1) there is a permission (2) that the first element is Permission Granted int enum value
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Check that they have the location permission
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, minUpdateTime, minUpdateDist, locationListener);
                //Get last known location
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                centerMapOnLocation(lastKnownLocation, "Your location");
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        mMap.setOnMapLongClickListener(this);

        Intent intent = getIntent();
        int locationArrayPosition = intent.getIntExtra("placeNumber",0);
        //Toast.makeText(this, String.valueOf(intent.getIntExtra("placeNumber",0)),Toast.LENGTH_SHORT).show();

        //if "Add a new place..." was pressed.
        if(locationArrayPosition == 0) {
            //Zoom in on (current) user location
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    centerMapOnLocation(location, "Your Location");
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };

            //Check to see if we have access already, else ask for it.
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, minUpdateTime, minUpdateDist, locationListener);
                //Get last known location
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                centerMapOnLocation(lastKnownLocation, "Your Location");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }

        } else {
            //User clicked on a saved location
            LatLng latLng = MainActivity.locations.get(locationArrayPosition);
            String address = MainActivity.places.get(locationArrayPosition);
            mMap.addMarker( new MarkerOptions().position(latLng).title(address).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Marker addedMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(getAddress(latLng)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        MainActivity.locations.add(latLng);
        MainActivity.places.add(addedMarker.getTitle());
        MainActivity.locationArrayAdapter.notifyDataSetChanged();
    }

    public String getAddress(LatLng latLng) {
        String address;
        try {
            address = new GetLocationTextTask().execute(latLng).get();
            return address;
        } catch (ExecutionException e) {
            e.printStackTrace();
            Log.i("Error in getAddress", e.toString() + " " + e.getMessage());
            return getTimeString();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.i("Error in getAddress", e.toString() + " " + e.getMessage());
            return getTimeString();
        }
    }

    public class GetLocationTextTask extends AsyncTask<LatLng, Void, String> {

        @Override
        protected String doInBackground(LatLng... latLngs) {
            LatLng latLng = latLngs[0];
            String address = "";

            //address += "Latitude: " + latLng.latitude + "\n\n";
            //address += "Longitude: " + latLng.longitude + "\n\n";

            //address += "Accuracy: " + location.getAccuracy() + "\n\n";
            //address += "Altitude: " + location.getAltitude() + "\n\n";
            //address += "Address:\n";

            try {
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addressList != null && addressList.size() > 0) {
                    Log.i("Address", addressList.get(0).toString());

                    if (addressList.get(0).getThoroughfare() != null) {
                        address += addressList.get(0).getThoroughfare() + ", ";
                    }
                    if (addressList.get(0).getLocality() != null) {
                        address += addressList.get(0).getLocality() + ", ";
                    }
                    if (addressList.get(0).getAdminArea() != null) {
                        address += addressList.get(0).getAdminArea() + ", ";
                    }
                    if (addressList.get(0).getPostalCode() != null) {
                        address += addressList.get(0).getPostalCode();
                    }

                } else {
                    Log.i("Address Error", "Could not obtain an address.");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    address = sdf.format(timestamp);
                }
                return address;
            } catch (IOException e) {
                Log.i("GetAddressTask Error:", "Error type: " + e.toString() + "\nError Message: " + e.getMessage());
                return getTimeString();
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("GetAddressTask Error:", "Error type: " + e.toString() + "\nError Message: " + e.getMessage());
                return getTimeString();
            }
        }
    }

    public String getTimeString() {
        String timeString;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        timeString = sdf.format(timestamp);
        return timeString;
    }
}
