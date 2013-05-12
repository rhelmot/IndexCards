package com.andrewdutcher.indexcards;


import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	public CardDrawer mview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mview = (CardDrawer) findViewById(R.id.drawer);
        mview.input = new CardInput(this, (EditText) findViewById(R.id.editText1), getResources().getDisplayMetrics().density);
        /*if (savedInstanceState != null && savedInstanceState.containsKey("mview")) {
        	mview.restore(savedInstanceState.getBundle("mview"));
        }*/	//do it through onRestoreInstanceState instead
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
    
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putBundle("mview", mview.serialize());
    }
    
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	if (savedInstanceState != null && savedInstanceState.containsKey("mview")) {
        	mview.saved = savedInstanceState.getBundle("mview");
        }
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	mview.restore();
    }
}
