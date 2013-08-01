package navigation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.codertalks.navigationlib.R;

public class NavigationActivity extends FragmentActivity {
	Navigator navigator;
	private UpdateOverlayReceiver updateOverlayReceiver;
	private WakeLock wakelock;
	public static final String RECEIVER_UPDATE_OVERLAY = "com.codertalks.navigationlib.UPDATE_OVERLAYS";
	
	public static final String EXTRA_DESTINATION_LATITUDE = "com.codertalks.navigationlib.EXTRA_LATITUDE";
	public static final String EXTRA_DESTINATION_LONGITUDE = "com.codertalks.navigationlib.EXTRA_LONGITUDE";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigation);
		
		navigator = new Navigator(this,getMap());
		
		
		Intent intent = getIntent();
		double latitude = intent.getDoubleExtra(EXTRA_DESTINATION_LATITUDE, 0);
		double longitude = intent.getDoubleExtra(EXTRA_DESTINATION_LONGITUDE, 0);
		if(latitude!=0 || longitude!=0)
			navigator.setDestinationLatLng(new LatLng(latitude,longitude));
		
		navigator.moveToDestination();
		
		updateOverlayReceiver = new UpdateOverlayReceiver();
		registerReceiver(updateOverlayReceiver, new IntentFilter(RECEIVER_UPDATE_OVERLAY));
	}

	
	
	@Override
	protected void onDestroy() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		unregisterReceiver(updateOverlayReceiver);
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(wakelock!=null) wakelock.release();
		if(navigator!=null) navigator.deregisterSensors();
	}
	@Override
	protected void onResume() {
		super.onResume();
		if(wakelock==null){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "NavigationActivity");
		}
		
		wakelock.acquire();
		
		if(navigator!=null) navigator.registerSensors();
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.navigation, menu);
		return true;
	}
	
	private GoogleMap getMap(){
		return ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
	}
	
	public class UpdateOverlayReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context arg0, Intent intent) {
			if(navigator!=null)	navigator.generateOverlays();
		}
		
	};

}
