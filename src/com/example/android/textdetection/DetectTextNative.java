package com.example.android.textdetection;

import org.opencv.core.Mat;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;


public class DetectTextNative {
	
    static { 
		System.loadLibrary("run_detection");
	}
	
	private long detectPtr = 0;
	
    public DetectTextNative() {
    	detectPtr = create();
    }

	private native long create();
	private native void destroy(long detectPtr);
	private native int [] getBoundingBoxes(long detectPtr, long matAddress);

	@Override
	protected void finalize() throws Throwable {
		if(detectPtr != 0) {
			destroy(detectPtr);
		}
		super.finalize();
	}
	
	public int [] getBoundingBoxes(long matAddress) {
		return getBoundingBoxes(detectPtr, matAddress);
	}
	
	public void destroy() {
		if(detectPtr != 0) {
			destroy(detectPtr);
		}
	}
	
	public Bitmap JPEGtoRGB888(Bitmap img) { 
		int numPixels = img.getWidth()* img.getHeight(); 
		int[] pixels = new int[numPixels]; 
		
		//Get JPEG pixels.  Each int is the color values for one 
		img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight()); 
		
		//Create a Bitmap of the appropriate format. 
		Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Config.ARGB_8888); 
		
		//Set RGB pixels. 
		result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight()); 
		return result; 
	}

}