package za.dams.paracrm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;

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
}
