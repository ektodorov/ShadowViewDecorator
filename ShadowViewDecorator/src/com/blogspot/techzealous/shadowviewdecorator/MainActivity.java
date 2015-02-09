package com.blogspot.techzealous.shadowviewdecorator;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
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
	
	private ShadowViewDecorator mDecorator;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		
		mButtonRect = (Button)findViewById(R.id.buttonRectLeft);
		mButtonRectRound = (Button)findViewById(R.id.buttonRectRoundRight);
		mButtonCircle = (Button)findViewById(R.id.buttonCircle);
		RelativeLayout rl = (RelativeLayout)findViewById(R.id.relativeLayoutMain);
		
		mDecorator = new ShadowViewDecorator(this);
		
		mButtonRect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {			
				Log.i("MainActivity", "mButtonRect, onClick");		
				
				mDecorator.dropShadowBoxBlur(mButtonRect, 3, Color.GREEN);
				mDecorator.dropShadowBoxBlur(mButtonRectRound, 3, Color.BLUE);
				mDecorator.dropShadowBoxBlur(mButtonCircle, 3, Color.YELLOW);
				
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
				mDecorator.dropShadowBoxBlur(mButtonRect, 3, Color.GREEN);
				mDecorator.dropShadowBoxBlur(mButtonRectRound, 3, Color.BLUE);
				mDecorator.dropShadowBoxBlur(mButtonCircle, 3, Color.YELLOW);
			}
		});
	}
	
	private Bitmap gaussianBlur(Bitmap aBitmap, int aSize, int aShadowColor)
	{
		Bitmap bitmapRet = aBitmap;
		
//		RenderScript rs = RenderScript.create(MainActivity.this);
//		final Allocation input = Allocation.createFromBitmap(rs, bitmapRet);
//		final Allocation output = Allocation.createTyped(rs, input.getType());
//		final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
//		script.setRadius(8f);
//		script.setInput(input);
//		script.forEach(output);
//		output.copyTo(bitmapRet);
		
		return bitmapRet;
	}
}
