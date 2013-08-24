package za.dams.paracrm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.provider.Settings;

public class HttpPostHelper {
	
	public static String getPostString( HashMap<String,String> postParams ) {
		if(postParams.size() == 0)
	        return "";

	    StringBuffer buf = new StringBuffer();
	    Iterator<String> keys = postParams.keySet().iterator() ;
	    while(keys.hasNext()) {
	        buf.append(buf.length() == 0 ? "" : "&");
	        String key = keys.next() ;
	        try {
				buf.append(key).append("=").append(URLEncoder.encode(postParams.get(key),"UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
			}
	    }
	    return buf.toString();
	}
	
	public static String readStream(InputStream in) throws IOException {  
	    StringBuilder sb = new StringBuilder();  
	    BufferedReader r = new BufferedReader(new InputStreamReader(in),1000);  
	    for (String line = r.readLine(); line != null; line =r.readLine()){  
	        sb.append(line);  
	    }  
	    in.close();  
	    return sb.toString();  
	}   	
	
	
	public static final int TIMEOUT_DL = 1 ;
	public static final int TIMEOUT_PULL = 2 ;
	
	public static HttpClient getHttpClient( Context context, int timeoutMode ) {
		
		final HttpClient client = AndroidHttpClient.newInstance("Android");
		
		int applyTimeout ;
		switch( timeoutMode ) {
		case TIMEOUT_DL :
			applyTimeout = MainPreferences.getInstance(context).getServerDlTimeout() ;
			break ;
		case TIMEOUT_PULL :
			applyTimeout = MainPreferences.getInstance(context).getServerPullTimeout() ;
			break ;
		default :
			applyTimeout = 0 ;
			break ;
		}
		if( applyTimeout > 0 ) {
			int msTimeout = applyTimeout * 1000 ;
	        client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, msTimeout);
	        client.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, msTimeout);
		}
		
		return client ;
	}
	
	public static HttpPost getHttpPostRequest( Context context, HashMap<String,String> postParams ) {
		
        final String serverUrl = MainPreferences.getInstance(context).getServerFullUrl() ;
        final HttpPost postRequest = new HttpPost(serverUrl);
        
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        
        String android_id = Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ANDROID_ID);
        nameValuePairs.add(new BasicNameValuePair("__ANDROID_ID", android_id));
        
        if( postParams != null ) {
        	for( Map.Entry<String,String> map : postParams.entrySet() ) {
        		nameValuePairs.add(new BasicNameValuePair(map.getKey(), map.getValue()));
        	}
        }
        
		try {
			postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e) {}
		
        return postRequest ;
	}
}
