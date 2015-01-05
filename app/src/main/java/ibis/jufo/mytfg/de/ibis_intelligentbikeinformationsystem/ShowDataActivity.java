package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class ShowDataActivity extends ActionBarActivity {

    boolean CollectData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_data);

        //receiving intent
        Intent incomingIntent = getIntent();
        CollectData = incomingIntent.getBooleanExtra("Key", false);

        // Start tracking Service
        Intent intent = new Intent(this, Tracking.class);
        intent.putExtra("Key", CollectData);
        startService(intent);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}