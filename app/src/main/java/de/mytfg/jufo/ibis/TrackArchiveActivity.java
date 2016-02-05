package de.mytfg.jufo.ibis;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import de.mytfg.jufo.ibis.util.Utils;

/**
 * TrackArchiveActivity to list archived tracks.
 * Click on a listed track to upload it
 * or click on the delete all button to delete any archived track
 */
public class TrackArchiveActivity extends AppCompatActivity {
    private String[] tracks = new String[0];
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_archive);
        ListView listView = (ListView) findViewById(R.id.trackListView);
        sharedPrefs = getSharedPreferences(
                getString(R.string.preference_file_key_upload_later), Context.MODE_PRIVATE);
        adapter = new ArrayAdapter<>(
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
        updateUI();
    }

    /**
     * Update ListView of archived tracks. Track list is read from shared preferences.
      */
    private void updateUI() {
        arrayList.clear();
        if (sharedPrefs.contains("trackNameList")) {
            String trackNameList = sharedPrefs.getString("trackNameList", "");
            tracks = trackNameList.split(";");
            for (String track : tracks) {
                String listText = track;
                if(sharedPrefs.contains(track)) {
                    try {
                        // Get timestamp from first track point and format as date
                        JSONArray trackData = new JSONArray(sharedPrefs.getString(track, ""));
                        listText = Utils.getDateTime(trackData.getJSONObject(0).getLong("tst_ms"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Toast.makeText(this, R.string.track_archive_corrupt, Toast.LENGTH_LONG).show();
                }
                arrayList.add(listText);
            }
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * onClick method for TrackArchiveDelete button in activity_track_archive.xml
     * @param view view
     */
    public void onClickTrackArchiveDelete(View view) {
        SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.clear();
        sharedPrefsEditor.apply();
        updateUI();
    }
}
