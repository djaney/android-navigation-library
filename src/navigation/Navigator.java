package navigation;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class Navigator implements SensorEventListener{
	private GoogleMap map;
	private LatLng destinationLatLng;
	private String destinationAddress;
	private Route route;
	private Context context;
	private float sensorBearing;
	List<Float> bearingMovingAverage = new ArrayList<Float>();
	private static int movingAverageStep = 10;
	private boolean cameraOnSite = false;
	
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	
	float[] gravity = null;
	float[] geomagnetic = null;
	
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
		
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}
	
	public void registerSensors(){
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	}
	
	public void deregisterSensors(){
		sensorManager.unregisterListener(this);
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
		printCurrentPath();
		updateCamera();
		cameraOnSite = true;

	}
	
	public void updateCamera(){
		Location myLocation = map.getMyLocation();
		if(myLocation!=null){
			// Construct a CameraPosition focusing on Mountain View and animate the camera to that position.
			CameraPosition cameraPosition = new CameraPosition.Builder()
			    .target(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()))
			    .zoom(getZoom())
			    .tilt(getTilt())
			    .bearing(myLocation.hasBearing()?myLocation.getBearing():sensorBearing)
			    .build();
			if(cameraOnSite){
				map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
			}else{
				map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
			}
		}
	}
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			gravity = event.values;
		    
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			geomagnetic = event.values;
		
		if (gravity != null && geomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				if(putBearingSmoother( (float) (orientation[0] * (180/Math.PI)))){
					if(cameraOnSite) updateCamera();
				}
			}
			
		}
	}
	
	public boolean putBearingSmoother(float bearing){
		bearingMovingAverage.add(bearing);
		if(bearingMovingAverage.size()>movingAverageStep){
			float acc = 0;
			for(float i : bearingMovingAverage){
				acc += i;
			}
			this.sensorBearing = acc / bearingMovingAverage.size();
			bearingMovingAverage.remove(0);
			return true;
		}
		
		return false;
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
	
	private void printCurrentPath() {
		Location myLocation = map.getMyLocation();
		LatLng myLatLng = new LatLng(myLocation.getLatitude(),myLocation.getLongitude());
		List<LatLng> path = route.getPath();
		List<LatLng> soFar = new ArrayList<LatLng>();
		double heading = -500;
		
		PolylineOptions option = new PolylineOptions()
		.width(5)
		.color(0xff0000ff)
		.visible(true)
		.geodesic(true);
		
		
		int nearestDistanceIndex = getNearestIndex(path);
		for(int i = path.size()-1;i>=0;i--){
			if(heading==-500){
				heading = acuteBearing(myLatLng,path.get(i));
			}
			
			
			if(Math.abs(acuteBearing(myLatLng,path.get(i))-heading)<=120 && i>=nearestDistanceIndex){
				soFar.add(path.get(i));
			}else{
				break;
			}
			heading = acuteBearing(myLatLng,path.get(i));
			
		}
		
		
		//soFar.add(myLatLng);
		option.addAll(soFar);
		map.addPolyline(option);
		
	}
	private void printPath() {
		List<LatLng> path = route.getPath();
		
		PolylineOptions option = new PolylineOptions()
		.width(10)
		.color(0x550000ff)
		.visible(true)
		.geodesic(true);
		
	
		option.addAll(path);
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
