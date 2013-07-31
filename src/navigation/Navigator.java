package navigation;

import java.util.ArrayList;
import java.util.List;

import navigation.Route.OnPointsFetchedListener;
import android.content.Context;
import android.content.IntentSender.SendIntentException;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class Navigator {
	private GoogleMap map;
	private LatLng destinationLatLng;
	private String destinationAddress;
	private Route route;
	private Context context;
	private float bearing;
	
	private float zoom = 17;
	private float tilt = 0;
	public Navigator(Context c,GoogleMap m){
		context = c.getApplicationContext();
		setMap(m);
		map.setMyLocationEnabled(true);
		map.moveCamera(CameraUpdateFactory.zoomTo(zoom));
		map.setOnMyLocationChangeListener(new OnMyLocationChangeListener(){

			@Override
			public void onMyLocationChange(Location loc) {
				generateRoute(loc);
			}
		});
		map.setOnCameraChangeListener(new OnCameraChangeListener(){

			@Override
			public void onCameraChange(CameraPosition camPos) {
				setZoom(camPos.zoom);
				
			}});
	}
	
	public void moveToDestination(){
		map.moveCamera(CameraUpdateFactory.newLatLng(getDestinationLatLng()));
	}
	
	public Route getRoute(){return route;}
	
	public GoogleMap getMap() {
		return map;
	}
	public void setMap(GoogleMap map) {
		this.map = map;
	}
	public LatLng getDestinationLatLng() {
		return destinationLatLng;
	}
	public void setDestinationLatLng(LatLng destinationLocation) {
		this.destinationLatLng = destinationLocation;
	}
	
	public String getDestinationAddress() {
		return destinationAddress;
	}

	public void setDestinationAddress(String destinationAddress) {
		this.destinationAddress = destinationAddress;
	}

	public float getZoom() {
		return zoom;
	}

	public void setZoom(float zoom) {
		this.zoom = zoom;
	}

	public float getTilt() {
		return tilt;
	}

	public void setTilt(float tilt) {
		this.tilt = tilt;
	}

	public void generateOverlays(){
		map.clear();
		
		printPath();
		
		Location myLocation = map.getMyLocation();
		if(myLocation!=null){
			Log.i("Navigation","bearing: "+bearing);
			// Construct a CameraPosition focusing on Mountain View and animate the camera to that position.
			CameraPosition cameraPosition = new CameraPosition.Builder()
			    .target(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()))
			    .zoom(getZoom())
			    .bearing(bearing)
			    .tilt(getTilt())
			    .build();
			map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
		}
	}
	private int getNearestIndex(List<LatLng> path){
		int index = -1;
		float distance = -1;
		int i=0;
		Location myLocation = map.getMyLocation();
		if(myLocation!=null){
			for(LatLng p:path){
				float distanceTmp = getDistance(new LatLng(myLocation.getLatitude(),myLocation.getLongitude()),p);
				if(index<0 || distanceTmp<=distance){
					index = i;
					distance = distanceTmp;
				}
				i++;
			}
		}
		return index;
	}
	private float getDistance(LatLng from,LatLng to){
		final float[] results= new float[3];
		Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results);
		final float distance = results[0];
		return distance;
	}
	private void generateRoute(Location myLocation){
		
		LatLng you = null;
		
		if(myLocation!=null)
			you = new LatLng(myLocation.getLatitude(), myLocation.getLongitude()-0.001);
		else Log.e("Navigator","myLocation null");
		
		if(you!=null && getDestinationLatLng()!=null && route==null)
			if(getDestinationLatLng()!=null)
				route = new Route(context,you,getDestinationLatLng());
		if(getDestinationAddress()!=null)
			route = new Route(context,you,getDestinationAddress());
		if(route!=null){
			route.generateRoute();
		}else Log.e("Navigator","route null");
		
	}
	
	private void printPath() {
		Location myLocation = map.getMyLocation();
		LatLng myLatLng = new LatLng(myLocation.getLatitude(),myLocation.getLongitude());
		List<LatLng> path = route.getPath();
		List<LatLng> soFar = new ArrayList<LatLng>();
		double heading = -500;
		
		PolylineOptions option = new PolylineOptions()
		.width(5)
		.color(Color.BLUE)
		.visible(true);
		
		
		int nearestDistanceIndex = getNearestIndex(path);
		for(int i = path.size()-1;i>=0;i--){
			if(heading==-500){
				heading = acuteBearing(myLatLng,path.get(i));
			}
			
			
			if(Math.abs(acuteBearing(myLatLng,path.get(i))-heading)<=120 && i>=nearestDistanceIndex){
				soFar.add(path.get(i));
			}else{
				Log.i("Navigation","Heading too great: "+Math.abs(acuteBearing(myLatLng,path.get(i))-heading));
				break;
			}
			heading = acuteBearing(myLatLng,path.get(i));
			
		}
		if(soFar.size()>0)
			bearing = calculateBearing(myLatLng, soFar.get(soFar.size()-1));
		
		soFar.add(myLatLng);
		option.addAll(soFar);
		map.addPolyline(option);
		
	}
	
	private float acuteBearing(LatLng from,LatLng to){
		float angle = Math.abs(calculateBearing(from,to));
		if(angle>180)
			angle = angle-180;
		return angle;
	}
	
	private float calculateBearing(LatLng from,LatLng to){
		final float[] results= new float[3];
		Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results);
		final float bearing = results[1];
		return bearing;
	}
	
}
