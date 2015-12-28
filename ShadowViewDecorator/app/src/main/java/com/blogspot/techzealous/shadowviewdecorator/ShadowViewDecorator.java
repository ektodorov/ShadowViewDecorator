package com.blogspot.techzealous.shadowviewdecorator;

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
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShadowViewDecorator {

	private static final String LOG = "ShadowViewDecorator";
	private static final String STR_METHOD_setElevation = "setElevation";
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

    /**
     * Create a shadow in using CSS box shadow attributes. Uses ShadowViewDecorator's dropShadowOffset method.
     * @param aView - view to decorate.
     * @param aHorizontalShadow - horizontal offset / offset left.
     * @param aVerticalShadow - vertical offset / offset top.
     * @param aBlur - blur radius.
     * @param aSpread - a spread radius.
     * @param aColor - shadow color.
	 * @param aAlphaInitial - alpha value for the shadow. How intesive is the shadow.
     * @param aAlphaStep - alpha value to be added to shadow with each shadow layer. How the shadow fades.
     * @return void.
     */
    public void boxShadow(View aView, int aHorizontalShadow, int aVerticalShadow, int aBlur, int aSpread, int aColor,
			int aAlphaInitial, int aAlphaStep)
    {
        dropShadow(aView, aBlur + aSpread, ((aBlur == 0) ? 1 : aBlur), aColor, aAlphaInitial, aAlphaStep,
                aHorizontalShadow, aVerticalShadow, true, null);
    }

    /**
     * Create a shadow in using CSS box shadow attributes. Uses dropShadowBoxBlur method.
     * @param aView - view to decorate.
     * @param aHorizontalShadow - horizontal offset / offset left.
     * @param aVerticalShadow - vertical offset / offset top.
     * @param aBlur - blur radius.
     * @param aSpread - a spread radius.
     * @param aColor - shadow color.
     * @return void.
     */
    public void boxShadowBoxBlur(View aView, int aHorizontalShadow, int aVerticalShadow, int aBlur, int aSpread, int aColor)
    {
        dropShadowBoxBlur(aView, aBlur, aColor, aHorizontalShadow, aVerticalShadow, true);
    }

    /**
     * Create a shadow in using CSS box shadow attributes. Uses ShadowViewDecorator's dropShadowGaussianBlurOffset.
     * Supports only positive spread values. If spread argument is less than 0, spread will be set to 0.
     * @param aView - view to decorate.
     * @param aHorizontalShadow - horizontal offset / offset left.
     * @param aVerticalShadow - vertical offset / offset top.
     * @param aBlur - blur radius.
     * @param aSpread - a spread radius.
     * @param aColor - shadow color.
     * @return void.
     */
    public void boxShadowGaussian(View aView, int aHorizontalShadow, int aVerticalShadow, int aBlur, int aSpread, int aColor)
    {
        if(aSpread < 0) {aSpread = 0;}
        dropShadowGaussianBlur(aView, aBlur + aSpread, aColor, aHorizontalShadow, aVerticalShadow, true);
    }

    /**
     * Create a shadow in using CSS box shadow attributes.
     * @param aBitmap - bitmap to decorate.
     * @param aHorizontalShadow - horizontal offset / offset left.
     * @param aVerticalShadow - vertical offset / offset top.
     * @param aBlur - blur radius.
     * @param aSpread - a spread radius.
     * @param aColor - shadow color.
     * @return Bitmap - decorated with shadow bitmap.
     */
    public Bitmap boxShadow(Bitmap aBitmap, int aHorizontalShadow, int aVerticalShadow, int aBlur, int aSpread, int aColor)
    {
        Bitmap bitmapRet = createShadow(aBitmap, aHorizontalShadow, aVerticalShadow, aBlur + aSpread, aBlur, aColor, 100, 10, true);
        return bitmapRet;
    }

    /**
     * Draws a shadow around the passed in view, with option to offset from left and top.
     * Uses ShadowViewDecorator's drawing of shadow.
     * @param aView - view which to decorate with a shadow.
     * @param aShadowSize - size of the shadow in pixels.
     * @param aShadowLayersCount - shadow layers.
     * @param aShadowColor - color of the shadow.
     * @param aAlphaInit - initial alpha value for the shadow. How strong should be the shadow
     * @param aAlphaStep - value with which to increment the shadow alpha. How the shadow fades.
     * @param aOffsetLeft - offset of the shadow from left.
     * @param aOffsetTop - offset of the shadow from top.
     * @param aIsExpand - if the view should expand with the size of the shadow or if the view should keep it's size.
     * @param aChangeMargins - which margins of the view to change or null if none should be changed.
     * @return void.
     */
	public void dropShadow(final View aView, final int aShadowSize, final int aShadowLayersCount, final int aShadowColor,
            final int aAlphaInit, final int aAlphaStep, final int aOffsetLeft, final int aOffsetTop,
            final boolean aIsExpand, final ChangeMargins aChangeMargins)
	{
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
                int offsetLeft = Math.abs(aOffsetLeft);
                int offsetTop = Math.abs(aOffsetTop);
                Bitmap bitmapCurrent = convertToBitmap(aView.getBackground(), aView.getWidth(), aView.getHeight());
                int bitmapCurrentWidth = bitmapCurrent.getWidth();
                int bitmapCurrentHeight = bitmapCurrent.getHeight();

                Bitmap bitmapAlpha = bitmapCurrent.extractAlpha();
                Bitmap bitmapTemp = null;
                Rect rectSrc = new Rect(0, 0, bitmapCurrentWidth, bitmapCurrentHeight);
                Rect rectDest = null;
                if(aIsExpand) {
                    rectDest = new Rect(offsetLeft, offsetTop, bitmapCurrentWidth + (aShadowSize * 2) + offsetLeft,
                            bitmapCurrentHeight + (aShadowSize * 2) + offsetTop);
                    bitmapTemp = Bitmap.createBitmap(bitmapCurrentWidth + (aShadowSize * 2) + (offsetLeft * 2),
                            bitmapCurrentHeight + (aShadowSize * 2) + (offsetTop * 2), Config.ARGB_8888);
                } else {
                    rectDest = new Rect(offsetLeft, offsetTop, bitmapCurrentWidth + offsetLeft, bitmapCurrentHeight + offsetTop);
                    bitmapTemp = Bitmap.createBitmap(bitmapCurrentWidth + (offsetLeft * 2),
                            bitmapCurrentHeight + (offsetTop * 2), Config.ARGB_8888);
                }
                final Bitmap bitmap = bitmapTemp;

                Paint paint = new Paint();
                paint.setColor(aShadowColor);
                paint.setAlpha(aAlphaInit);
                Canvas canvas = new Canvas(bitmap);

                int add = aShadowSize / ((aShadowLayersCount == 0) ? 1 : aShadowLayersCount);
                for(int x = 0; x < aShadowLayersCount; x++) {
                    canvas.drawBitmap(bitmapAlpha, rectSrc, rectDest, paint);
                    int alpha = paint.getAlpha() + aAlphaStep;
                    if(alpha > 255) {alpha = 255;}
                    paint.setAlpha(alpha);
                    rectDest.set(rectDest.left + add, rectDest.top + add, rectDest.right - add, rectDest.bottom - add);
                }
                if(aIsExpand) {
                    rectDest.set(aShadowSize, aShadowSize, bitmapCurrentWidth + aShadowSize, bitmapCurrentHeight + aShadowSize);
                } else {
                    rectDest.set(aShadowSize, aShadowSize, bitmapCurrentWidth - aShadowSize, bitmapCurrentHeight - aShadowSize);
                }
                canvas.drawBitmap(bitmapCurrent, rectSrc, rectDest, null);

				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Context ctx = mWeakCtx.get();
						if(ctx == null) {return;}

                        aView.setPadding(-aOffsetLeft, -aOffsetTop, aView.getPaddingRight(), aView.getPaddingBottom());
						aView.setBackgroundDrawable(new BitmapDrawable(ctx.getResources(), bitmap));

                        if(aChangeMargins != null) {
                            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) aView.getLayoutParams();
//						    if(aChangeMargins.changeLeft) {params.rightMargin = params.rightMargin - aShadowSize;}
//						    if(aChangeMargins.changeTop) {params.topMargin = params.topMargin - aShadowSize;}
//						    if(aChangeMargins.changeRight) {params.rightMargin = params.rightMargin - aShadowSize;}
//						    if(aChangeMargins.changeBottom) {params.bottomMargin = params.bottomMargin - aShadowSize;}
                            params.setMargins((aChangeMargins.changeLeft) ? params.leftMargin - aShadowSize : params.leftMargin,
                                    (aChangeMargins.changeTop) ? params.topMargin - aShadowSize : params.topMargin,
                                    (aChangeMargins.changeRight) ? params.rightMargin - aShadowSize : params.rightMargin,
                                    (aChangeMargins.changeBottom) ? params.bottomMargin - aShadowSize : params.bottomMargin);
                        }
					}
				});
			}
		});
	}

    /**
     * Drops a shadow around the passed in view using Box blur.
     * @param aView - view which to decorate with a shadow.
     * @param aShadowSize - size of the shadow in pixels.
     * @param aShadowColor - color of the shadow.
     * @return void.
     */
	public void dropShadowBoxBlur(final View aView, final int aShadowSize, final int aShadowColor,
            final int aOffsetLeft, final int aOffsetTop, final boolean aIsExpand)
	{
        final int viewWidth = aView.getWidth();
        final int viewHeight = aView.getHeight();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                float kernel = 1.0f / ((aShadowSize * 2) + 1);
                Bitmap bitmapCurrent = convertToBitmap(aView.getBackground(), viewWidth, viewHeight);
                Bitmap bitmapAlpha = bitmapCurrent.extractAlpha();
                Bitmap bitmapTemp = null;
                int offsetLeft = Math.abs(aOffsetLeft);
                int offsetTop = Math.abs(aOffsetTop);

                Rect rectSrc = new Rect(0, 0, bitmapCurrent.getWidth(), bitmapCurrent.getHeight());
                Rect rectDest = null;
                if(aIsExpand) {
                    rectDest = new Rect(aShadowSize + aOffsetLeft, aShadowSize + aOffsetTop, bitmapCurrent.getWidth() + aShadowSize + aOffsetLeft,
                            bitmapCurrent.getHeight() + aShadowSize + aOffsetTop);
                    bitmapTemp = Bitmap.createBitmap(bitmapCurrent.getWidth() + (aShadowSize * 2) + (offsetLeft * 2),
                            bitmapCurrent.getHeight() + (aShadowSize * 2) + (offsetTop * 2), Config.ARGB_8888);
                } else {
                    rectDest = new Rect(aShadowSize + aOffsetLeft, aShadowSize + aOffsetTop,
                            bitmapCurrent.getWidth() - aShadowSize + aOffsetLeft, bitmapCurrent.getHeight() - aShadowSize + aOffsetTop);
                    bitmapTemp = Bitmap.createBitmap(bitmapCurrent.getWidth() + (offsetLeft * 2),
                            bitmapCurrent.getHeight() + (offsetTop * 2), Config.ARGB_8888);
                }
                final Bitmap bitmap = bitmapTemp;

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
                int colsCount = bitmap.getWidth();
                int rowsCount = bitmap.getHeight();

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

                int addLeft = 0;
                int addTop = 0;
                final int paddingLeft = (aOffsetLeft < 0) ? aShadowSize : -aOffsetLeft * 2;
                final int paddingTop = (aOffsetTop < 0) ? aShadowSize : -aOffsetTop * 2;
                if(aOffsetLeft < 0) {addLeft = offsetLeft;}
                if(aOffsetTop < 0) {addTop = offsetTop;}

                if(aIsExpand) {
                    rectDest.set(aShadowSize + addLeft, aShadowSize + addTop,
                            bitmapCurrent.getWidth() + aShadowSize + addLeft, bitmapCurrent.getHeight() + aShadowSize + addTop);
                } else {
                    rectDest.set(aShadowSize + addLeft, aShadowSize + addTop,
                            bitmapCurrent.getWidth() - aShadowSize + addLeft, bitmapCurrent.getHeight() - aShadowSize + addTop);
                }
                canvas.drawBitmap(bitmapCurrent, rectSrc, rectDest, null);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Context ctx = mWeakCtx.get();
                        if(ctx == null) {return;}

                        aView.setPadding(paddingLeft, paddingTop, aView.getPaddingRight(), aView.getPaddingBottom());
                        aView.setBackgroundDrawable(new BitmapDrawable(ctx.getResources(), bitmap));
                    }
                });
            }
        });
	}

    /**
     * Drops offset shadow using GaussianBlur with Renderscript.
     * Has some issues with negative offset values.
     * @param aView - view which to decorate with a shadow.
     * @param aShadowSize - size of the shadow in pixels.
     * @param aShadowColor - color of the shadow.
     * @param aOffsetLeft - offset from left.
     * @param aOffsetTop - offset from top.
     * @param aIsExpand - if the view should expand with the size of the shadow or if it should keep it's size.
     * @return void.
     */
	public void dropShadowGaussianBlur(final View aView, final int aShadowSize, final int aShadowColor,
            final int aOffsetLeft, final int aOffsetTop, final boolean aIsExpand)
	{
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Context ctx = mWeakCtx.get();
                if(ctx == null) {return;}

                Bitmap bitmapCurrent = convertToBitmap(aView.getBackground(), aView.getWidth(), aView.getHeight());
                int currentWidth = bitmapCurrent.getWidth();
                int currentHeight = bitmapCurrent.getHeight();
                int offsetLeft = Math.abs(aOffsetLeft);
                int offsetTop = Math.abs(aOffsetTop);

                Rect rectSrc = new Rect(0, 0, currentWidth, currentHeight);
                Rect rectDest = null;
                Bitmap bitmapTemp = null;
                Bitmap bitmapAlpha = null;
                if(aIsExpand) {
                    rectDest = new Rect(aShadowSize, aShadowSize, currentWidth + aShadowSize, currentHeight + aShadowSize);
                    bitmapTemp = Bitmap.createBitmap(currentWidth + (aShadowSize * 2) + offsetLeft,
                            currentHeight + (aShadowSize * 2) + offsetTop, Config.ARGB_8888);
                    bitmapAlpha = bitmapCurrent.extractAlpha();
                } else {
                    rectDest = new Rect(aShadowSize, aShadowSize, currentWidth - aShadowSize, currentHeight - aShadowSize);
                    bitmapTemp = Bitmap.createBitmap(currentWidth + offsetLeft, currentHeight + offsetTop, Config.ARGB_8888);
                    bitmapAlpha = Bitmap.createScaledBitmap(bitmapCurrent.extractAlpha(),
                            currentWidth - (aShadowSize * 2) + offsetLeft, currentHeight - (aShadowSize * 2) + offsetTop, false);
                }
                final Bitmap bitmap = bitmapTemp;

                Paint paint = new Paint();
                paint.setColor(aShadowColor);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(bitmapAlpha, aShadowSize + aOffsetLeft, aShadowSize + aOffsetTop, null);
                Bitmap bitmapShadow = gaussianBlur(ctx, bitmap, aShadowSize);
                canvas.drawBitmap(bitmapShadow, 0, 0, paint);
                canvas.drawBitmap(bitmapCurrent, rectSrc, rectDest, null);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Context ctx = mWeakCtx.get();
                        if(ctx == null) {return;}
                        aView.setPadding(-aOffsetLeft, -aOffsetTop, aView.getPaddingRight(), aView.getPaddingBottom());
                        aView.setBackgroundDrawable(new BitmapDrawable(ctx.getResources(), bitmap));
                    }
                });
            }
        });
	}

    /**
     * Drops offset shadow using elevation property of the view if running on Android 5+ or using the ShadowViewDecorator methods if lower.
     * If elevation is available the other parameters will be ignored.
     * This method is not designed to create the same result as if using elevation property. The shadow may be different than if using elevation.
     * @param aView - view which to decorate.
     * @param aOffsetLeft - offset from left.
     * @param aOffsetTop - offset from top.
     * @param aShadowSize - size of the shadow in pixels.
     * @param aShadowLayersCount - count of shadow layers.
     * @param aShadowColor - color of the shadow.
     * @param aAlphaInit - initial alpha value of the shadow. How strong is the shadow.
     * @param aAlphaStep - how much to increase the shadow with each layer. How the shadow fades.
     * @param aIsExpand - if the view should expand with the size of the shadow or if it should keep it's size.
     * @param aChangeMargins - if the decorator should change the margins of the view with the size of the shadow.
     * @param aElevation - elevation if available.
     * @return void.
     */
	public void dropShadowCompat(View aView, int aOffsetLeft, int aOffsetTop, int aShadowSize, int aShadowLayersCount, int aShadowColor,
			int aAlphaInit, int aAlphaStep, boolean aIsExpand, ChangeMargins aChangeMargins, float aElevation)
	{
		int sdkLevel = android.os.Build.VERSION.SDK_INT;
		if(sdkLevel >= kSDK_LEVEL_LOLLIPOP) {
			try {
				Method methodSetElevation = aView.getClass().getMethod(STR_METHOD_setElevation, new Class[]{float.class});
				methodSetElevation.invoke(aView, new Object[]{aElevation});
			} catch (NoSuchMethodException e) {
				dropShadow(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor,
                        aAlphaInit, aAlphaStep, aIsExpand, aChangeMargins);
			} catch (IllegalAccessException e) {
				dropShadow(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor,
                        aAlphaInit, aAlphaStep, aIsExpand, aChangeMargins);
			} catch (IllegalArgumentException e) {
				dropShadow(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor,
                        aAlphaInit, aAlphaStep, aIsExpand, aChangeMargins);
			} catch (InvocationTargetException e) {
				dropShadow(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor,
                        aAlphaInit, aAlphaStep, aIsExpand, aChangeMargins);
			}
		} else {
			dropShadow(aView, aOffsetLeft, aOffsetTop, aShadowSize, aShadowLayersCount, aShadowColor, aAlphaInit, aAlphaStep,
                    aIsExpand, aChangeMargins);
		}
	}

    /**
     * Drops shadow using BoxBlur or with elevation property if running on Android 5+.
     * If elevation is available the other parameters will be ignored.
     * @param aView - view which to decorate.
     * @param aShadowSize - shadow size in pixels.
     * @param aShadowColor - shadow color.
     * @param aElevation - elevation if available.
     * @return void.
     */
	public void dropShadowBoxBlurCompat(View aView, int aShadowSize, int aShadowColor, float aElevation,
            int aOffsetLeft, int aOffsetTop, boolean aIsExpand)
	{
		int sdkLevel = android.os.Build.VERSION.SDK_INT;
		if(sdkLevel >= kSDK_LEVEL_LOLLIPOP) {
			try {
				Method methodSetElevation = aView.getClass().getMethod(STR_METHOD_setElevation, new Class[]{float.class});
				methodSetElevation.invoke(aView, new Object[]{aElevation});
			} catch (NoSuchMethodException e) {
				dropShadowBoxBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
			} catch (IllegalAccessException e) {
				dropShadowBoxBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
			} catch (IllegalArgumentException e) {
				dropShadowBoxBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
			} catch (InvocationTargetException e) {
				dropShadowBoxBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
			}
		} else {
			dropShadowBoxBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
		}
	}

    /**
     * Drops offset shadow using GaussianBlur or elevation property of the view if running on Android 5+.
     * If elevation is available the other parameters will be ignored.
     * @param aView - view which to decorate.
     * @param aShadowSize - shadow size in pixels.
     * @param aShadowColor - shadow color.
     * @param aOffsetLeft - offset from left.
     * @param aOffsetTop - offset from top.
     * @param aElevation - elevation if available.
     * @param aIsExpand - if the view should expand with the size of the shadow or if it should keep it's size.
     * @return void.
     */
	public void dropShadowGaussianBlurCompat(View aView, int aShadowSize, int aShadowColor,
			int aOffsetLeft, int aOffsetTop, float aElevation, boolean aIsExpand)
	{
		int sdkLevel = android.os.Build.VERSION.SDK_INT;
		if(sdkLevel >= kSDK_LEVEL_LOLLIPOP) {
			try {
				Method methodSetElevation = aView.getClass().getMethod(STR_METHOD_setElevation, new Class[]{float.class});
				methodSetElevation.invoke(aView, new Object[]{aElevation});
			} catch (NoSuchMethodException e) {
				dropShadowGaussianBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
			} catch (IllegalAccessException e) {
				dropShadowGaussianBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
			} catch (IllegalArgumentException e) {
				dropShadowGaussianBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
			} catch (InvocationTargetException e) {
				dropShadowGaussianBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
			}
		} else {
			dropShadowGaussianBlur(aView, aShadowSize, aShadowColor, aOffsetLeft, aOffsetTop, aIsExpand);
		}
	}

    /**
     * Creates an offset shadow with the shape of the passed in bitmap.
     * @param aBitmap - bitmap from which to extract shape and size for the shadow.
     * @param aOffsetLeft - offset from left.
     * @param aOffsetTop - offset from top.
     * @param aShadowSize - size of the shadow in pixels.
     * @param aShadowLayersCount - how many layers the shadow will have.
     * @param aShadowColor - color of the shadow.
     * @param aAlphaInit - initial alpha value of the shadow. How strong the shadow will be.
     * @param aAlphaStep - alpha value that will be added on each layer. How the shadow fades.
     * @param aIsExpand - if the bitmap size should be expanded with the size of the shadow or not.
     * @return Bitmap - the shadow as bitmap.
     */
	public Bitmap createShadow(Bitmap aBitmap, int aOffsetLeft, int aOffsetTop, int aShadowSize, int aShadowLayersCount,
				int aShadowColor, int aAlphaInit, int aAlphaStep, boolean aIsExpand)
	{
		Bitmap bitmapCurrent = aBitmap;
		int bitmapCurrentWidth = bitmapCurrent.getWidth();
		int bitmapCurrentHeight = bitmapCurrent.getHeight();
        int offsetLeft = Math.abs(aOffsetLeft);
        int offsetTop = Math.abs(aOffsetTop);

		Bitmap bitmapAlpha = bitmapCurrent.extractAlpha();
		Bitmap bitmap = null;
		Rect rectSrc = new Rect(0, 0, bitmapCurrentWidth, bitmapCurrentHeight);
		Rect rectDest = null;
        if(aIsExpand) {
            rectDest = new Rect(aOffsetLeft, aOffsetTop, bitmapCurrentWidth + (aShadowSize * 2), bitmapCurrentHeight + (aShadowSize * 2));
            bitmap = Bitmap.createBitmap(bitmapCurrentWidth + (aShadowSize * 2) + offsetLeft,
                    bitmapCurrentHeight + (aShadowSize * 2) + offsetTop, Config.ARGB_8888);
        } else {
            rectDest = new Rect(aOffsetLeft, aOffsetTop, bitmapCurrentWidth + offsetLeft,
                    bitmapCurrentHeight + offsetTop);
            bitmap = Bitmap.createBitmap(bitmapCurrentWidth + offsetLeft,
                    bitmapCurrentHeight + offsetTop, Config.ARGB_8888);
        }

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

    /**
     * Creates an offset shadow with the shape of the passed in bitmap.
     * @param aBitmap - bitmap from which to extract shape and size for the shadow.
     * @param aOffsetLeft - offset from left.
     * @param aOffsetTop - offset from top.
     * @param aShadowSize - size of the shadow in pixels.
     * @param aShadowColor - color of the shadow.
     * @param aIsExpand - if the bitmap size should be expanded with the size of the shadow or not.
     * @return Bitmap - the shadow as bitmap.
     */
    public Bitmap createShadowGaussianBlur(Bitmap aBitmap, int aOffsetLeft, int aOffsetTop, int aShadowSize,
            int aShadowColor, boolean aIsExpand)
    {
        Context ctx = mWeakCtx.get();
        Bitmap bitmapCurrent = aBitmap;
        int currentWidth = bitmapCurrent.getWidth();
        int currentHeight = bitmapCurrent.getHeight();
        int offsetLeft = Math.abs(aOffsetLeft);
        int offsetTop = Math.abs(aOffsetTop);

        Rect rectSrc = new Rect(0, 0, currentWidth, currentHeight);
        Rect rectDest = null;
        Bitmap bitmap = null;
        Bitmap bitmapAlpha = null;
        if(aIsExpand) {
            rectDest = new Rect(aShadowSize, aShadowSize, currentWidth + aShadowSize, currentHeight + aShadowSize);
            bitmap = Bitmap.createBitmap(currentWidth + (aShadowSize * 2) + offsetLeft,
                    currentHeight + (aShadowSize * 2) + offsetTop, Config.ARGB_8888);
            bitmapAlpha = bitmapCurrent.extractAlpha();
        } else {
            rectDest = new Rect(aShadowSize, aShadowSize, currentWidth - aShadowSize, currentHeight - aShadowSize);
            bitmap = Bitmap.createBitmap(currentWidth + offsetLeft, currentHeight + offsetTop, Config.ARGB_8888);
            bitmapAlpha = Bitmap.createScaledBitmap(bitmapCurrent.extractAlpha(),
                    currentWidth - (aShadowSize * 2) + offsetLeft, currentHeight - (aShadowSize * 2) + offsetTop, false);
        }

        Paint paint = new Paint();
        paint.setColor(aShadowColor);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmapAlpha, aShadowSize + aOffsetLeft, aShadowSize + aOffsetTop, null);
        Bitmap bitmapShadow = gaussianBlur(ctx, bitmap, aShadowSize);
        canvas.drawBitmap(bitmapShadow, 0, 0, paint);
        canvas.drawBitmap(bitmapCurrent, rectSrc, rectDest, null);

        return bitmap;
    }

    /**
     * Creates a shadow with the shape of the passed in bitmap on selected sides of the view.
     * @param aBitmap - bitmap from which to extract shape and size.
     * @param aShadowSize - size of the shadow in pixels.
     * @param aShadowLayersCount - layers of the shadow.
     * @param aShadowColor - shadow color.
     * @param aAlphaInit - initial alpha value of the shadow. How strong the shadow will be.
     * @param aAlphaStep - alpha value that will be added on each layer. How the shadow fades.
     * @param aShadowLeft - drop shadow on left side.
     * @param aShadowRight - drop shadow on right side.
     * @param aShadowTop - drop shadow on top side.
     * @param aShadowBottom - drop shadow on bottom side.
     * @return Bitmap - the passed in bitmap decorated with shadow.
     */
	public Bitmap createShadow(Bitmap aBitmap, int aShadowSize, int aShadowLayersCount, int aShadowColor, int aAlphaInit, int aAlphaStep,
			boolean aShadowLeft, boolean aShadowRight, boolean aShadowTop, boolean aShadowBottom)
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
				rectDest.set(aShadowLeft ? (rectDest.left + add) : rectDest.left, aShadowTop ? (rectDest.top + add) : rectDest.top,
						aShadowRight ? (rectDest.right - add) : rectDest.right, aShadowBottom ? (rectDest.bottom - add) : rectDest.bottom);
			}

			rectDest.set(aShadowLeft ? rectDest.left : 0, aShadowTop ? rectDest.top : 0,
					aShadowRight ? rectDest.right : bitmapCurrentWidth, aShadowBottom ? rectDest.bottom : bitmapCurrentHeight);
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

		RenderScript rs = RenderScript.create(aCtx);
		final Allocation input = Allocation.createFromBitmap(rs, bitmapRet);
		final Allocation output = Allocation.createTyped(rs, input.getType());
		final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
		script.setRadius(aSize);
		script.setInput(input);
		script.forEach(output);
		output.copyTo(bitmapRet);

		return bitmapRet;
	}
}
