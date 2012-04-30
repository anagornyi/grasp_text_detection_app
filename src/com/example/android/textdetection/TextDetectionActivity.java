package com.example.android.textdetection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.CvType;
import org.opencv.core.Size;
import org.opencv.core.Rect;

import com.googlecode.tesseract.android.TessBaseAPI;


public class TextDetectionActivity extends Activity {
	
	private static final int ACTION_TAKE_PHOTO = 1;
	
	//private ImageView mImageView;
	//private Bitmap mImageBitmap;
	
	private static int CURRENT_VIEW = 1;
	
	private String mCurrentPhotoPath;
	private String mCurrentPhotoDir;
	
	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";
	
	private DetectTextNative dtn;
	
	private int[] boundingBoxes;
	
	private Mat curImg;
	private Bitmap curProcessedImage;
	
	
	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
	
	private String getAlbumName() {
		return getString(R.string.go_to_gallery);
	}
			
	private File setUpPhotoFile() throws IOException {
		
		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();
		
		return f;
	}
	
	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			
			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
			
			mCurrentPhotoDir = storageDir.getAbsolutePath();
			
		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}
		
		return storageDir;
	}
	
	
	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		return imageF;
	}
	
	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

//		/* Get the size of the ImageView */
//		int targetW = mImageView.getWidth();
//		int targetH = mImageView.getHeight();
//
//		/* Get the size of the image */
//		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//		bmOptions.inJustDecodeBounds = true;
//		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
//		int photoW = bmOptions.outWidth;
//		int photoH = bmOptions.outHeight;
//		
//		/* Figure out which way needs to be reduced less */
//		int scaleFactor = 1;
//		if ((targetW > 0) || (targetH > 0)) {
//			scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
//		}
//
//		/* Set bitmap options to scale the image decode target */
//		bmOptions.inJustDecodeBounds = false;
//		bmOptions.inSampleSize = scaleFactor;
//		bmOptions.inPurgeable = true;
//
//		/* Decode the JPEG file into a Bitmap */
//		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
//		
//		/* Associate the Bitmap to the ImageView */
//		mImageView.setImageBitmap(bitmap);
//		mImageView.setVisibility(View.VISIBLE);
		
		
	}
	
//	private void galleryAddPic() {
//	    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
//		File f = new File(mCurrentPhotoPath);
//	    Uri contentUri = Uri.fromFile(f);
//	    mediaScanIntent.setData(contentUri);
//	    this.sendBroadcast(mediaScanIntent);
//	}
	
	private void dispatchTakePictureIntent(int actionCode) {

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		switch(actionCode) {
		case ACTION_TAKE_PHOTO:
			File f = null;
			
			try {
				f = setUpPhotoFile();
				mCurrentPhotoPath = f.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			} catch (IOException e) {
				e.printStackTrace();
				f = null;
				mCurrentPhotoPath = null;
			}
			break;

		default:
			break;			
		} // switch

		startActivityForResult(takePictureIntent, actionCode);
	}
	
	private void processPhoto() {

		CURRENT_VIEW = 2;
		
		setContentView(R.layout.view_image);
		
		Button uploadButton = (Button) findViewById(R.id.upload);
		uploadButton.setVisibility(View.GONE);
		
		curImg = Highgui.imread(mCurrentPhotoPath, -1);
		Imgproc.resize(curImg, curImg, new Size(curImg.cols()*1/2, curImg.rows()*1/2));
		Imgproc.cvtColor(curImg, curImg, Imgproc.COLOR_RGB2RGBA, 4);
		curImg = curImg.t();		
		Core.flip(curImg, curImg, 1);

		new BoundingBoxesAsyncTask(this, dtn, (ImageView) findViewById(R.id.imageView1)).execute();
	
	}
	
	private void handleCameraPhoto() {
		if (mCurrentPhotoPath != null) {
			processPhoto();
		}
	}
	
	Button.OnClickListener mTakePicOnClickListener = 
			new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				dispatchTakePictureIntent(ACTION_TAKE_PHOTO);
			}
	};

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button pictureButton = (Button) findViewById(R.id.buttonPicture);
		setBtnListenerOrDisable( 
				pictureButton, 
				mTakePicOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE
		);
		
