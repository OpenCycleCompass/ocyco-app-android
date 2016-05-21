package de.opencyclecompass.app.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import de.opencyclecompass.app.android.storage.OcycoTrack;
import de.opencyclecompass.app.android.util.Utils;

/**
 * TrackArchiveActivity to list archived tracks.
 * Click on a listed track to upload it
 * or click on the delete all button to delete any archived track
 */
public class TrackArchiveActivity extends AppCompatActivity {
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_archive);
        adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, arrayList);
        ListView listView = (ListView) findViewById(R.id.trackListView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!OcycoApplication.trackArchive.getTrackMetadataList().get(position).
                        isUploaded()) {
                    Toast.makeText(getApplicationContext(), "Upload Track ...",
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(getApplicationContext(), UploadTrackActivity.class);
                    intent.putExtra("track", OcycoApplication.trackArchive.getTrackMetadataMap().get(
                            OcycoApplication.trackArchive.getTrackUuidList().get(position)
                    ).getUuid().toString());
                    intent.putExtra("fromArchive", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(getApplicationContext(), R.string.upload_track_already_uploaded,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        updateUI();
    }

    /**
     * Update ListView of archived tracks. Track list is read from shared preferences.
      */
    private void updateUI() {
        arrayList.clear();

        for (OcycoTrack.MetaData metadata : OcycoApplication.trackArchive.getTrackMetadataList()) {
            if (metadata != null) {
                arrayList.add(Utils.getDateTime(metadata.getStartTime()));
            }
            else {
                arrayList.add("Fehler :-(");
            }
        }

        adapter.notifyDataSetChanged();
    }

    /**
     * onClick method for TrackArchiveDelete button in activity_track_archive.xml
     * @param view view
     */
    public void onClickTrackArchiveDeleteAll(View view) {
        OcycoApplication.trackArchive.deleteAll();
    }
}
