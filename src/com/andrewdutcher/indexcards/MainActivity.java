package com.andrewdutcher.indexcards;


import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	
	private CardDrawer mview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mview = new CardDrawer(this);
        setContentView(mview);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
                //TODO: Add settings activity
                return true;
            case R.id.menu_addnew:
            	mview.addnew();
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
