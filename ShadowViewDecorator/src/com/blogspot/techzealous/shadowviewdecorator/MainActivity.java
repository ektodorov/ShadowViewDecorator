package com.blogspot.techzealous.shadowviewdecorator;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {

	public static final String LOG = "MainActivity";
	
	private Button mButtonRect;
	private Button mButtonRectRound;
	private Button mButtonCircle;
	
	private Resources mResources;
	private ShadowViewDecorator mDecorator;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		
		mButtonRect = (Button)findViewById(R.id.buttonRectLeft);
		mButtonRectRound = (Button)findViewById(R.id.buttonRectRoundRight);
		mButtonCircle = (Button)findViewById(R.id.buttonCircle);
		RelativeLayout rl = (RelativeLayout)findViewById(R.id.relativeLayoutMain);
		
		mResources = getResources();
		mDecorator = new ShadowViewDecorator(this);
		
		mButtonRect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {			
				Log.i("MainActivity", "mButtonRect, onClick");		
				
			}
		});
		
		mButtonRectRound.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {		
				Log.i("MainActivity", "mButtonRectRound, onClick");
				
			}
		});
		
		rl.post(new Runnable() {
			@Override
			public void run() {
				
			}
		});
	}
}
