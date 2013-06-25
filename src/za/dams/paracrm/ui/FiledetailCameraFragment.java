/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.dams.paracrm.ui ;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class FiledetailCameraFragment extends FiledetailFragment {
	
	private static final String TAG = "PARACRM/UI/FiledetailCameraFragment";

    ViewGroup mCameraFrame ;
    
    Menu mMenu ;
    Camera mCamera;

    public static FiledetailCameraFragment newInstance(int index) {
    	FiledetailCameraFragment f = new FiledetailCameraFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);

        return f;
    }
    
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    /** A generic method to find optimal size */
	private static Size findOptimalSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }		
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Add an up arrow to the "home" button, indicating that the button will go "up"
        // one activity in the app's Activity heirarchy.
        // Calls to getActionBar() aren't guaranteed to return the ActionBar when called
        // from within the Fragment's onCreate method, because the Window's decor hasn't been
        // initialized yet.  Either call for the ActionBar reference in Activity.onCreate()
        // (after the setContentView(...) call), or in the Fragment's onActivityCreated method.
        Activity activity = this.getActivity();
        ActionBar actionBar = activity.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	mCameraFrame = (ViewGroup)inflater.inflate(R.layout.filecapture_filedetail_camera, container, false) ;
        return mCameraFrame ;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Use mCurrentCamera to select the camera desired to safely restore
        // the fragment after the camera has been changed
        mCamera = getCameraInstance();
        if( mCamera == null ) {
        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        	builder.setTitle("Error")
        		   .setMessage("Cannot open device camera")
        	       .setCancelable(false)
        	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	                getFragmentManager().popBackStack() ;
        	           }
        	       });
        	AlertDialog alert = builder.create();            
        	alert.show();
        	
        	return ;
        }

        Camera.Parameters mCamParams = mCamera.getParameters() ;
        
        //Query pictures sizes
        List<Size> pictureSizes = mCamParams.getSupportedPictureSizes() ;
        Size optimalSize = findOptimalSize(pictureSizes, 1000, 750);
        
        mCamParams.setPictureSize(optimalSize.width, optimalSize.height) ;
        mCamParams.setJpegQuality(90) ;
        mCamParams.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO) ;
        mCamera.setParameters(mCamParams) ;
        
        
        CameraPreview cameraPreview = new CameraPreview(this.getActivity(),mCamera) ;
        mCameraFrame.removeAllViews() ;
        mCameraFrame.addView(cameraPreview) ;
    }

    @Override
    public void onPause() {
        super.onPause();
        
        mCameraFrame.removeAllViews() ;

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	mMenu = menu ;
        inflater.inflate(R.menu.camera_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_shoot:
        	mMenu.findItem(R.id.menu_shoot).setEnabled(false) ;
        	mCamera.takePicture(null, null, jpegCallback) ;
            return true;
            
        case android.R.id.home:
        	/*
            Intent intent = new Intent(this.getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            */
        	getFragmentManager().popBackStack() ;
        	
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    
    PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			try {
				// write to local sandbox file system
				long tstamp = System.currentTimeMillis() ;
				String tmpFileName = String.format("%d",tstamp) ;
				String tmpFileNameThumb = String.format("%d.thumb",tstamp) ;
				
				outStream = getActivity().openFileOutput(tmpFileName, 0);
				// Or write to sdcard
				//outStream = new FileOutputStream(String.format("/sdcard/Pictures/paracrm_%d.jpg", System.currentTimeMillis()));
				outStream.write(data);
				outStream.close();
				
				//Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
				
				BitmapFactory.Options options=new BitmapFactory.Options();
				options.inSampleSize = 4;
				Bitmap preview_bitmap=BitmapFactory.decodeByteArray(data,0,data.length,options);

				outStream = getActivity().openFileOutput(tmpFileNameThumb, 0);
				preview_bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
				outStream.close();
				
				
		    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
		    	CrmFileTransaction mTransaction = mManager.getTransaction() ;
				
		    	mTransaction.page_addRecord_photo(getShownIndex(), 
		    			Uri.fromFile(getActivity().getFileStreamPath (tmpFileName)).toString(),
		    			Uri.fromFile(getActivity().getFileStreamPath (tmpFileNameThumb)).toString() ) ;
				
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			// Log.d(TAG, "onPictureTaken - jpeg");
			
            try {
            	Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
			
            
            
			
            camera.startPreview();
            mMenu.findItem(R.id.menu_shoot).setEnabled(true) ;
		}
	};    
	
	/** A basic Camera preview class
	 * from : http://developer.android.com/guide/topics/media/camera.html#custom-camera
	 * & from : ApiDemos
	 *  */
	private class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {
		private SurfaceView mSurfaceView;
	    private SurfaceHolder mHolder;
	    
	    private Camera mCamera;
	    List<Size> mSupportedPreviewSizes;
	    
	    Size mPreviewSize;

	    public CameraPreview(Context context, Camera camera) {
	        super(context);
	        
	        mCamera = camera;
	        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
	        
	        mSurfaceView = new SurfaceView(context);
	        addView(mSurfaceView);

	        // Install a SurfaceHolder.Callback so we get notified when the
	        // underlying surface is created and destroyed.
	        mHolder = mSurfaceView.getHolder();
	        mHolder.addCallback(this);
	        // deprecated setting, but required on Android versions prior to 3.0
	        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	    }

	    public void surfaceCreated(SurfaceHolder holder) {
	    	//Log.w(TAG,"surfaceCreated ");
	        // The Surface has been created, now tell the camera where to draw the preview.
	        try {
	            mCamera.setPreviewDisplay(holder);
	        } catch (IOException e) {
	            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
	        }
	        mCamera.startPreview() ;
	    }

	    public void surfaceDestroyed(SurfaceHolder holder) {
	        // empty. Take care of releasing the Camera preview in your activity.
    		mCamera.stopPreview() ;
	    }

	    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
	    	/* Note : 
	    	 * La taille de la surface est imposée par onLayout,
	    	 * on ne récupère jamais rien de nouveau ici 
	    	 */
	    	
	    	//Log.w(TAG,"surfaceChanged "+w+" x "+h);
	    	
	    	/*
	    	Camera.Parameters parameters = mCamera.getParameters();
	        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
	        requestLayout();

	        mCamera.setParameters(parameters);
	        mCamera.startPreview();
	        */
	    }

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;
			if (mPreviewSize != null) {
				previewWidth = mPreviewSize.width;
				previewHeight = mPreviewSize.height;
			}

			// Center the child SurfaceView within the parent.
			if (width * previewHeight > height * previewWidth) {
				final int scaledChildWidth = previewWidth * height
						/ previewHeight;
				mSurfaceView.layout((width - scaledChildWidth) / 2, 0,
						(width + scaledChildWidth) / 2, height);
			} else {
				final int scaledChildHeight = previewHeight * width
						/ previewWidth;
				mSurfaceView.layout(0, (height - scaledChildHeight) / 2, width,
						(height + scaledChildHeight) / 2);
			}
		}
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			// We purposely disregard child measurements because act as a
			// wrapper to a SurfaceView that centers the camera preview instead
			// of stretching it.
			final int width = resolveSize(getSuggestedMinimumWidth(),
					widthMeasureSpec);
			final int height = resolveSize(getSuggestedMinimumHeight(),
					heightMeasureSpec);
			setMeasuredDimension(width, height);
			
			//Log.w(TAG,"onMeasure "+width+" x "+height);

			mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);

			//Log.w(TAG,"Setting size is "+mPreviewSize.width+" x "+mPreviewSize.height);
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			mCamera.setParameters(parameters);
		}
		
		private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
	       return findOptimalSize(sizes,w,h);
	    }		
	}
}