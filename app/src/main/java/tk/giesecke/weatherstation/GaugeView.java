/*******************************************************************************
 * Copyright (c) 2012 Evelina Vrabie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package tk.giesecke.weatherstation;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

/**
 * AutoStart
 * <p/>
 * Broadcast receiver for boot completed
 *
 * @author Evelina Vrabie -- patched by Bernd Giesecke
 * @version 1.0 May 31, 2015.
 */
public class GaugeView extends View {

	/** Default size of gauge */
	private static final int SIZE = 300;
	/** Default top coordinate of gauge */
	private static final float TOP = 0.0f;
	/** Default left coordinate of gauge */
	private static final float LEFT = 0.0f;
	/** Default right coordinate of gauge */
	private static final float RIGHT = 1.0f;
	/** Default bottom coordinate of gauge */
	private static final float BOTTOM = 1.0f;
	/** Default center coordinate of gauge */
	private static final float CENTER = 0.5f;
	/** Flag for outer shadow visibility */
	private static final boolean SHOW_OUTER_SHADOW = true;
	/** Flag for outer border visibility */
	private static final boolean SHOW_OUTER_BORDER = true;
	/** Flag for outer rim visibility */
	private static final boolean SHOW_OUTER_RIM = true;
	/** Flag for inner shadow visibility */
	private static final boolean SHOW_INNER_RIM = true;
	/** Flag for needle visibility */
	private static final boolean SHOW_NEEDLE = true;
	/** Flag for scale visibility */
	private static final boolean SHOW_SCALE = false;
	/** Flag for ranges visibility */
	private static final boolean SHOW_RANGES = true;

	/** Default outer shadow width */
	private static final float OUTER_SHADOW_WIDTH = 0.03f;
	/** Default outer border width */
	private static final float OUTER_BORDER_WIDTH = 0.04f;
	/** Default outer rim width */
	private static final float OUTER_RIM_WIDTH = 0.04f;
	/** Default inner rim width */
	private static final float INNER_RIM_WIDTH = 0.05f;
	/** Default inner rim border width */
	private static final float INNER_RIM_BORDER_WIDTH = 0.005f;

	/** Default needle width */
	private static final float NEEDLE_WIDTH = 0.035f;
	/** Default needle length */
	private static final float NEEDLE_HEIGHT = 0.28f;

	/** Default scale start value */
	private static final float SCALE_START_VALUE = 0.0f;
	/** Default scale end value */
	private static final float SCALE_END_VALUE = 100.0f;
	/** Default scale start angle */
	private static final float SCALE_START_ANGLE = 30.0f;
	/** Default scale number of divisions */
	private static final int SCALE_DIVISIONS = 10;
	/** Default scale number of sub divisions */
	private static final int SCALE_SUBDIVISIONS = 5;

	/** Default outer shadow colors */
	private static final int[] OUTER_SHADOW_COLORS = {Color.argb(40, 255, 254, 187), Color.argb(20, 255, 247, 219),
			Color.argb(5, 255, 255, 255)};
	/** Default outer shadow position */
	private static final float[] OUTER_SHADOW_POS = {0.90f, 0.95f, 0.99f};

	/** Default face bitmap */
	private static final int FACE_IMAGE_ID = R.drawable.thermometer;

	// *--------------------------------------------------------------------- *//
	// Customizable properties
	// *--------------------------------------------------------------------- *//

	/** Flag for outer shadow visibility */
	private boolean mShowOuterShadow;
	/** Flag for outer border visibility */
	private boolean mShowOuterBorder;
	/** Flag for outer rim visibility */
	private boolean mShowOuterRim;
	/** Flag for inner rim visibility */
	private boolean mShowInnerRim;
	/** Flag for scale visibility */
	private boolean mShowScale;
	/** Flag for ranges visibility */
	private boolean mShowRanges;
	/** Flag for needle visibility */
	private boolean mShowNeedle;

	/** Outer shadow width */
	private float mOuterShadowWidth;
	/** Outer border width */
	private float mOuterBorderWidth;
	/** Outer rim width */
	private float mOuterRimWidth;
	/** Inner rim width */
	private float mInnerRimWidth;
	/** Inner rim border width */
	private float mInnerRimBorderWidth;
	/** Needle width */
	private float mNeedleWidth;
	/** Needle length */
	private float mNeedleHeight;

