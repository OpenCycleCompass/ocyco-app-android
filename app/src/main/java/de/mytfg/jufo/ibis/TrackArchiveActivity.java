package de.mytfg.jufo.ibis;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import de.mytfg.jufo.ibis.util.Utils;

public class TrackArchiveActivity extends AppCompatActivity {
    private static final String TAG = "TrackArchiveAct..-class";

    String[] tracks = new String[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_archive);
        ListView listView = (ListView) findViewById(R.id.trackListView);

        ArrayList<String> arrayList = new ArrayList<>();

        SharedPreferences sharedPrefs = getSharedPreferences(
                getString(R.string.preference_file_key_upload_later), Context.MODE_PRIVATE);

        if (sharedPrefs.contains("trackNameList")) {
            String trackNameList = sharedPrefs.getString("trackNameList", "");
            Log.i(TAG, "trackNameList=" + trackNameList);

            tracks = trackNameList.split(";");
            for (String track : tracks) {
                Log.i(TAG, "track=" + track);
                String listText = track;
                if(sharedPrefs.contains(track)) {
                    try {
                        // Get timestamp from first track point and format as date
                        JSONArray trackData = new JSONArray(sharedPrefs.getString(track, ""));
                        listText = Utils.getDateTime((long) trackData.getJSONObject(0).getDouble("tst"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.e(TAG, "track '" + track + "' not found in sharedPrefs.");
                }
                arrayList.add(track + " | " + listText);
            }
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, arrayList);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(),
                        "Upload Track " + tracks[position], Toast.LENGTH_LONG)
                        .show();

                Intent intent = new Intent(getApplicationContext(), UploadTrackActivity.class);
                intent.putExtra("track", tracks[position]);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    public void onClickTrackArchiveDelete(View view) {
        SharedPreferences sharedPrefs = getSharedPreferences(
                getString(R.string.preference_file_key_upload_later), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.clear();
        sharedPrefsEditor.apply();
    }
}
