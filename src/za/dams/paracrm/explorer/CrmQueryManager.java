package za.dams.paracrm.explorer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.R;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.http.AndroidHttpClient;
import android.util.Log;

public class CrmQueryManager {
	private static final String LOG_TAG = "CrmQueryManager" ;
	
	
	public static List<CrmQueryModel> model_getAll( Context context ) {
		DatabaseManager mDb = DatabaseManager.getInstance(context) ;
		
		ArrayList<CrmQueryModel> crmQueryModels = new ArrayList<CrmQueryModel>() ;
		
		Cursor c = mDb.rawQuery("SELECT querysrc_id FROM input_query ORDER BY querysrc_index") ;
		while( c.moveToNext() ) {
			int querysrcId = c.getInt(0) ;
			CrmQueryModel cqm = model_get( context,querysrcId ) ;
			if( cqm != null ) {
				crmQueryModels.add(cqm) ;
			}
		}
		c.close() ;
		
		return crmQueryModels ;
	}
	public static CrmQueryModel model_get( Context context, int querysrcId ) {
		DatabaseManager mDb = DatabaseManager.getInstance(context) ;
		Cursor c ;
		c = mDb.rawQuery(String.format("SELECT querysrc_id, querysrc_index, querysrc_type, querysrc_name FROM input_query WHERE querysrc_id='%s'",querysrcId));
		if( c.getCount() != 1 ){
			c.close() ;
			return null ;
		}
		c.moveToNext() ;
		CrmQueryModel crmQueryModel = new CrmQueryModel() ;
		crmQueryModel.querysrcId = c.getInt(0) ;
		crmQueryModel.querysrcIndex = c.getInt(1) ;
		String strType = c.getString(2) ;
		if( strType.equals("query") ) {
			crmQueryModel.querysrcType = CrmQueryModel.QueryType.QUERY ;
		} else if( strType.equals("qmerge") ) {
			crmQueryModel.querysrcType = CrmQueryModel.QueryType.QMERGE ;
		} else if( strType.equals("qweb") ) {
			crmQueryModel.querysrcType = CrmQueryModel.QueryType.QWEB ;
		}
		crmQueryModel.querysrcName = c.getString(3) ;
		c.close() ;
		
		c = mDb.rawQuery(String.format("SELECT querysrc_targetfield_ssid, field_type, field_linkbible, field_lib FROM input_query_where WHERE querysrc_id='%s' ORDER BY querysrc_targetfield_ssid",querysrcId));
		while( c.moveToNext() ) {
			CrmQueryModel.CrmQueryCondition cqc = new CrmQueryModel.CrmQueryCondition() ;
			cqc.querysrcTargetFieldSsid = c.getInt(0) ;
			String fieldType = c.getString(1) ;
			if( fieldType.equals("date") ) {
				cqc.fieldType = CrmQueryModel.FieldType.FIELD_DATE ;
			} else if( fieldType.equals("link") ) {
				cqc.fieldType = CrmQueryModel.FieldType.FIELD_BIBLE ;
			} else {
				continue ;
			}
			cqc.fieldLinkBible = c.getString(2) ;
			cqc.fieldName = c.getString(3);
			cqc.conditionIsSet=false ;
			
			crmQueryModel.querysrcConditions.add(cqc) ;
		}
		c.close() ;
		return crmQueryModel ;
	}
	
	
	public static boolean validateModel( CrmQueryModel cqm ) {
		for( CrmQueryModel.CrmQueryCondition cqc : cqm.querysrcConditions ) {
			if( !cqc.conditionIsSet ) {
				return false ;
			}
		}
		return true ;
	}
	public static Integer fetchRemoteJson( Context c , CrmQueryModel cqm ) {
		JSONArray jsonArrayWhere = new JSONArray() ;
		try {
			for( CrmQueryModel.CrmQueryCondition cqc : cqm.querysrcConditions ) {
				if( !cqc.conditionIsSet ) {
					return null ;
				}
				JSONObject jsonCondition = new JSONObject() ;
				jsonCondition.put( "querysrc_targetfield_ssid" , cqc.querysrcTargetFieldSsid ) ;
				switch( cqc.fieldType ) {
				case FIELD_DATE :
					jsonCondition.put( "condition_date_gt" , cqc.conditionDateGt.format("%Y-%m-%d") ) ;
					jsonCondition.put( "condition_date_lt" , cqc.conditionDateLt.format("%Y-%m-%d") ) ;
					break ;
				case FIELD_BIBLE :
					jsonCondition.put("condition_bible_entries",cqc.conditionBibleEntry.entryKey) ;
					break ;
				default :
					return null ;
				}
				jsonArrayWhere.put(jsonCondition) ;
			}
		} catch( JSONException e ) {
			
		}
		
		String jsonString = null ;
		
        // AndroidHttpClient is not allowed to be used from the main thread
        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpParams httpParameters = client.getParams() ;
    	HttpConnectionParams.setConnectionTimeout(httpParameters, 60000);
    	HttpConnectionParams.setSoTimeout(httpParameters, 60000);
        final HttpPost postRequest = new HttpPost(c.getString(R.string.server_url));

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("_domain", "paramount"));
            nameValuePairs.add(new BasicNameValuePair("_moduleName", "paracrm"));
            nameValuePairs.add(new BasicNameValuePair("_action", "android_query_fetchResult"));
            nameValuePairs.add(new BasicNameValuePair("querysrc_id", String.valueOf(cqm.querysrcId)));
            nameValuePairs.add(new BasicNameValuePair("querysrc_where", jsonArrayWhere.toString()));
			postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        	
            HttpResponse response = client.execute(postRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w(LOG_TAG, "Error " + statusCode +
                        " while retrieving query");
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    BufferedInputStream bis = new BufferedInputStream(inputStream);
                    InputStreamReader isr = new InputStreamReader(bis);
                    StringBuilder strBuffer = new StringBuilder();
    				int bufferSize = 1024;
    				char[] buffer = new char[bufferSize];
    				int len = 0;
    				try {
    				    while (true) {
    				    	len = isr.read(buffer, 0, bufferSize);
    				        if (len == -1) {
    				           break;
    				        }

    				        strBuffer.append(buffer, 0, len);
    				     }
    				} catch (IOException e) {
    				}
    				
    				jsonString = strBuffer.toString() ;
    				
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (IOException e) {
        	postRequest.abort();
            Log.w(LOG_TAG, "I/O error while retrieving query", e);
        } catch (IllegalStateException e) {
        	postRequest.abort();
            Log.w(LOG_TAG, "Incorrect URL: ");
        } catch (Exception e) {
        	postRequest.abort();
            Log.w(LOG_TAG, "Error while retrieving query", e);
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
            }
        }
        
        
        if( jsonString == null ) {
        	return null ;
        }
        try {
			JSONObject jsonObject = new JSONObject(jsonString) ;
			if( jsonObject.optBoolean("success") != true ) {
				return null ;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null ;
		}
        
        DatabaseManager mDb = DatabaseManager.getInstance(c) ;
        ContentValues cv = new ContentValues() ;
        cv.put("json_blob", jsonString) ;
        int jsonResultId = (int)mDb.insert("query_cache_json", cv);
        
        
		return jsonResultId ;
	}

}