	/** Scale start value */
	private float mScaleStartValue;
	/** Scale end value */
	private float mScaleEndValue;
	/** Scale start angle */
	private float mScaleStartAngle;
	/** Scale end angle */
	private float mScaleEndAngle;

	/** Scale number of divisions */
	private int mDivisions;
	/** Scale number of sub divisions */
	private int mSubdivisions;

	/** Rectangle for outer shadow */
	private RectF mOuterShadowRect;
	/** Rectangle for outer border */
	private RectF mOuterBorderRect;
	/** Rectangle for outer rim */
	private RectF mOuterRimRect;
	/** Rectangle for inner rim */
	private RectF mInnerRimRect;
	/** Rectangle for inner rim border */
	private RectF mInnerRimBorderRect;
	/** Rectangle for face */
	private RectF mFaceRect;

	/** Bitmap for background */
	private Bitmap mBackground;
	/** Paint for background */
	private Paint mBackgroundPaint;
	/** Paint for outer shadow */
	private Paint mOuterShadowPaint;
	/** Paint for outer border */
	private Paint mOuterBorderPaint;
	/** Paint for outer rim */
	private Paint mOuterRimPaint;
	/** Paint for inner rim */
	private Paint mInnerRimPaint;
	/** Light paint for inner rim border */
	private Paint mInnerRimBorderLightPaint;
	/** Dark paint for inner rim border */
	private Paint mInnerRimBorderDarkPaint;
	/** Paint for face */
	private Paint mFacePaint;
	/** Paint for background */
	private Paint mNeedleRightPaint;
	/** Paint for needle left side */
	private Paint mNeedleLeftPaint;
	/** Paint for needle right side */
	private Paint mNeedleScrewPaint;
	/** Paint for needle center screw border */
	private Paint mNeedleScrewBorderPaint;

	/** Path for needle right side */
	private Path mNeedleRightPath;
	/** Path for needle left side */
	private Path mNeedleLeftPath;

	// *--------------------------------------------------------------------- *//

	// *--------------------------------------------------------------------- *//
	// BeeGee additional properties
	// *--------------------------------------------------------------------- *//

	/** Drawable id of face bitmap */
	private int faceImageID;

	// *--------------------------------------------------------------------- *//

	/** Float value of scale rotation */
	private float mScaleRotation;
	/** Float value of subdivision value */
	private float mSubdivisionValue;
	/** Float value of subdivision angle */
	private float mSubdivisionAngle;

	/** Float value of target value */
	private float mTargetValue;
	/** Float value of current value */
	private float mCurrentValue;

	/** Float value of needle movement speed */
	private float mNeedleVelocity;
	/** Float value of needle movement acceleration */
	private float mNeedleAcceleration;
	/** Long value of needles last move */
	private long mNeedleLastMoved = -1;
	/** Flag for needle initialization */
	private boolean mNeedleInitialized;

