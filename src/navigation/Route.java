package navigation;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.syntacticsinc.navigationlib.R;

public class Route {
	private LatLng from;
	private LatLng to;
	private String toAddress;
	private List<LatLng> path;
	private Context context;
	
	public interface OnPointsFetchedListener{
		void onPointsFetched(List<LatLng> path);
	}

	
	
	public Route(Context c,LatLng f,LatLng t){
		context = c.getApplicationContext();
		setFrom(f);
		setTo(t);
		path = new ArrayList<LatLng>();
	}
	
	public Route(Context c, LatLng f, String destinationAddress) {
		context = c.getApplicationContext();
		setFrom(f);
		setToAddress(destinationAddress);
		path = new ArrayList<LatLng>();
	}

	public List<LatLng> getPath(){
		return path;
	}
	public LatLng getFrom() {
		return from;
	}
	public void setFrom(LatLng from) {
		this.from = from;
	}
	public LatLng getTo() {
		return to;
	}
	public void setTo(LatLng to) {
		this.to = to;
	}
	
	public void generateRoute(){
		if(path==null) path = new ArrayList<LatLng>();
		if(path.size()==0){
			new FetchPoints().execute();
			Log.i("","fetching path");
			Toast.makeText(context, "Loading route", Toast.LENGTH_LONG).show();
		}else{
			Log.i("","path already resolved path");
			Intent intent = new Intent();
			intent.setAction(NavigationActivity.RECEIVER_UPDATE_OVERLAY);
			context.sendBroadcast(intent);
		}
	}
	private List<LatLng> decodePoly(String encoded) {

		List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
 
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
 
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
 
            LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
            poly.add(p);
        }
 
        return poly;

	}
	
	private void parseJSONPoints(String content){
		path = new ArrayList<LatLng>();
        try {
			JSONObject jsonContent = null;
			JSONObject route = null;
			JSONArray legs = null;
			if(content!=null){
				jsonContent = new JSONObject(content);
				JSONArray routes = jsonContent.getJSONArray("routes");
				route = routes.getJSONObject(0);
			}
			
			if(route!=null){
				legs = route.getJSONArray("legs");
			}
			
			if(legs!=null){
				for(int i=0;i<legs.length();i++){
					JSONObject leg = legs.getJSONObject(i);
					JSONArray steps = leg.getJSONArray("steps");
					if(steps!=null){
						for(int j=0;j<steps.length();j++){
							JSONObject step = steps.getJSONObject(j);
							JSONObject polyline = null;
							String encryptedPoints = null;
							if(step!=null) polyline = step.getJSONObject("polyline");
							if(polyline!=null) encryptedPoints = polyline.getString("points");
							if(encryptedPoints!=null) path.addAll(decodePoly(encryptedPoints));
						}
					}
				}
			}
			Intent intent = new Intent();
			intent.setAction(NavigationActivity.RECEIVER_UPDATE_OVERLAY);
			context.sendBroadcast(intent);
		} catch (Exception e) {
			Toast.makeText(context, "Error loading route", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	
	 public String getToAddress() {
		return toAddress;
	}

	public void setToAddress(String toAddress) {
		this.toAddress = toAddress;
	}


	private class FetchPoints extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			String url = "http://maps.googleapis.com/maps/api/directions/json?";
			url+="origin="+getFrom().latitude+","+getFrom().longitude;
			if(Route.this.getTo()!=null)
				url+="&destination="+getTo().latitude+","+getTo().longitude;
			else if(Route.this.getToAddress()!=null)
				url+="&destination="+URLEncoder.encode(Route.this.getToAddress());
			url+="&sensor=false";
			
			Log.i("","URL: "+url);
			
			HttpClient http = new DefaultHttpClient();
	        try {
				HttpResponse response = http.execute(new HttpGet(url));
				HttpEntity entity = response.getEntity();
				if (entity != null) {  
			        // do something with the response
			        String content = EntityUtils.toString(entity);
			        Log.i("Route",content);
			        parseJSONPoints(content);
			    }
				
				if(path!=null){
					FetchPoints.this.getClass();
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	 }

	

}
