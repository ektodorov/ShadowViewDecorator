package com.blogspot.techzealous.shadowviewdecorator;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class ShadowViewDecorator {

	private static final String LOG = "ShadowViewDecorator";
	private static final String STR_SDK_LESS_THAN_JELLYBEANMR1 = "Warning current cannot make Guassian blur. ScriptIntrinsicBlur requires API Level 17+, current SDK Level is ";
	private static final String STR_METHOD_SETELEVATION = "setElevation";
	private static final int kSDK_LEVEL_JELLYBEANMR1 = 17;
	private static final int kSDK_LEVEL_LOLLIPOP = 21;
	
	private WeakReference<Context> mWeakCtx;
	private Handler mHandler;
	private ExecutorService mExecutor;
	
	public ShadowViewDecorator(Context aCtx)
	{
		mWeakCtx = new WeakReference<Context>(aCtx);
		mHandler = new Handler(Looper.getMainLooper());
		mExecutor = Executors.newSingleThreadExecutor();
	}
	
	public void dropShadow(final View aView, final int aShadowSize, final int aShadowLayersCount, final int aShadowColor, 
			final int aAlphaInit, final int aAlphaStep)
	{		
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				Bitmap bitmapCurrent = convertToBitmap(aView.getBackground(), aView.getWidth(), aView.getHeight());
				int bitmapCurrentWidth = bitmapCurrent.getWidth();
				int bitmapCurrentHeight = bitmapCurrent.getHeight();
				
				Bitmap bitmapAlpha = bitmapCurrent.extractAlpha();
				final Bitmap bitmap = Bitmap.createBitmap(bitmapCurrentWidth, bitmapCurrentHeight, Config.ARGB_8888);
				Rect rectSrc = new Rect(0, 0, bitmapCurrentWidth, bitmapCurrentHeight);
				Rect rectDest = new Rect(0, 0, bitmapCurrentWidth, bitmapCurrentHeight);
				
				Paint paint = new Paint();
				paint.setColor(aShadowColor);
				paint.setAlpha(aAlphaInit);
				Canvas canvas = new Canvas(bitmap);
				
				int add = aShadowSize / aShadowLayersCount;
				for(int x = 0; x < aShadowLayersCount; x++) {
					canvas.drawBitmap(bitmapAlpha, rectSrc, rectDest, paint);
					int alpha = paint.getAlpha() + aAlphaStep;
					if(alpha > 255) {alpha = 255;}
					paint.setAlpha(alpha);
					rectDest.set(rectDest.left + add, rectDest.top + add, rectDest.right - add, rectDest.bottom - add);
				}
				canvas.drawBitmap(bitmapCurrent, rectSrc, rectDest, null);
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Context ctx = mWeakCtx.get();
						if(ctx == null) {return;}
						
						aView.setBackgroundDrawable(new BitmapDrawable(ctx.getResources(), bitmap));
					}
				});
			}
		});
	}
	
	public void dropShadowOffset(final View aView, final int aOffsetLeft, final int aOffsetTop, final int aShadowSize, final int aShadowLayersCount, 
			final int aShadowColor, final int aAlphaInit, final int aAlphaStep, final ChangeMargins aChangeMargins)
	{	
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				Bitmap bitmapCurrent = convertToBitmap(aView.getBackground(), aView.getWidth(), aView.getHeight());
				int bitmapCurrentWidth = bitmapCurrent.getWidth();
				int bitmapCurrentHeight = bitmapCurrent.getHeight();
				
				Bitmap bitmapAlpha = bitmapCurrent.extractAlpha();
				final Bitmap bitmap = Bitmap.createBitmap(bitmapCurrentWidth, bitmapCurrentHeight, Config.ARGB_8888);
				Rect rectSrc = new Rect(0, 0, bitmapCurrentWidth, bitmapCurrentHeight);
				Rect rectDest = new Rect(aOffsetLeft, aOffsetTop, bitmapCurrentWidth, bitmapCurrentHeight);
				
				Paint paint = new Paint();
				paint.setColor(aShadowColor);
				paint.setAlpha(aAlphaInit);
				Canvas canvas = new Canvas(bitmap);
				
				int add = aShadowSize / aShadowLayersCount;
				for(int x = 0; x < aShadowLayersCount; x++) {
					canvas.drawBitmap(bitmapAlpha, rectSrc, rectDest, paint);
					int alpha = paint.getAlpha() + aAlphaStep;
					if(alpha > 255) {alpha = 255;}
					paint.setAlpha(alpha);
					rectDest.set(rectDest.left + add, rectDest.top + add, rectDest.right - add, rectDest.bottom - add);
				}
				rectDest.set(aShadowSize, aShadowSize, bitmapCurrentWidth - aShadowSize, bitmapCurrentHeight - aShadowSize);
				canvas.drawBitmap(bitmapCurrent, rectSrc, rectDest, null);
				
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Context ctx = mWeakCtx.get();
						if(ctx == null) {return;}
						
						aView.setBackgroundDrawable(new BitmapDrawable(ctx.getResources(), bitmap));
						
						ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)aView.getLayoutParams();