	public GaugeView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		readAttrs(context, attrs, defStyle);
		init();
	}

	public GaugeView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public GaugeView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * Get attributes from XML layout file
	 *
	 * @param context
	 *            Application context.
	 * @param attrs
	 *            Set of attributes.
	 * @param defStyle
	 *            Default style attributes.
	 */
	private void readAttrs(final Context context, final AttributeSet attrs, final int defStyle) {
		/** Array holding the default style attributes */
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, defStyle, 0);
		mShowOuterShadow = a.getBoolean(R.styleable.GaugeView_showOuterShadow, SHOW_OUTER_SHADOW);
		mShowOuterBorder = a.getBoolean(R.styleable.GaugeView_showOuterBorder, SHOW_OUTER_BORDER);
		mShowOuterRim = a.getBoolean(R.styleable.GaugeView_showOuterRim, SHOW_OUTER_RIM);
		mShowInnerRim = a.getBoolean(R.styleable.GaugeView_showInnerRim, SHOW_INNER_RIM);
		mShowNeedle = a.getBoolean(R.styleable.GaugeView_showNeedle, SHOW_NEEDLE);
		mShowScale = a.getBoolean(R.styleable.GaugeView_showScale, SHOW_SCALE);
		mShowRanges = a.getBoolean(R.styleable.GaugeView_showRanges, SHOW_RANGES);

		mOuterShadowWidth = mShowOuterShadow ? a.getFloat(R.styleable.GaugeView_outerShadowWidth, OUTER_SHADOW_WIDTH) : 0.0f;
		mOuterBorderWidth = mShowOuterBorder ? a.getFloat(R.styleable.GaugeView_outerBorderWidth, OUTER_BORDER_WIDTH) : 0.0f;
		mOuterRimWidth = mShowOuterRim ? a.getFloat(R.styleable.GaugeView_outerRimWidth, OUTER_RIM_WIDTH) : 0.0f;
		mInnerRimWidth = mShowInnerRim ? a.getFloat(R.styleable.GaugeView_innerRimWidth, INNER_RIM_WIDTH) : 0.0f;
		mInnerRimBorderWidth = mShowInnerRim ? a.getFloat(R.styleable.GaugeView_innerRimBorderWidth, INNER_RIM_BORDER_WIDTH) : 0.0f;

		mNeedleWidth = a.getFloat(R.styleable.GaugeView_needleWidth, NEEDLE_WIDTH);
		mNeedleHeight = a.getFloat(R.styleable.GaugeView_needleHeight, NEEDLE_HEIGHT);

		mScaleStartValue = a.getFloat(R.styleable.GaugeView_scaleStartValue, SCALE_START_VALUE);
		mScaleEndValue = a.getFloat(R.styleable.GaugeView_scaleEndValue, SCALE_END_VALUE);
		mScaleStartAngle = a.getFloat(R.styleable.GaugeView_scaleStartAngle, SCALE_START_ANGLE);
		mScaleEndAngle = a.getFloat(R.styleable.GaugeView_scaleEndAngle, 360.0f - mScaleStartAngle);

		mDivisions = a.getInteger(R.styleable.GaugeView_divisions, SCALE_DIVISIONS);
		mSubdivisions = a.getInteger(R.styleable.GaugeView_subdivisions, SCALE_SUBDIVISIONS);

		faceImageID = a.getResourceId(R.styleable.GaugeView_faceImageID, FACE_IMAGE_ID);

		a.recycle();
	}

	/**
	 * Initialize gauge
	 */
	@TargetApi(11)
	private void init() {

		setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		initDrawingRects();
		initDrawingTools();

		// Patch @BeeGee - show scale even if ranges are not shown
		// Compute the scale properties
		//if (mShowRanges) {
			initScale();
		//}
	}

	/**
	 * Initialize drawing rectangles
	 */
	private void initDrawingRects() {
		// The drawing area is a rectangle of width 1 and height 1,
		// where (0,0) is the top left corner of the canvas.
		// Note that on Canvas X axis points to right, while the Y axis points downwards.
		mOuterShadowRect = new RectF(LEFT, TOP, RIGHT, BOTTOM);

		mOuterBorderRect = new RectF(mOuterShadowRect.left + mOuterShadowWidth, mOuterShadowRect.top + mOuterShadowWidth,
				mOuterShadowRect.right - mOuterShadowWidth, mOuterShadowRect.bottom - mOuterShadowWidth);

		mOuterRimRect = new RectF(mOuterBorderRect.left + mOuterBorderWidth, mOuterBorderRect.top + mOuterBorderWidth,
				mOuterBorderRect.right - mOuterBorderWidth, mOuterBorderRect.bottom - mOuterBorderWidth);

		mInnerRimRect = new RectF(mOuterRimRect.left + mOuterRimWidth, mOuterRimRect.top + mOuterRimWidth, mOuterRimRect.right
				- mOuterRimWidth, mOuterRimRect.bottom - mOuterRimWidth);

		mInnerRimBorderRect = new RectF(mInnerRimRect.left + mInnerRimBorderWidth, mInnerRimRect.top + mInnerRimBorderWidth,
				mInnerRimRect.right - mInnerRimBorderWidth, mInnerRimRect.bottom - mInnerRimBorderWidth);

		mFaceRect = new RectF(mInnerRimRect.left + mInnerRimWidth, mInnerRimRect.top + mInnerRimWidth,
				mInnerRimRect.right - mInnerRimWidth, mInnerRimRect.bottom - mInnerRimWidth);

	}

	/**
	 * Initialize drawing tools
	 */
	private void initDrawingTools() {
		mBackgroundPaint = new Paint();
		mBackgroundPaint.setFilterBitmap(true);

		if (mShowOuterShadow) {
			mOuterShadowPaint = getDefaultOuterShadowPaint();
		}
		if (mShowOuterBorder) {
			mOuterBorderPaint = getDefaultOuterBorderPaint();
		}
		if (mShowOuterRim) {
			mOuterRimPaint = getDefaultOuterRimPaint();
		}
		if (mShowInnerRim) {
			mInnerRimPaint = getDefaultInnerRimPaint();
			mInnerRimBorderLightPaint = getDefaultInnerRimBorderLightPaint();
			mInnerRimBorderDarkPaint = getDefaultInnerRimBorderDarkPaint();
		}
		if (mShowNeedle) {
			setDefaultNeedlePaths();
			mNeedleLeftPaint = getDefaultNeedleLeftPaint();
			mNeedleRightPaint = getDefaultNeedleRightPaint();
			mNeedleScrewPaint = getDefaultNeedleScrewPaint();
			mNeedleScrewBorderPaint = getDefaultNeedleScrewBorderPaint();
		}
		mFacePaint = getDefaultFacePaint();
	}

	/**
	 * Set default outer shadow paint
	 *
	 * @return paint
	 *          Paint for outer shadow
	 */
	private Paint getDefaultOuterShadowPaint() {
		/** Paint for outer shadow */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL);
		paint.setShader(new RadialGradient(CENTER, CENTER, mOuterShadowRect.width() / 2.0f, OUTER_SHADOW_COLORS, OUTER_SHADOW_POS,
				TileMode.MIRROR));
		return paint;
	}

	/**
	 * Set default outer border paint
	 *
	 * @return paint
	 *          Paint for outer border
	 */
	private Paint getDefaultOuterBorderPaint() {
		/** Paint for outer border */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.argb(245, 0, 0, 0));
		return paint;
	}

	/**
	 * Set default outer rim paint
	 *
	 * @return paint
	 *          Paint for outer rim
	 */
	private Paint getDefaultOuterRimPaint() {
		/** Linear gradient to create the 3D effect */
		final LinearGradient verticalGradient = new LinearGradient(mOuterRimRect.left, mOuterRimRect.top, mOuterRimRect.left,
				mOuterRimRect.bottom, Color.rgb(255, 255, 255), Color.rgb(84, 90, 100), TileMode.REPEAT);

		/** Bitmap for outer rim */
		final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.darkwood);
		/** Bitmap shader for the metallic style */
		final BitmapShader outerRimTile = new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
		/** Matrix for outer rim */
		final Matrix matrix = new Matrix();
		matrix.setScale(1.0f / bitmap.getWidth(), 1.0f / bitmap.getHeight());
		outerRimTile.setLocalMatrix(matrix);

		/** Paint for outer rim */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setShader(new ComposeShader(verticalGradient, outerRimTile, PorterDuff.Mode.MULTIPLY));
		paint.setFilterBitmap(true);
		return paint;
	}

	/**
	 * Set default inner rim paint
	 *
	 * @return paint
	 *          Paint for inner rim
	 */
	private Paint getDefaultInnerRimPaint() {
		/** Linear gradient to create the 3D effect */
		final LinearGradient verticalGradient = new LinearGradient(mOuterRimRect.left, mOuterRimRect.top, mOuterRimRect.left,
				mOuterRimRect.bottom, Color.rgb(255, 255, 255), Color.rgb(84, 90, 100), TileMode.REPEAT);

		/** Bitmap for inner rim */
		final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.darkerwood);
		/** Bitmap shader for the metallic style */
		final BitmapShader innerRimTile = new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
		/** Matrix for inner rim */
		final Matrix matrix = new Matrix();
		matrix.setScale(1.0f / bitmap.getWidth(), 1.0f / bitmap.getHeight());
		innerRimTile.setLocalMatrix(matrix);

		/** Paint for outer rim */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setShader(new ComposeShader(verticalGradient, innerRimTile, PorterDuff.Mode.MULTIPLY));
		paint.setFilterBitmap(true);
		return paint;
	}

	/**
	 * Set default inner rim light border paint
	 *
	 * @return paint
	 *          Paint for inner rim light border
	 */
	private Paint getDefaultInnerRimBorderLightPaint() {
		/** Paint for inner rim light border */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.argb(100, 255, 255, 255));
		paint.setColor(getResources().getColor(R.color.my_gold));
		paint.setStrokeWidth(0.005f);
		return paint;
	}

	/**
	 * Set default inner rim dark border paint
	 *
	 * @return paint
	 *          Paint for inner rim dark border
	 */
	private Paint getDefaultInnerRimBorderDarkPaint() {
		/** Paint for inner rim dark border */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.argb(100, 81, 84, 89));
		paint.setColor(getResources().getColor(R.color.my_gold_brown));
		paint.setStrokeWidth(0.005f);
		return paint;
	}

	/**
	 * Set default face paint
	 *
	 * @return paint
	 *          Paint for face
	 */
	private Paint getDefaultFacePaint() {
		/** Paint for face */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		/** Bitmap for face */
		Bitmap faceTexture = BitmapFactory.decodeResource(getContext().getResources(), faceImageID);
		/** Shader for face bitmap */
		BitmapShader paperShader = new BitmapShader(faceTexture,
				Shader.TileMode.CLAMP,
				Shader.TileMode.CLAMP);
		/** Matrix for face */
		Matrix paperMatrix = new Matrix();
		paperMatrix.setScale(1.0f / faceTexture.getWidth(),
				1.0f / faceTexture.getHeight());

		paperShader.setLocalMatrix(paperMatrix);
		paint.setShader(paperShader);
		return paint;

	}

	/**
	 * Set default path for needle
	 */
	private void setDefaultNeedlePaths() {
		/** Float for center position of needle */
		final float x = 0.5f, y = 0.5f;
		mNeedleLeftPath = new Path();
		mNeedleLeftPath.moveTo(x, y);
		mNeedleLeftPath.lineTo(x - mNeedleWidth, y);
		mNeedleLeftPath.lineTo(x, y - mNeedleHeight);
		mNeedleLeftPath.lineTo(x, y);
		mNeedleLeftPath.lineTo(x - mNeedleWidth, y);

		mNeedleRightPath = new Path();
		mNeedleRightPath.moveTo(x, y);
		mNeedleRightPath.lineTo(x + mNeedleWidth, y);
		mNeedleRightPath.lineTo(x, y - mNeedleHeight);
		mNeedleRightPath.lineTo(x, y);
		mNeedleRightPath.lineTo(x + mNeedleWidth, y);
	}

	/**
	 * Set default needle left side paint
	 *
	 * @return paint
	 *          Paint for needle left side
	 */
	private Paint getDefaultNeedleLeftPaint() {
		/** Paint for needle left side */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//paint.setColor(Color.rgb(176, 10, 19));
		// Patch BeeGee - needle color is gold
		paint.setColor(getResources().getColor(R.color.my_gold));
		return paint;
	}

	/**
	 * Set default needle right side paint
	 *
	 * @return paint
	 *          Paint for meedle right side
	 */
	private Paint getDefaultNeedleRightPaint() {
		/** Paint for needle right side */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//paint.setColor(Color.rgb(252, 18, 30));
		// Patch BeeGee - needle color is gold-brown
		paint.setColor(getResources().getColor(R.color.my_gold_brown));
		return paint;
	}

	/**
	 * Set default needle screw paint
	 *
	 * @return paint
	 *          Paint for needle screw
	 */
	private Paint getDefaultNeedleScrewPaint() {
		/** Paint for needle screw */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//paint.setShader(new RadialGradient(0.5f, 0.5f, 0.07f,
		//		new int[]{Color.rgb(171, 171, 171), Color.WHITE}, new float[]{0.05f,
		//		0.9f}, TileMode.MIRROR));
		// Patch BeeGee - needle color is gold-brown -> gold
		paint.setShader(new RadialGradient(0.5f, 0.5f, 0.07f,
				new int[]{getResources().getColor(R.color.my_gold_brown),
						getResources().getColor(R.color.my_gold)}, new float[]{0.05f,
				0.9f}, TileMode.MIRROR));
		return paint;
	}

	/**
	 * Set default needle screw border paint
	 *
	 * @return paint
	 *          Paint for needle screw
	 */
	private Paint getDefaultNeedleScrewBorderPaint() {
		/** Paint for needle screw */
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.STROKE);
		//paint.setColor(Color.argb(100, 81, 84, 89));
		// Patch BeeGee - needle color is gold-brown
		paint.setColor(getResources().getColor(R.color.my_gold_brown));
		paint.setStrokeWidth(0.005f);
		return paint;
	}

	@Override
	protected void onRestoreInstanceState(final Parcelable state) {
		final Bundle bundle = (Bundle) state;
		final Parcelable superState = bundle.getParcelable("superState");
		super.onRestoreInstanceState(superState);

		mNeedleInitialized = bundle.getBoolean("needleInitialized");
		mNeedleVelocity = bundle.getFloat("needleVelocity");
		mNeedleAcceleration = bundle.getFloat("needleAcceleration");
		mNeedleLastMoved = bundle.getLong("needleLastMoved");
		mCurrentValue = bundle.getFloat("currentValue");
		mTargetValue = bundle.getFloat("targetValue");
	}

	/**
	 * Initialize the scale
	 */
	private void initScale() {
		mScaleRotation = (mScaleStartAngle + 180) % 360;
		/** Float value for the main divisions */
		float mDivisionValue = (mScaleEndValue - mScaleStartValue) / mDivisions;
		mSubdivisionValue = mDivisionValue / mSubdivisions;
		mSubdivisionAngle = (mScaleEndAngle - mScaleStartAngle) / (mDivisions * mSubdivisions);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		/** Parcable containing state */
		final Parcelable superState = super.onSaveInstanceState();

		/** Bundle containing saved state */
		final Bundle state = new Bundle();
		state.putParcelable("superState", superState);
		state.putBoolean("needleInitialized", mNeedleInitialized);
		state.putFloat("needleVelocity", mNeedleVelocity);
		state.putFloat("needleAcceleration", mNeedleAcceleration);
		state.putLong("needleLastMoved", mNeedleLastMoved);
		state.putFloat("currentValue", mCurrentValue);
		state.putFloat("targetValue", mTargetValue);
		return state;
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		// Loggable.log.debug(String.format("widthMeasureSpec=%s, heightMeasureSpec=%s",
		// View.MeasureSpec.toString(widthMeasureSpec),
		// View.MeasureSpec.toString(heightMeasureSpec)));

		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		final int chosenWidth = chooseDimension(widthMode, widthSize);
		final int chosenHeight = chooseDimension(heightMode, heightSize);
		setMeasuredDimension(chosenWidth, chosenHeight);
	}

	/**
	 * Set measurement specs
	 *
	 * @param mode
	 *          measurement mode
	 * @param size
	 *          width or height size
	 *
	 * @return size
	 *          requested size of width or height
	 */
	private int chooseDimension(final int mode, final int size) {
		switch (mode) {
			case View.MeasureSpec.AT_MOST:
			case View.MeasureSpec.EXACTLY:
				return size;
			case View.MeasureSpec.UNSPECIFIED:
			default:
				return SIZE;
		}
	}

	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
		drawGauge();
	}

	/**
	 * Draw the gauge
	 */
	private void drawGauge() {
		if (null != mBackground) {
			// Let go of the old background
			mBackground.recycle();
		}
		// Create a new background according to the new width and height
		mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		/** Canvas for the background */
		final Canvas canvas = new Canvas(mBackground);
		/** Scale for drawing */
		final float scale = Math.min(getWidth(), getHeight());
		canvas.scale(scale, scale);
		canvas.translate((scale == getHeight()) ? ((getWidth() - scale) / 2) / scale : 0
				, (scale == getWidth()) ? ((getHeight() - scale) / 2) / scale : 0);

		drawRim(canvas);
		drawFace(canvas);

		// Patch BeeGee - never draw the ranges nor the scale. We use a bitmap instead
		//if (mShowRanges) {
			//drawScale(canvas);
		//}
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		drawBackground(canvas);

		/** Scale for drawing */
		final float scale = Math.min(getWidth(), getHeight());
		canvas.scale(scale, scale);
		canvas.translate((scale == getHeight()) ? ((getWidth() - scale) / 2) / scale : 0
				, (scale == getWidth()) ? ((getHeight() - scale) / 2) / scale : 0);

		if (mShowNeedle) {
			drawNeedle(canvas);
		}

		// Patch BeeGee - never draw the extra text.
/*		if (mShowText) {
			drawText(canvas);
		}
*/
		computeCurrentValue();
	}

	/**
	 * Draw the background
	 *
	 * @param canvas
	 *          canvas to draw the background
	 */
	private void drawBackground(final Canvas canvas) {
		if (null != mBackground) {
			canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
		}
	}

	/**
	 * Draw the inner and outer rim and the outer shadow
	 *
	 * @param canvas
	 *          canvas to draw the inner and outer rim and the outer shadow
	 */
	private void drawRim(final Canvas canvas) {
		if (mShowOuterShadow) {
			canvas.drawOval(mOuterShadowRect, mOuterShadowPaint);
		}
		if (mShowOuterBorder) {
			canvas.drawOval(mOuterBorderRect, mOuterBorderPaint);
		}
		if (mShowOuterRim) {
			canvas.drawOval(mOuterRimRect, mOuterRimPaint);
		}
		if (mShowInnerRim) {
			canvas.drawOval(mInnerRimRect, mInnerRimPaint);
			canvas.drawOval(mInnerRimRect, mInnerRimBorderLightPaint);
			canvas.drawOval(mInnerRimBorderRect, mInnerRimBorderDarkPaint);
		}
	}

	/**
	 * Draw the face
	 *
	 * @param canvas
	 *          canvas to draw face
	 */
	private void drawFace(final Canvas canvas) {

		// Draw the face gradient
		canvas.drawOval(mFaceRect, mFacePaint);
		// Patch BeeGee - never draw the face border or shadow
		// Draw the face border
		//canvas.drawOval(mScaleImageRect, mFaceBorderPaint);
		// Draw the inner face shadow
		//canvas.drawOval(mFaceRect, mFaceShadowPaint);
	}

	/**
	 * Draw the needle
	 *
	 * @param canvas
	 *          canvas to draw needle
	 */
	private void drawNeedle(final Canvas canvas) {
		if (mNeedleInitialized) {
			/** Angle for the needle drawing */
			final float angle = getAngleForValue(mCurrentValue);
			// Logger.log.info(String.format("value=%f -> angle=%f", mCurrentValue, angle));

			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate(angle, 0.5f, 0.5f);

			canvas.drawPath(mNeedleLeftPath, mNeedleLeftPaint);
			canvas.drawPath(mNeedleRightPath, mNeedleRightPaint);

			canvas.restore();

			// Draw the needle screw and its border
			canvas.drawCircle(0.5f, 0.5f, 0.04f, mNeedleScrewPaint);
			canvas.drawCircle(0.5f, 0.5f, 0.04f, mNeedleScrewBorderPaint);
		}
	}

	/**
	 * Calculate the angle for the needle drawing
	 */
	private float getAngleForValue(final float value) {
		return (mScaleRotation + ((value - mScaleStartValue) / mSubdivisionValue) * mSubdivisionAngle) % 360;
	}

	/**
	 * Calculate curren value for needle movement and acceleration
	 */
	private void computeCurrentValue() {
		// Logger.log.warn(String.format("velocity=%f, acceleration=%f", mNeedleVelocity,
		// mNeedleAcceleration));

		if (!(Math.abs(mCurrentValue - mTargetValue) > 0.01f)) {
			return;
		}

		if (-1 != mNeedleLastMoved) {
			/** Time for needle movement */
			final float time = (System.currentTimeMillis() - mNeedleLastMoved) / 1000.0f;
			/** Direction for needle movement */
			final float direction = Math.signum(mNeedleVelocity);
			if (Math.abs(mNeedleVelocity) < 90.0f) {
				mNeedleAcceleration = 5.0f * (mTargetValue - mCurrentValue);
			} else {
				mNeedleAcceleration = 0.0f;
			}

			mNeedleAcceleration = 5.0f * (mTargetValue - mCurrentValue);
			mCurrentValue += mNeedleVelocity * time;
			mNeedleVelocity += mNeedleAcceleration * time;

			if ((mTargetValue - mCurrentValue) * direction < 0.01f * direction) {
				mCurrentValue = mTargetValue;
				mNeedleVelocity = 0.0f;
				mNeedleAcceleration = 0.0f;
				mNeedleLastMoved = -1L;
			} else {
				mNeedleLastMoved = System.currentTimeMillis();
			}

			invalidate();

		} else {
			mNeedleLastMoved = System.currentTimeMillis();
			computeCurrentValue();
		}
	}

	/**
	 * Receive the current value for the gauge
	 *
	 * @param value
	 *          value to be shown on the gauge
	 */
	public void setTargetValue(final float value) {
		if (mShowScale || mShowRanges) {
			if (value < mScaleStartValue) {
				mTargetValue = mScaleStartValue;
			} else if (value > mScaleEndValue) {
				mTargetValue = mScaleEndValue;
			} else {
				mTargetValue = value;
			}
		} else {
			mTargetValue = value;
		}
		mNeedleInitialized = true;
		invalidate();
	}
}
