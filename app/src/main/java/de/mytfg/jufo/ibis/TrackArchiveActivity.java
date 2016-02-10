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

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import de.mytfg.jufo.ibis.storage.TrackDatabaseMemory;
import de.mytfg.jufo.ibis.util.Utils;

/**
 * TrackArchiveActivity to list archived tracks.
 * Click on a listed track to upload it
 * or click on the delete all button to delete any archived track
 */
public class TrackArchiveActivity extends AppCompatActivity {
    private long[] track_names = new long[0];
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private SharedPreferences sharedPrefs;
    private ArrayList<TrackDatabaseMemory> tracks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_archive);
        ListView listView = (ListView) findViewById(R.id.trackListView);
        tracks = new ArrayList<>();
        sharedPrefs = getSharedPreferences(
                getString(R.string.preference_file_key_upload_later), Context.MODE_PRIVATE);
        adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(),
                        "Upload Track " + track_names[position], Toast.LENGTH_LONG)
                        .show();
                Intent intent = new Intent(getApplicationContext(), UploadTrackActivity.class);
                intent.putExtra("track", track_names[position]);
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
            // read list of archived tracks from shard preferences
            String trackNameList = sharedPrefs.getString("trackNameList", "");
            String[] track_names_string = trackNameList.split(";");
            // allocate/clear lists for track ids and track database objects
            track_names = new long[track_names_string.length];
            tracks.clear();
            tracks.ensureCapacity(track_names_string.length);
            for (int i = 0; i < track_names_string.length; i++) {
                track_names[i] = Long.parseLong(track_names_string[i]);
                ObjectInputStream input;
                String filename = track_names_string[i] + ".track";
                try {
                    input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(),"")+ File.separator+filename)));
                    TrackDatabaseMemory track = (TrackDatabaseMemory) input.readObject();
                    input.close();
                    tracks.add(track);
                    arrayList.add(Utils.getDateTime(track.getStartTime())
                            + " (" + getString(R.string.upload_track_duration) + ": "
                            + Utils.formatTime(track.getDuration()) + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.track_archive_corrupt, Toast.LENGTH_LONG).show();
                }
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