//						if(aChangeMargins.changeLeft) {params.rightMargin = params.rightMargin - aShadowSize;}
//						if(aChangeMargins.changeTop) {params.topMargin = params.topMargin - aShadowSize;}
//						if(aChangeMargins.changeRight) {params.rightMargin = params.rightMargin - aShadowSize;}
//						if(aChangeMargins.changeBottom) {params.bottomMargin = params.bottomMargin - aShadowSize;}
						params.setMargins((aChangeMargins.changeLeft) ? params.leftMargin - aShadowSize : params.leftMargin, 
								(aChangeMargins.changeTop) ? params.topMargin - aShadowSize : params.topMargin, 
								(aChangeMargins.changeRight) ? params.rightMargin - aShadowSize : params.rightMargin, 
								(aChangeMargins.changeBottom) ? params.bottomMargin - aShadowSize : params.bottomMargin);
					}
				});
			}
		});
	}
	
	public void dropShadowBoxBlur(final View aView, final int aShadowSize, final int aShadowColor)
	{
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				float kernel = 1.0f / ((aShadowSize * 2) + 1);
				Bitmap bitmapCurrent = convertToBitmap(aView.getBackground(), aView.getWidth(), aView.getHeight());
				Bitmap bitmapAlpha = bitmapCurrent.extractAlpha();
				final Bitmap bitmap = Bitmap.createBitmap(bitmapCurrent.getWidth(), bitmapCurrent.getHeight(), Config.ARGB_8888);
				
				Rect rectSrc = new Rect(0, 0, bitmapCurrent.getWidth(), bitmapCurrent.getHeight());
				Rect rectDest = new Rect(aShadowSize, aShadowSize, bitmapCurrent.getWidth() - aShadowSize, bitmapCurrent.getHeight() - aShadowSize);
				
				Paint paint = new Paint();
				paint.setColor(aShadowColor);
				Canvas canvas = new Canvas(bitmap);
				canvas.drawBitmap(bitmapAlpha, rectSrc, rectDest, paint);
				bitmapAlpha.recycle();
				
				/* 
				 * Box blur
				 * If we didn't have to support different shadow colors we would be using only one color and won't have to multiply them all.
				 * example we use only red: bitmap.setPixel(col, row, Color.argb((int)sumAlpha, (int)sumRed, (int)sumRed, (int)sumRed));
				 */
				int colsCount = bitmapCurrent.getWidth();
				int rowsCount = bitmapCurrent.getHeight();
				
				for(int row = 0; row < rowsCount; row++) {
					for(int col = 0; col < colsCount; col++) {
						float sumAlpha = 0;
						float sumRed = 0;
						float sumGreen = 0;
						float sumBlue = 0;
						for(int k = col - aShadowSize; k <= col + aShadowSize; k++) {
							int pixel = 0;
							if(k < 0) {
								pixel = Color.argb(0, 255, 255, 255);
							} else if(k >= colsCount) {
								pixel = Color.argb(0, 255, 255, 255);
							} else {
								pixel = bitmap.getPixel(k, row);
							}
							
							int alpha = Color.alpha(pixel);
							int red = Color.red(pixel);
							int green = Color.green(pixel);
							int blue = Color.blue(pixel);
								
							float resultAlpha = (float)alpha * kernel;
							float resultRed = (float)red * kernel;
							float resultGreen = (float)green * kernel;
							float resultBlue = (float)blue * kernel;
								
							sumAlpha += resultAlpha;
							sumRed += resultRed;
							sumGreen += resultGreen;
							sumBlue += resultBlue;
						}
						bitmap.setPixel(col, row, Color.argb((int)sumAlpha, (int)sumRed, (int)sumGreen, (int)sumBlue));
					}
				}
				
				for(int col = 0; col < colsCount; col++) {
					for(int row = 0; row < rowsCount; row++) {
						float sumAlpha = 0;
						float sumRed = 0;
						float sumGreen = 0;
						float sumBlue = 0;
						for(int k = row - aShadowSize; k <= row + aShadowSize; k++) {
							int pixel = 0;
							if(k < 0) {
								pixel = Color.argb(0, 255, 255, 255);
							} else if(k >= rowsCount) {
								pixel = Color.argb(0, 255, 255, 255);
							} else {
								pixel = bitmap.getPixel(col, k);
							}
							
							int alpha = Color.alpha(pixel);
							int red = Color.red(pixel);
							int green = Color.green(pixel);
							int blue = Color.blue(pixel);
								
							float resultAlpha = (float)alpha * kernel;
							float resultRed = (float)red * kernel;
							float resultGreen = (float)green * kernel;
							float resultBlue = (float)blue * kernel;
								
							sumAlpha += resultAlpha;
							sumRed += resultRed;
							sumGreen += resultGreen;
							sumBlue += resultBlue;
						}
						bitmap.setPixel(col, row, Color.argb((int)sumAlpha, (int)sumRed, (int)sumGreen, (int)sumBlue));
					}
				}
				
				//draw the blurred alpha	
				canvas.drawBitmap(bitmap, rectSrc, rectSrc, null);
				
				//1.decrease the rectDest with the amount of the shadow
				//2.draw the background we took of the view in the rectDest
				rectDest.set(aShadowSize, aShadowSize, bitmapCurrent.getWidth() - aShadowSize, bitmapCurrent.getHeight() - aShadowSize);
				canvas.drawBitmap(bitmapCurrent, rectSrc, rectDest, null);
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Context ctx = mWeakCtx.get();
						if(ctx == null) {return;}
						
						aView.setBackgroundDrawable(new BitmapDrawable(ctx.getResources(), bitmap));
					}
				});
			}
		});
	}
	
	public void dropShadowBoxBlurOffset(View aView, int aShadowSize, int aShadowColor, int aOffsetLeft, int aOffsetTop)
	{
		
	}
	
	public void dropShadowGaussianBlur(View aView, int aShadowSize, int aShadowColor)
	{
		
	}
	
	public void dropShadowGaussianBlurOffset(View aView, int aShadowSize, int aShadowColor, int aOffsetLeft, int aOffsetTop)
	{
		
	}
	
	public void dropShadowCompat(View aView, int aShadowSize, int aShadowLayersCount, int aShadowColor, 
			int aAlphaInit, int aAlphaStep, float aElevation)
	{
		int sdkLevel = android.os.Build.VERSION.SDK_INT;
		if(sdkLevel >= kSDK_LEVEL_LOLLIPOP) {
			try {
				Method methodSetElevation = aView.getClass().getMethod(STR_METHOD_SETELEVATION, new Class[]{float.class});
				methodSetElevation.invoke(aView, new Object[]{aElevation});
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				dropShadow(aView, aShadowSize, aShadowLayersCount, aShadowColor, aAlphaInit, aAlphaStep);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				dropShadow(aView, aShadowSize, aShadowLayersCount, aShadowColor, aAlphaInit, aAlphaStep);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				dropShadow(aView, aShadowSize, aShadowLayersCount, aShadowColor, aAlphaInit, aAlphaStep);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				dropShadow(aView, aShadowSize, aShadowLayersCount, aShadowColor, aAlphaInit, aAlphaStep);
			}
		} else {
			dropShadow(aView, aShadowSize, aShadowLayersCount, aShadowColor, aAlphaInit, aAlphaStep);
		}
	}
	
	public void dropShadowOffsetCompat(View aView, int aOffsetLeft, int aOffsetTop, int aShadowSize, int aShadowLayersCount, int aShadowColor, 
			int aAlphaInit, int aAlphaStep, ChangeMargins aChangeMargins, float aElevation)
	{
		int sdkLevel = android.os.Build.VERSION.SDK_INT;
		if(sdkLevel >= kSDK_LEVEL_LOLLIPOP) {
			try {
				Method methodSetElevation = aView.getClass().getMethod(STR_METHOD_SETELEVATION, new Class[]{float.class});
				methodSetElevation.invoke(aView, new Object[]{aElevation});
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				dropShadowOffset(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor, 
					aAlphaInit, aAlphaStep, aChangeMargins);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				dropShadowOffset(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor, 
					aAlphaInit, aAlphaStep, aChangeMargins);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				dropShadowOffset(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor, 
					aAlphaInit, aAlphaStep, aChangeMargins);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				dropShadowOffset(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor, 
					aAlphaInit, aAlphaStep, aChangeMargins);
			}
		} else {
			dropShadowOffset(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor, aAlphaInit, aAlphaStep, aChangeMargins);
		}
	}
	
	public void dropShadowBoxBlurCompat(View aView, int aShadowSize, int aShadowColor, float aElevation)
	{
		int sdkLevel = android.os.Build.VERSION.SDK_INT;
		if(sdkLevel >= kSDK_LEVEL_LOLLIPOP) {
			try {
				Method methodSetElevation = aView.getClass().getMethod(STR_METHOD_SETELEVATION, new Class[]{float.class});
				methodSetElevation.invoke(aView, new Object[]{aElevation});
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				dropShadowBoxBlur(aView, aShadowSize, aShadowColor);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				dropShadowBoxBlur(aView, aShadowSize, aShadowColor);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				dropShadowBoxBlur(aView, aShadowSize, aShadowColor);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				dropShadowBoxBlur(aView, aShadowSize, aShadowColor);
			}
		} else {
			dropShadowBoxBlur(aView, aShadowSize, aShadowColor);
		}
	}
	
	public void dropShadowBoxBlurOffsetCompat(View aView, int aShadowSize, int aShadowColor, 
			int aOffsetLeft, int aOffsetTop, float aElevation)
	{
		int sdkLevel = android.os.Build.VERSION.SDK_INT;
		if(sdkLevel >= kSDK_LEVEL_LOLLIPOP) {
			try {
				Method methodSetElevation = aView.getClass().getMethod(STR_METHOD_SETELEVATION, new Class[]{float.class});
				methodSetElevation.invoke(aView, new Object[]{aElevation});
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				dropShadowBoxBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				dropShadowBoxBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				dropShadowBoxBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				dropShadowBoxBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
			}
		} else {
			dropShadowBoxBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
		}
	}
	
	public void dropShadowGaussianBlurCompat(View aView, int aShadowSize, int aShadowColor, float aElevation)
	{
		int sdkLevel = android.os.Build.VERSION.SDK_INT;
		if(sdkLevel >= kSDK_LEVEL_LOLLIPOP) {
			try {
				Method methodSetElevation = aView.getClass().getMethod(STR_METHOD_SETELEVATION, new Class[]{float.class});
				methodSetElevation.invoke(aView, new Object[]{aElevation});
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				dropShadowGaussianBlur(aView, aShadowSize, aShadowColor);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				dropShadowGaussianBlur(aView, aShadowSize, aShadowColor);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				dropShadowGaussianBlur(aView, aShadowSize, aShadowColor);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				dropShadowGaussianBlur(aView, aShadowSize, aShadowColor);
			}
		} else {
			dropShadowGaussianBlur(aView, aShadowSize, aShadowColor);
		}
	}
	
	public void dropShadowGaussianBlurOffsetCompat(View aView, int aShadowSize, int aShadowColor, 
			int aOffsetLeft, int aOffsetTop, float aElevation)
	{
		int sdkLevel = android.os.Build.VERSION.SDK_INT;
		if(sdkLevel >= kSDK_LEVEL_LOLLIPOP) {
			try {
				Method methodSetElevation = aView.getClass().getMethod(STR_METHOD_SETELEVATION, new Class[]{float.class});
				methodSetElevation.invoke(aView, new Object[]{aElevation});
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				dropShadowGaussianBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				dropShadowGaussianBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				dropShadowGaussianBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				dropShadowGaussianBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
			}
		} else {
			dropShadowGaussianBlurOffset(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop);
		}
	}
	
	public Bitmap createShadow(Bitmap aBitmap, int aShadowSize, int aShadowLayersCount, int aShadowColor, int aAlphaInit, int aAlphaStep)
	{		
			Bitmap bitmapCurrent = aBitmap;
			int bitmapCurrentWidth = bitmapCurrent.getWidth();
			int bitmapCurrentHeight = bitmapCurrent.getHeight();
			
			Bitmap bitmapAlpha = bitmapCurrent.extractAlpha();
			final Bitmap bitmap = Bitmap.createBitmap(bitmapCurrentWidth, bitmapCurrentHeight, Config.ARGB_8888);
			Rect rectSrc = new Rect(0, 0, bitmapCurrentWidth, bitmapCurrentHeight);
			Rect rectDest = new Rect(0, 0, bitmapCurrentWidth, bitmapCurrentHeight);
				
			Paint paint = new Paint();
			paint.setColor(aShadowColor);
			paint.setAlpha(aAlphaInit);
			Canvas canvas = new Canvas(bitmap);
				
			int add = aShadowSize / aShadowLayersCount;
			for(int x = 0; x < aShadowLayersCount; x++) {
				canvas.drawBitmap(bitmapAlpha, rectSrc, rectDest, paint);
				int alpha = paint.getAlpha() + aAlphaStep;
				if(alpha > 255) {alpha = 255;}
				paint.setAlpha(alpha);
				rectDest.set(rectDest.left + add, rectDest.top + add, rectDest.right - add, rectDest.bottom - add);
			}
			canvas.drawBitmap(bitmapCurrent, rectSrc, rectDest, null);
			return bitmap;
	}
	
	public Bitmap createShadowOffset(Bitmap aBitmap, int aOffsetLeft, int aOffsetTop, int aShadowSize, int aShadowLayersCount, 
				int aShadowColor, int aAlphaInit, int aAlphaStep, ChangeMargins aChangeMargins, boolean aCreateNewBitmap)
	{	
		Bitmap bitmapCurrent = aBitmap;
		int bitmapCurrentWidth = bitmapCurrent.getWidth();
		int bitmapCurrentHeight = bitmapCurrent.getHeight();
		
		Bitmap bitmapAlpha = bitmapCurrent.extractAlpha();
		Bitmap bitmap = Bitmap.createBitmap(bitmapCurrentWidth, bitmapCurrentHeight, Config.ARGB_8888);
		Rect rectSrc = new Rect(0, 0, bitmapCurrentWidth, bitmapCurrentHeight);
		Rect rectDest = new Rect(aOffsetLeft, aOffsetTop, bitmapCurrentWidth, bitmapCurrentHeight);
				
		Paint paint = new Paint();
		paint.setColor(aShadowColor);
		paint.setAlpha(aAlphaInit);
		Canvas canvas = new Canvas(bitmap);
				
		int add = aShadowSize / aShadowLayersCount;
		for(int x = 0; x < aShadowLayersCount; x++) {
			canvas.drawBitmap(bitmapAlpha, rectSrc, rectDest, paint);
			int alpha = paint.getAlpha() + aAlphaStep;
			if(alpha > 255) {alpha = 255;}
			paint.setAlpha(alpha);
			rectDest.set(rectDest.left + add, rectDest.top + add, rectDest.right - add, rectDest.bottom - add);
		}
		rectDest.set(aShadowSize, aShadowSize, bitmapCurrentWidth - aShadowSize, bitmapCurrentHeight - aShadowSize);
		canvas.drawBitmap(bitmapCurrent, rectSrc, rectDest, null);
		return bitmap;
	}
	
	
	/* Utils */
	public static Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) 
	{
		Bitmap bitmapRet = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapRet);
		drawable.setBounds(0, 0, widthPixels, heightPixels);
		drawable.draw(canvas);

		return bitmapRet;
	}
	
	public static Bitmap boxBlur(Bitmap aBitmap, int aSize, boolean aCreateNewBitmap)
	{
		float kernel = 1.0f / ((aSize * 2) + 1);
		Bitmap bitmap = null;
		if(aCreateNewBitmap) {
			bitmap = Bitmap.createBitmap(aBitmap.getWidth(), aBitmap.getHeight(), Config.ARGB_8888);
		} else {
			bitmap = aBitmap;
		}
		
		Rect rectSrc = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		Rect rectDest = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		
		Canvas canvas = new Canvas(bitmap);
		
		//box blur
		int colsCount = bitmap.getWidth();
		int rowsCount = bitmap.getHeight();
		
		for(int row = 0; row < rowsCount; row++) {
			for(int col = 0; col < colsCount; col++) {
				float sumAlpha = 0;
				float sumRed = 0;
				float sumGreen = 0;
				float sumBlue = 0;
				for(int k = col - aSize; k <= col + aSize; k++) {
					int x = k;
					if(x < 0) {x = Math.abs(x);}
					if(x >= colsCount) {x = (colsCount - 1) - (x - colsCount);}
					
					int pixel = bitmap.getPixel(x, row);
					
					int alpha = Color.alpha(pixel);
					int red = Color.red(pixel);
					int green = Color.green(pixel);
					int blue = Color.blue(pixel);
						
					float resultAlpha = (float)alpha * kernel;
					float resultRed = (float)red * kernel;
					float resultGreen = (float)green * kernel;
					float resultBlue = (float)blue * kernel;
						
					sumAlpha += resultAlpha;
					sumRed += resultRed;
					sumGreen += resultGreen;
					sumBlue += resultBlue;
				}
				bitmap.setPixel(col, row, Color.argb((int)sumAlpha, (int)sumRed, (int)sumGreen, (int)sumBlue));
			}
		}
		
		for(int col = 0; col < colsCount; col++) {
			for(int row = 0; row < rowsCount; row++) {
				float sumAlpha = 0;
				float sumRed = 0;
				float sumGreen = 0;
				float sumBlue = 0;
				for(int k = row - aSize; k <= row + aSize; k++) {
					int y = k;
					if(y < 0) {y = Math.abs(y);}
					if(y >= rowsCount) {y = (rowsCount - 1) - (y - rowsCount);}
					
					int pixel = bitmap.getPixel(col, y);
					
					int alpha = Color.alpha(pixel);
					int red = Color.red(pixel);
					int green = Color.green(pixel);
					int blue = Color.blue(pixel);
						
					float resultAlpha = (float)alpha * kernel;
					float resultRed = (float)red * kernel;
					float resultGreen = (float)green * kernel;
					float resultBlue = (float)blue * kernel;
						
					sumAlpha += resultAlpha;
					sumRed += resultRed;
					sumGreen += resultGreen;
					sumBlue += resultBlue;
				}
				bitmap.setPixel(col, row, Color.argb((int)sumAlpha, (int)sumRed, (int)sumGreen, (int)sumBlue));
			}
		}
				
		canvas.drawBitmap(bitmap, rectSrc, rectDest, null);
		return bitmap;
	}
	
	public static Bitmap gaussianBlur(Context aCtx, Bitmap aBitmap, int aSize) 
	{
		Bitmap bitmapRet = aBitmap;
		if(android.os.Build.VERSION.SDK_INT >= kSDK_LEVEL_JELLYBEANMR1) {
			RenderScript rs = RenderScript.create(aCtx);
			final Allocation input = Allocation.createFromBitmap(rs, bitmapRet);
			final Allocation output = Allocation.createTyped(rs, input.getType());
			final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
			script.setRadius(aSize);
			script.setInput(input);
			script.forEach(output);
			output.copyTo(bitmapRet);
		} else {
			Log.w(LOG, STR_SDK_LESS_THAN_JELLYBEANMR1 + android.os.Build.VERSION.SDK_INT);
		}
		return bitmapRet;
	}
}
