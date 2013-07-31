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
import com.syntacticsinc.navigationlib.R;

public class NavigationActivity extends FragmentActivity {
	Navigator navigator;
	private UpdateOverlayReceiver updateOverlayReceiver;
	private WakeLock wakelock;
	public static String RECEIVER_UPDATE_OVERLAY = "com.syntacticsinc.navigationlib.UPDATE_OVERLAYS";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigation);
		
		navigator = new Navigator(this,getMap());
		navigator.setDestinationLatLng(new LatLng(8.499368,124.625638));
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
		wakelock.release();
		super.onPause();
	}
	@Override
	protected void onResume() {
		
		if(wakelock==null){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "NavigationActivity");
		}
		
		wakelock.acquire();
		super.onResume();
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
