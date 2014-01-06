package com.infoenable.flect;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.infoenable.flect.R;
import com.google.android.glass.*;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.media.*;

import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity  implements TextureView.SurfaceTextureListener {
    private GestureDetector mGestureDetector;
    private static Context thisContext;
    private SurfaceHolder mHolder;
	private android.hardware.Camera mCamera;
	private CameraPreview mCameraPreview;
	private TextView message;
	ImageView image;          
	FrameLayout preview;
	private TextureView mTextureView;
	MainActivity ma = this;
	int bufferSize=0;
	Size setSize;
	
    // ...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Keep the screen from dimming/turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGestureDetector = createGestureDetector(this);
        thisContext=this;
        if (false){
        mTextureView = new TextureView(this);//(TextureView) findViewById(R.string.texture_view);
        mTextureView.setSurfaceTextureListener(this);
        setContentView(mTextureView);
        }
        if (true) {
        setContentView(R.layout.activity_main);
        message = (TextView) this.findViewById(R.string.message);
        image = (ImageView) findViewById(R.string.picture_view);
        preview = (FrameLayout) findViewById(R.string.camera_preview);
        }
        
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    HttpFileUpload hu=null;
    Thread t=null;
    ByteArrayInputStream bis;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection. Menu items typically start another
        // activity, start a service, or broadcast another intent.
        switch (item.getItemId()) {
        //case R.id.share_menu_item_text:
        case R.id.share_menu_item:
            	t = new Thread() {
            		@Override
            		public void run() {
            			try{
            				hu.uploadFile("http://www.infoenable.com/ieweb/helthi/protocols/uploadFlect.php","thefile1.jpg");
            			}catch (Exception e) {
            				Log.d("Flect","E - " + e.getMessage());
            			}
            		}
            	};
            	hu = new HttpFileUpload();
            	hu.ba = getJpeg(cameraBuffers[this.currentPicture-1]);
            	Log.d("",String.format("bytes %d", hu.ba.length));
            	t.start();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters){
        Size bestSize = null;
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();

        for(int i = 1; i < sizeList.size(); i++){
         if(sizeList.get(i).width <= width){
        	 return sizeList.get(i);
         }
        }

        return null;
       }
    
    public void stopCamera(){
    	mCamera.stopPreview();
    	mCamera.release();
    	mCamera = null;
    	
    	if (mCameraPreview != null) {
    		mCameraPreview=null;
    	}
    	System.gc();

    }
    
    public boolean isSetup=false;
    public void setup() {     
    	if (mCamera != null){
    		stopCamera();
    	}
        mCamera = Camera.open();
        mCameraPreview = new CameraPreview(this, mCamera);
        preview.removeAllViews();
        preview.addView(mCameraPreview);
        Camera.Parameters parameters=mCamera.getParameters();
        //parameters.setPreviewSize(preview.getWidth(), preview.getHeight());
        
        setSize = getBestPreviewSize(400,400,parameters);
        parameters.setPreviewSize(setSize.width, setSize.height);
        //parameters.setPreviewRange(5, 10);
        image.setVisibility(View.INVISIBLE);
      
        int imageFormat = parameters.getPreviewFormat();
        bufferSize = setSize.width * setSize.height
                * ImageFormat.getBitsPerPixel(imageFormat) / 8;
        FrameCatcher catcher = new FrameCatcher(setSize.width, setSize.height);
        
        //Set the camera parameters now that we've modified
        mCamera.setParameters(parameters);
        
        setupCallback(mCamera, catcher, bufferSize);
        
        isSetup=true;

      }
    
    private final int FramesPerSec = 5;
    private long lastFrameTime = currentTime();
    private class FrameCatcher implements Camera.PreviewCallback {
        public int mFrames = 0;
        private final int mExpectedSize;
        public FrameCatcher(int width, int height) {
            mExpectedSize = width * height * 3 / 2;
        }
        
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mExpectedSize != data.length) {
                throw new UnsupportedOperationException("bad size, got " + data.length + " expected " + mExpectedSize);
            }
            //Should we grab a frame
            long now = currentTime();
            if (now > lastFrameTime + 1000/FramesPerSec ){
            	//Remember the current time for next round
            	lastFrameTime = now;
            	cameraBuffers[currentBuffer]=data.clone();
            	currentBuffer++; 
            	if (currentBuffer == NUM_CAMERA_PREVIEW_BUFFERS) currentBuffer=0;
            	totalFramesCaptured++;
            }
            mFrames++;
            
            camera.addCallbackBuffer(data);
        }

    }
    
    static final int NUM_CAMERA_PREVIEW_BUFFERS = 100;
    byte [][] cameraBuffers=new byte[NUM_CAMERA_PREVIEW_BUFFERS][0];
    private int currentBuffer=0;
    private int totalFramesCaptured=0;
    private void setupCallback(Camera camera, FrameCatcher catcher, int bufferSize) {
        camera.setPreviewCallbackWithBuffer(null);
        camera.setPreviewCallbackWithBuffer(catcher);
        //for (int i = 0; i < NUM_CAMERA_PREVIEW_BUFFERS; i++) {
        	byte []cameraBuffer = new byte[bufferSize];
        	camera.addCallbackBuffer(cameraBuffer);
       // }
    }
    
    public void  onActivityResult(int request, int result, Intent i)
    {
    	if (result == RESULT_OK){
    		Toast.makeText(thisContext, "PICTURE", Toast.LENGTH_SHORT).show();
    	}
    }
    
    private byte[] getJpeg(byte[] data){
    
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, setSize.width, setSize.height, null);
    	yuvImage.compressToJpeg(new Rect(0, 0, setSize.width, setSize.height), 50, out);
    
    	return out.toByteArray();
    	
    }
    
    private void renderImage(byte[] data){
    	byte[] imageBytes = getJpeg(data);
    	Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    	image.setImageBitmap(bitmap);
    }

    private class Album{
    	long timestamp;
    	byte[] rawPicture;
    }
    ArrayList<Album> album = new ArrayList<Album>();
    
    long currentTime = currentTime();
    private long currentTime(){
    	currentTime = System.currentTimeMillis();
    	return 	currentTime;
    }
    
    private final long BufferSeconds=5000; 
    private boolean oldPic(long picTime){
    	return picTime < currentTime - BufferSeconds;
    }
    
    private void addNew(byte[] data){
    	Album a = new Album();
    	a.timestamp = currentTime();
    	a.rawPicture = data;
    	album.add(a);
    }
    
    private void removeOld()
    {
    	for (int i=0;i<album.size();i++){
    		if (oldPic(album.get(i).timestamp)) album.remove(i);
    	}
    }
    
    PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
        	if (data == null) return;
        	addNew(data);
        	removeOld();

            if (album.size()>1){ma.setTitle(" " + (album.get(album.size()-1).timestamp - album.get(album.size()-2).timestamp));}
        	
        	//Capture the next frame
        	mCamera.takePicture(null, null, mPicture );
        	
            File pictureFile = null; //getOutputMediaFile();
            if (pictureFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            }
        }


    };
    
    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date(0));
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    private int currentPicture=0;
    
    public boolean setPicture(int i){
    	if (i<0 || i>=Math.min(NUM_CAMERA_PREVIEW_BUFFERS,totalFramesCaptured)) return false;
    	
    	if(true){
    	 renderImage(this.cameraBuffers[i]);
    	 return true;
    	}
    	
    	if (false){
    	byte[] rawPicture = ((Album)album.get(i)).rawPicture;
    	Bitmap bMap = BitmapFactory.decodeByteArray(rawPicture, 0, rawPicture.length);
    	image.setImageBitmap(bMap);
    	}
    	if (false){
    	InputStream is = this.getResources().openRawResource(R.drawable.ic_launcher);
    	Bitmap originalBitmap = BitmapFactory.decodeStream(is);  
    	image.setImageBitmap(originalBitmap);
    	}
    	
    	if (true){
    		
    	}
    	
    	return true;
    }
    
    private boolean recording = false;
    private GestureDetector createGestureDetector(Context context) {
    	final GestureDetector gestureDetector = new GestureDetector(context);
        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @SuppressWarnings("static-access")
			@Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
            		try {
            			if (false){
            	    	InputStream is = ma.getResources().openRawResource(R.drawable.ic_launcher);
            	    	Bitmap originalBitmap = BitmapFactory.decodeStream(is);  
            	    	image.setImageBitmap(originalBitmap);
            			}
   
            			recording = !recording; //Toggle

            		    if (recording) {
            		    	setup();
            		    	message.setText("REC");
            		    	preview.setVisibility(View.VISIBLE);
            		    	image.setVisibility(View.INVISIBLE);
            		    }else {
            		    	message.setText("STOPPED");
            		    	stopCamera();
            		    	preview.setVisibility(View.INVISIBLE);
            		    	image.setVisibility(View.VISIBLE);
            		    	currentPicture = currentBuffer-1;
            		    	setPicture(currentPicture);
            		    	

            		    }
            		}catch(Exception e){
            			Log.d("Exc",e.getMessage() + e.getStackTrace());
            		}finally{
            		}
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    // do something on two finger tap
                	Toast.makeText(thisContext, "TWO TAP", Toast.LENGTH_SHORT).show();
    		    	//TODO - add options menu
    		    	openOptionsMenu();
                	return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    // do something on right (forward) swipe
                	if (setPicture(currentPicture+1)) currentPicture++;
                	//Toast.makeText(thisContext, "SWIPE R", Toast.LENGTH_SHORT).show();
                	message.setText(String.format("Next (%d)>",currentPicture));
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    // do something on left (backwards) swipe
                	message.setText(String.format("<Prev (%d)",currentPicture));
                   	if (setPicture(currentPicture-1)) currentPicture--;
                                   	//Toast.makeText(thisContext, "SWIPE L", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
        gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {
              // do something on finger count changes
            }
        });
        gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                // do something on scrolling
            	//Toast.makeText(thisContext, "SCROLL", Toast.LENGTH_SHORT).show();
            	return true;
            }
        });
        return gestureDetector;
    }

    /*
     * Send generic motion events to the gesture detector
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }
    @Override
    protected void onPause (){
    	
    	try {
    	  super.onPause();
    	  stopCamera();
    	}catch(Exception e){}

    }
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
			int height) {
        mCamera = Camera.open();

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }
		
	}
	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
        if (mCamera != null) {
        	mCamera.stopPreview();
        	mCamera.release();
        }
        return true;

	}
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1,
			int arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// TODO Auto-generated method stub
		
	}
}