package za.dams.paracrm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.DownloadManager;
import android.content.Context;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class SdcardManager {

	public static boolean saveData(Context context, String filename, byte[] data ){
	    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	    File file = new File(path, filename);
	    String filePath = file.getAbsolutePath();
	    try {                      
	        FileOutputStream stream = new FileOutputStream(file, true);             
	        stream.write(data);            
	        stream.close();
	    } catch (IOException e) {
	        return false ;
	    }
		DownloadManager dmm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE) ;
		dmm.addCompletedDownload(filename, filename, false, getMimeType(filename), filePath, data.length, true);
	    return true ;
	}
	public static boolean saveData(Context context, String filename, byte[] data, boolean doToast ){
		boolean success = saveData( context, filename, data ) ;
		if( success ) {
			saveToast(context, filename) ;
		} else {
			saveToast(context, null) ;
		}
		return success ;
	}
	
	public static String getMimeType(String filename)
	{
	    String type = null;
	    String extension = MimeTypeMap.getFileExtensionFromUrl(filename);
	    if (extension != null) {
	        MimeTypeMap mime = MimeTypeMap.getSingleton();
	        type = mime.getMimeTypeFromExtension(extension);
	    }
	    return type;
	}	
	public static void saveToast(Context context, String filename) {
		if( filename != null ) {
			Toast.makeText(context, "Saved as : " + filename,Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(context, "Failed to save !",Toast.LENGTH_LONG).show();
		}
	}
}