//		Button uploadButton = (Button) findViewById(R.id.upload);
//		uploadButton.setVisibility(View.GONE);
//		uploadButton.setOnClickListener(new Button.OnClickListener() {
//            public void onClick(View v) {
//            	System.out.println("UPLOAD");	
//            }
//        });
		
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			mAlbumStorageDirFactory = new GingerbreadAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
		
		dtn = new DetectTextNative();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PHOTO: {
			if (resultCode == RESULT_OK) {
				handleCameraPhoto();
			}
			break;
		}
		}
	}
	
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	private void setBtnListenerOrDisable( 
			Button btn, 
			Button.OnClickListener onClickListener,
			String intentName
	) { 
		if (isIntentAvailable(this, intentName)) {
			btn.setOnClickListener(onClickListener);        	
		} else {
			btn.setText( 
				getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}
	
	@Override
	protected void onDestroy() {
		dtn.destroy();
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		if(CURRENT_VIEW != 1) {
			CURRENT_VIEW = 1;
			setContentView(R.layout.main);
			Button pictureButton = (Button) findViewById(R.id.buttonPicture);
			setBtnListenerOrDisable( 
					pictureButton, 
					mTakePicOnClickListener,
					MediaStore.ACTION_IMAGE_CAPTURE
			);
		} else super.onBackPressed();
	}

	private class BoundingBoxesAsyncTask extends AsyncTask<Void, Void, Bitmap> {
		
		private ProgressDialog pd;
		private Context context;
		private DetectTextNative dtn;
		private ImageView imageView;
		
		public BoundingBoxesAsyncTask(Context context, DetectTextNative dtn, ImageView imageView) {
			this.context = context;
			this.dtn = dtn;
			this.imageView = imageView;
		}
		
		@Override
		protected void onPreExecute() {
			pd = new ProgressDialog(context);
			pd.setMessage("Processing Image ...");
			pd.show();
			super.onPreExecute();
			
		}

		@Override
		protected Bitmap doInBackground(Void...args) {
			
			Bitmap bitmap = Bitmap.createBitmap(curImg.cols(), curImg.rows(), Bitmap.Config.ARGB_8888); 
			
			boundingBoxes = dtn.getBoundingBoxes(curImg.getNativeObjAddr());
				
			Mat img_copy = curImg.clone(), patchMat;
			Bitmap patchBit;
			Point rect_pt1, rect_pt2;
			Rect roi;
			String basePath = mCurrentPhotoPath.split(JPEG_FILE_SUFFIX)[0], curPath;
			
			TessBaseAPI baseApi = new TessBaseAPI();
			baseApi.init("/mnt/sdcard/", "eng");
			
			
			for(int i = 0; i < boundingBoxes.length; i+=4) {
				rect_pt1 = new Point(boundingBoxes[i], boundingBoxes[i+1]);
				rect_pt2 = new Point(boundingBoxes[i] + boundingBoxes[i+2], boundingBoxes[i+1] + boundingBoxes[i+3]);
				roi = new Rect(boundingBoxes[i], boundingBoxes[i+1], boundingBoxes[i+2], boundingBoxes[i+3]);
				
				patchMat = img_copy.submat(roi);
				patchBit = Bitmap.createBitmap(patchMat.width(), patchMat.height(), Bitmap.Config.ARGB_8888);
				
				Utils.matToBitmap(patchMat.clone(), patchBit);
				
				curPath = basePath+"_tmp_patch_" + (i/4 + 1)+ ".jpg";
				 
				baseApi.setImage(patchBit);
				String recognizedText = baseApi.getUTF8Text();
				
				System.out.println(recognizedText);
				
				Highgui.imwrite(curPath, patchMat);
				
				Core.rectangle(curImg, rect_pt1, rect_pt2, new Scalar(0, 0, 255), 10);
			}

			baseApi.end();
			Utils.matToBitmap(curImg, bitmap);
			
			return bitmap;
		}

		@Override 
		protected void onPostExecute(Bitmap result) {
			pd.dismiss();
			imageView.setImageBitmap(result);
		
			curProcessedImage = result;
			
			Button uploadButton = (Button) findViewById(R.id.upload);
			uploadButton.setVisibility(View.VISIBLE);
			
			uploadButton.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {

				    try {
					    HttpClient httpclient = new DefaultHttpClient();
					    HttpPost httppost = new HttpPost("http://107.20.255.113/upload");  	
				        HttpResponse response = httpclient.execute(httppost);			        
				    } catch (ClientProtocolException e) {
				    	e.printStackTrace();
				    } catch (IOException e) {
				    	e.printStackTrace();
				    }
				System.out.println("UPLOAD");	
				}
			});
		}

		
	}
	
}
