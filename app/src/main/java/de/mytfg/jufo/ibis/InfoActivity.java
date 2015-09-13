package de.mytfg.jufo.ibis;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class InfoActivity extends AppCompatActivity {

    TextView license_links;
    TextView contact_links;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        license_links = (TextView) findViewById(R.id.license_links);
        license_links.setMovementMethod(LinkMovementMethod.getInstance());
        contact_links = (TextView) findViewById(R.id.contact_links);
        contact_links.setMovementMethod(LinkMovementMethod.getInstance());

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_MainActivity:
                Intent intent_main = new Intent(this, MainActivity.class);
                startActivity(intent_main);
                return true;
            case R.id.action_show_data_activity:
                Intent intent_showData = new Intent(this, ShowDataActivity.class);
                startActivity(intent_showData);
                return true;
            case R.id.action_RoutingActivity:
                Intent intent_routing = new Intent(this, RoutingActivity.class);
                startActivity(intent_routing);
                return true;
            case R.id.action_settings:
                Intent intent_settings = new Intent(this, SettingsActivity.class);
                startActivity(intent_settings);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
