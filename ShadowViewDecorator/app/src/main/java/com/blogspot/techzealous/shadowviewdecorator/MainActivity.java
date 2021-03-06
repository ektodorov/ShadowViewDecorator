package com.blogspot.techzealous.shadowviewdecorator;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

	public static final String LOG = "MainActivity";

	private Button mButtonRect;
	private Button mButtonRectRound;
	private Button mButtonCircle;
	private Button mButtonArrow;
	private TextView mTextViewHorizontal;

	private Resources mResources;
	private ShadowViewDecorator mDecorator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		mButtonRect = (Button)findViewById(R.id.buttonRectLeft);
		mButtonRectRound = (Button)findViewById(R.id.buttonRectRoundRight);
		mButtonCircle = (Button)findViewById(R.id.buttonCircle);
		mButtonArrow = (Button)findViewById(R.id.buttonArrow);
		mTextViewHorizontal = (TextView)findViewById(R.id.textViewHorizontal);
		RelativeLayout rl = (RelativeLayout)findViewById(R.id.relativeLayoutMain);

		mResources = getResources();
		mDecorator = new ShadowViewDecorator(this);

		mButtonRect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("MainActivity", "mButtonRect, onClick");
				Log.i("MainActivity", "width=" + mButtonRect.getWidth() + ", height=" + mButtonRect.getHeight());
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
                //mDecorator.dropShadowBoxBlur(mButtonRect, 4, Color.BLACK, 10, 10, true);
                //mDecorator.boxShadow(mButtonRect, 4, 4, 0, -2, Color.BLACK, 100, 10);
                //mDecorator.boxShadowGaussian(mButtonRect, 4, 4, 0, -2, Color.BLACK);
				//mDecorator.dropShadowGaussianBlur(mButtonRect, 4, Color.BLACK, -4, -4, true);
				mDecorator.boxShadowBoxBlur(mButtonRect, 2, 2, 4, 0, Color.BLACK);

                //mDecorator.dropShadow(mButtonRect, 4, 4, Color.BLACK, 20, 5, true, true);
				mDecorator.dropShadow(mButtonRectRound, 4, 4, Color.BLACK, 20, 5, 0, 0, false, null);
				mDecorator.dropShadow(mButtonCircle, 4, 4, Color.BLACK, 20, 5, 0, 0, false, null);
				mDecorator.dropShadow(mButtonArrow, 4, 4, Color.BLACK, 20, 5, 0, 0, false, null);
				Bitmap bitmap = ShadowViewDecorator.convertToBitmap(mTextViewHorizontal.getBackground(),
						mTextViewHorizontal.getWidth(), mTextViewHorizontal.getHeight());
				Bitmap bitmapShadow = mDecorator.createShadow(bitmap, 3, 3, Color.BLACK, 30, 10, false, false, false, true);
				mTextViewHorizontal.setBackgroundDrawable(new BitmapDrawable(mResources, bitmapShadow));
			}
		});
	}
}
