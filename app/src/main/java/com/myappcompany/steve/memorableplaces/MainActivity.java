package com.myappcompany.steve.memorableplaces;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static ArrayList<String> places = new ArrayList<String>();
    static ArrayList<LatLng> locations = new ArrayList<LatLng>();
    static ArrayAdapter locationArrayAdapter;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = this.getSharedPreferences("com.myappcompany.steve.memorableplacesanswer", Context.MODE_PRIVATE);
        ListView locationListView = findViewById(R.id.locationListView);

        if(locations.size() == 0) {
            locations.add(new LatLng(0, 0));
        }

        if(places.size() == 0) {
            places.add("Add a new place...");
        }

        locationArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, places);

        locationListView.setAdapter(locationArrayAdapter);

        locationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(MainActivity.this, places.get(position), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                intent.putExtra("placeNumber", position);

                startActivity(intent);
            }
        });
    }

    public void onClickSave(View view) {
        //LatLng objects are currently not serializable so we have to convert to something that is :(
        ArrayList<Double> lats = new ArrayList<>();
        ArrayList<Double> longs = new ArrayList<>();

        for(int i = 0; i < locations.size(); i++) {
            lats.add(locations.get(i).latitude);
            longs.add(locations.get(i).longitude);
        }


        try {
            String serializedPlaces = ObjectSerializer.serialize(places);
            String serializedLats = ObjectSerializer.serialize(lats);
            String serializedLongs = ObjectSerializer.serialize(longs);
            sharedPreferences.edit().putString("places", serializedPlaces).apply();
            sharedPreferences.edit().putString("lats", serializedLats).apply();
            sharedPreferences.edit().putString("longs", serializedLongs).apply();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("onClickLoad Error", "type: " + e.toString() + " message: " + e.getMessage());
            //Toast.makeText(getApplicationContext(), "There was an error saving!", Toast.LENGTH_SHORT).show();
        }

    }

    public void onClickLoad(View view) {

        try {
            ArrayList<String> loadedPlaces = (ArrayList<String>) ObjectSerializer
                    .deserialize(sharedPreferences
                            .getString("places"
                                    //You need to serialize an ArrayList<String> as a default
                                    //Otherwise if you don't find the friends ArrayList serialized
                                    //String you at least get something that won't break the
                                    //ObjectSerializer.deserialize
                                    , ObjectSerializer.serialize(new ArrayList<String>())));
            ArrayList<Double> lats = (ArrayList<Double>) ObjectSerializer
                    .deserialize(sharedPreferences
                            .getString("lats"
                                    //You need to serialize an ArrayList<Double> as a default
                                    //Otherwise if you don't find the friends ArrayList serialized
                                    //String you at least get something that won't break the
                                    //ObjectSerializer.deserialize
                                    , ObjectSerializer.serialize(new ArrayList<Double>())));
            ArrayList<Double> longs = (ArrayList<Double>) ObjectSerializer
                    .deserialize(sharedPreferences
                            .getString("longs"
                                    //You need to serialize an ArrayList<Double> as a default
                                    //Otherwise if you don't find the friends ArrayList serialized
                                    //String you at least get something that won't break the
                                    //ObjectSerializer.deserialize
                                    , ObjectSerializer.serialize(new ArrayList<Double>())));

            //Build an ArrayList of LatLngs from the stored Lat and Long Double data.
            ArrayList<LatLng> loadedLatLngs = new ArrayList<LatLng>();
            for(int i = 0; i < lats.size(); i++) {
                loadedLatLngs.add(new LatLng(lats.get(i), longs.get(i)));
            }

            //clear the existing list
            for(int i = 1; i < places.size(); i++) {
                places.remove(1);
                locations.remove(1);
            }

            //add in the loaded values
            for(int i = 1; i < loadedPlaces.size(); i++) {
                places.add(loadedPlaces.get(i));
                locations.add(loadedLatLngs.get(i));
            }

            locationArrayAdapter.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("onClickLoad Error", "type: " + e.toString() + " message: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "There was an error loading your locations!", Toast.LENGTH_SHORT).show();
        }

    }

    public void onClickClear(View view) {

        for(int i = 1; i < places.size(); i++) {
            places.remove(1);
            locations.remove(1);
        }

        locationArrayAdapter.notifyDataSetChanged();
    }
}
