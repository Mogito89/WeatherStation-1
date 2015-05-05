package tk.giesecke.weatherstation;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.TextOrientationType;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.util.PixelUtils;

import com.androidplot.xy.*;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * WeatherStation
 * main activity
 *
 * @author Bernd Giesecke
 * @version 0.1 beta May 5, 2015.
 */
public class WeatherStation extends ActionBarActivity implements
		View.OnClickListener, SensorEventListener, AdapterView.OnItemClickListener {

	/** Debug tag */
	private static final String LOG_TAG = "WeatherStation";
    /** Application context */
    private Context appContext;
	/** Access to shared preferences of application*/
	private SharedPreferences mPrefs;
	/** Pointer to drawer layout */
	private DrawerLayout mDrawerLayout;

	/** User selected theme */
	private int themeColor;
    /** User clicked theme */
    private int selectColor;
	/** User selected temperature unit */
	private int tempUnit;
	/** User clicked temperature unit */
	private int selectTempUnit;
	/** User selected pressure unit */
	private int pressUnit;
	/** User clicked pressure unit */
	private int selectPressUnit;
    /** Radio group column 1 of double column radio group */
    private RadioGroup rgPressUnit1;
	/** Radio group column 2 of double column radio group */
	private RadioGroup rgPressUnit2;
	/** Radio group listener for column 1 of an double column radio group */
	private RadioGroup.OnCheckedChangeListener PressUnit1;
	/** Radio group listener for column 2 of an double column radio group */
	private RadioGroup.OnCheckedChangeListener PressUnit2;

	/** Dark color of selected theme */
	private int colorDark;
    /** Bright color of selected theme */
    private int colorBright;

	/** SensorManager to get info about available sensors */
	private SensorManager mSensorManager;
	/** Access to temp sensor */
	private Sensor mTempSensor;
	/** Access to pressure sensor */
	private Sensor mPressSensor;
	/** Access to humidity sensor */
	private Sensor mHumidSensor;
	/** Textview to show current temperature */
	private TextView tvCurrTempView;
	/** Textview to show current pressure */
	private TextView tvCurrPressView;
	/** Textview to show current humidity */
	private TextView tvCurrHumidView;

	/** Number of plot y values */
    private int plotValues = 20;
	/** Day to show in day view */
    private int dayToShow = 1;
	/** Flag for continuous update of charts */
    private boolean isContinuous = true;
	/** Number of recorded days */
    private int numOfDayRecords;

	/** XYPlot view for the temperature chart */
	private XYPlot tempLevelsPlot;
	/** Data series for the temperature */
	public static SimpleXYSeries tempLevelsSeries = null;
	/** XYPlot view for the barometric pressure chart */
	private XYPlot pressLevelsPlot;
	/** Data series for the barometric pressure */
	public static SimpleXYSeries pressLevelsSeries = null;
	/** XYPlot view for the humidity chart */
	private XYPlot humidLevelsPlot;
	/** Data series for the humidity */
	public static SimpleXYSeries humidLevelsSeries = null;
    /** Min value of temperature series */
    private float minTempValue;
	/** Max value of temperature series */
    private float maxTempValue;
	/** Min value of pressure series */
    private float minPressValue;
	/** Max value of pressure series */
    private float maxPressValue;
	/** Min value of humidity series */
    private float minHumidValue;
	/** Max value of humidity series */
    private float maxHumidValue;

	/** Action bar drawer toggle */
	private ActionBarDrawerToggle mDrawerToggle;
	/** Drawer title when drawer is open */
	private CharSequence mDrawerTitle;
	/** Drawer title when drawer is closed */
	private CharSequence mTitle;

	/** Instance of weather db helper */
    private WSDatabaseHelper wsDbHelper;
	/** Access to weather db  */
    private SQLiteDatabase dataBase;
    /** String with path and filename to location of backup file */
    private String exportFilePath;
    /** String with path and filename to location of restore file */
    private String restoreFilePath;
    /** String with path to read location of JSON file */
    private String path;
    /** Flag for result of file selection dialog */
    private boolean isFileSelected = false;
    /** List with available backup files */
    private final List<String> files = new ArrayList<>();

	/** Small text size as float */
    private float textSizeSmall;
	/** Medium text size as float */
    private float textSizeMedium;
	/** 10dp margin as float */
    private float margin10dp;
	/** 20dp margin as float */
	private float margin20dp;
	/** 25dp margin as float */
	private float margin25dp;
	/** 30dp margin as float */
	private float margin30dp;
	/** 40dp margin as float */
    private float margin40dp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/** Access to shared preferences of application*/
		mPrefs = getSharedPreferences("WeatherStation", 0);
		/** Holds user selected theme */
		themeColor = mPrefs.getInt("dark_theme", 0);
        switch (themeColor) {
            case 1: // green
                setTheme(R.style.AppTheme_Green_Base);
                colorDark = getResources().getColor(android.R.color.holo_green_dark);
                colorBright = getResources().getColor(R.color.my_green_bright);
                break;
            case 2: // grey
                setTheme(R.style.AppTheme_Gray_Base);
                colorDark = getResources().getColor(android.R.color.darker_gray);
                colorBright = getResources().getColor(R.color.my_gray_bright);
                break;
            case 3: // orange
                setTheme(R.style.AppTheme_Orange_Base);
                colorDark = getResources().getColor(android.R.color.holo_orange_dark);
                colorBright = getResources().getColor(R.color.my_orange_bright);
                break;
            case 4: // red
                setTheme(R.style.AppTheme_Red_Base);
                colorDark = getResources().getColor(android.R.color.holo_red_dark);
                colorBright = getResources().getColor(R.color.my_red_bright);
                break;
            case 5: // white
                setTheme(R.style.AppTheme_White_Base);
                colorDark = getResources().getColor(R.color.my_white_dark);
                colorBright = getResources().getColor(android.R.color.white);
                break;
            default: // == 0 == blue
                setTheme(R.style.AppTheme_Base);
                colorDark = getResources().getColor(android.R.color.holo_blue_dark);
                colorBright = getResources().getColor(android.R.color.holo_blue_bright);
                break;
        }

		if (android.os.Build.VERSION.SDK_INT >= 21) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			getWindow().setStatusBarColor(colorDark);
		}

		setContentView(R.layout.weather_station);

		/** Pointer to action bar */
		Toolbar actionBar = (Toolbar) findViewById(R.id.toolbar);
		if (actionBar != null) {
			setSupportActionBar(actionBar);
		}

		mTitle = mDrawerTitle = getTitle();
		/** Pointer to drawer layout */
		mDrawerLayout = (DrawerLayout) findViewById(R.id.main_layout);
		mDrawerLayout.setStatusBarBackgroundColor(colorDark);
		mDrawerToggle = new ActionBarDrawerToggle(
				this,
				mDrawerLayout,
				actionBar,
				R.string.drawer_open,
				R.string.drawer_close
		) {

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getSupportActionBar().setTitle(mDrawerTitle);
			}

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getSupportActionBar().setTitle(mTitle);
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		// Prepare views of weather values
		tvCurrTempView = (TextView) findViewById(R.id.tvCurrTempView);
		tvCurrPressView = (TextView) findViewById(R.id.tvCurrPressView);
		tvCurrHumidView = (TextView) findViewById(R.id.tvCurrHumidView);

		tvCurrTempView.setText(getString(R.string.waitForSensor));
		tvCurrPressView.setText(getString(R.string.waitForSensor));
		tvCurrHumidView.setText(getString(R.string.waitForSensor));

		// connect to temperature sensor
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		// connect to temperature sensor
		mTempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		// connect to air pressure sensor
		mPressSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		// connect to humidity sensor
		mHumidSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
		if (mTempSensor == null) {
			tvCurrTempView.setText(getString(R.string.sensorNotAvail));
			findViewById(R.id.trTemp).setVisibility(View.GONE);
			findViewById(R.id.xyTempPlot).setVisibility(View.GONE);
		}
		if (mPressSensor == null) {
			tvCurrPressView.setText(getString(R.string.sensorNotAvail));
			findViewById(R.id.trPress).setVisibility(View.GONE);
			findViewById(R.id.xyPressPlot).setVisibility(View.GONE);
		}
		if (mHumidSensor == null) {
			tvCurrHumidView.setText(getString(R.string.sensorNotAvail));
			findViewById(R.id.trHumid).setVisibility(View.GONE);
			findViewById(R.id.xyHumidPlot).setVisibility(View.GONE);
		}

		// calculate some sizes from dp/sp to float for AndroidPlot
		//displayMetrics = getResources().getDisplayMetrics();
		//textSizeSmall = displayMetrics.density*14f;
		textSizeSmall = PixelUtils.spToPix(14);
		//textSizeMedium = displayMetrics.density*18f;
		textSizeMedium = PixelUtils.spToPix(18);
		//margin10dp = displayMetrics.density*10f;
		margin10dp = PixelUtils.dpToPix(10);
		//margin20dp = displayMetrics.density*20f;
		margin20dp = PixelUtils.dpToPix(20);
		//margin25dp = displayMetrics.density*25f;
		margin25dp = PixelUtils.dpToPix(25);
		//margin30dp = displayMetrics.density*30f;
		margin30dp = PixelUtils.dpToPix(30);
		//margin40dp = displayMetrics.density*40f;
		margin40dp = PixelUtils.dpToPix(40);

        appContext = this;

        path = Environment.getExternalStorageDirectory() + "/" + "WeatherStation/";

		tempUnit = mPrefs.getInt("temp_unit", 0);
		pressUnit = mPrefs.getInt("press_unit", 0);

		initCharts(true, true, 1); // continuous, day view, view today

		/** Intent of background service */
		Intent alarmIntent = new Intent(this, BGService.class);
		/** Pending intent of background service */
		PendingIntent pendingStartIntent = PendingIntent.getService(this, 1700, alarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		/** AlarmManager for repeated call of background service */
		AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
				3600000, pendingStartIntent);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onPostCreate called");
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mTempSensor != null) {
			mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		if (mPressSensor != null) {
			mSensorManager.registerListener(this, mPressSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		if (mHumidSensor != null) {
			mSensorManager.registerListener(this, mHumidSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onConfiguration called");
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onClick(View v) {
		if (BuildConfig.DEBUG) Log.d(LOG_TAG, "OnClick id = " + v.getId());
		switch (v.getId()) {
			case R.id.b_tb_up:
				if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Finish");
				finish();
				break;
			case R.id.b_infinite:
				if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Infinite");
				isContinuous = true;
				plotValues = 20;
                clearCharts();
				initCharts(true, true, 1);
				tempLevelsPlot.redraw();
				pressLevelsPlot.redraw();
				humidLevelsPlot.redraw();
				/** Image button to jump to previous or next day */
				ImageButton b_nav = (ImageButton) findViewById(R.id.b_next);
				b_nav.setVisibility(View.GONE);
				b_nav = (ImageButton) findViewById(R.id.b_last);
				b_nav.setVisibility(View.GONE);
				break;
			case R.id.b_day_view:
				if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Day view");
				isContinuous = false;
				plotValues = 24;
				dayToShow = 1;
                clearCharts();
				initCharts(false, true, 1);
				tempLevelsPlot.redraw();
				pressLevelsPlot.redraw();
				humidLevelsPlot.redraw();
				if (numOfDayRecords != 1) {
					b_nav = (ImageButton) findViewById(R.id.b_last);
					b_nav.setVisibility(View.VISIBLE);
				}
				break;
			case R.id.b_month_view:
				if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Month view");
				isContinuous = false;
				plotValues = 31;
                clearCharts();
				initCharts(false, false, 1);
				tempLevelsPlot.redraw();
				pressLevelsPlot.redraw();
				humidLevelsPlot.redraw();
				b_nav = (ImageButton) findViewById(R.id.b_next);
				b_nav.setVisibility(View.GONE);
				b_nav = (ImageButton) findViewById(R.id.b_last);
				b_nav.setVisibility(View.GONE);
				break;
			case R.id.b_next:
				if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Next day view");
				if (dayToShow > 1) {
					dayToShow--;

					isContinuous = false;
					plotValues = 24;
                    clearCharts();
					initCharts(false, true, dayToShow);
					tempLevelsPlot.redraw();
					pressLevelsPlot.redraw();
					humidLevelsPlot.redraw();
					if (dayToShow == 1) {
						b_nav = (ImageButton) findViewById(R.id.b_next);
						b_nav.setVisibility(View.GONE);
					}
					if (numOfDayRecords != 1) {
						b_nav = (ImageButton) findViewById(R.id.b_last);
						b_nav.setVisibility(View.VISIBLE);
					}
				}
				break;
			case R.id.b_last:
				if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Previous day view");
				//numOfDayRecords = DataHolder.sizeOfDayEntry();
				if (dayToShow < numOfDayRecords) {
					dayToShow++;

					isContinuous = false;
					plotValues = 24;
                    clearCharts();
					initCharts(false, true, dayToShow);
					tempLevelsPlot.redraw();
					pressLevelsPlot.redraw();
					humidLevelsPlot.redraw();
					if (dayToShow != 1) {
						b_nav = (ImageButton) findViewById(R.id.b_next);
						b_nav.setVisibility(View.VISIBLE);
					}
					if (dayToShow == numOfDayRecords) {
						b_nav = (ImageButton) findViewById(R.id.b_last);
						b_nav.setVisibility(View.GONE);
					}
				}
				break;
			case R.id.sb_export:
				if (!exportDatabase()) {
					Utils.myAlert(this, getString(R.string.errorExportTitle), getString(R.string.errorExport));
				} else {
					Utils.myAlert(this, getString(R.string.succExportTitle), getString(R.string.succExport, exportFilePath));
				}
				findViewById(R.id.sb_temp).setVisibility(View.GONE);
				findViewById(R.id.sb_pressure).setVisibility(View.GONE);
				findViewById(R.id.sb_theme).setVisibility(View.GONE);
				mDrawerLayout.closeDrawers();
				break;
			case R.id.sb_backup:
				if (!backupDBasJSON()) {
					Utils.myAlert(this, getString(R.string.errorBackupTitle), getString(R.string.errorBackup));
				} else {
					Utils.myAlert(this, getString(R.string.succBackupTitle), getString(R.string.succBackup, exportFilePath));
				}
				findViewById(R.id.sb_temp).setVisibility(View.GONE);
				findViewById(R.id.sb_pressure).setVisibility(View.GONE);
				findViewById(R.id.sb_theme).setVisibility(View.GONE);
				mDrawerLayout.closeDrawers();
				break;
			case R.id.sb_restore:
                restoreFileDialog();
				findViewById(R.id.sb_temp).setVisibility(View.GONE);
				findViewById(R.id.sb_pressure).setVisibility(View.GONE);
				findViewById(R.id.sb_theme).setVisibility(View.GONE);
				mDrawerLayout.closeDrawers();
				break;
			case R.id.sb_clear:
				/** Builder for alert dialog */
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						this);
				// set title
				alertDialogBuilder.setTitle(getString(R.string.clearTitle));
				// set dialog message
				alertDialogBuilder
						.setMessage(getString(R.string.clearText))
						.setCancelable(false)
						.setPositiveButton(this.getResources().getString(android.R.string.ok),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										/** Instance of weather db helper */
										wsDbHelper = new WSDatabaseHelper(appContext);
										dataBase = wsDbHelper.getReadableDatabase();
										wsDbHelper.cleanDB(dataBase);
										dataBase.close();
										wsDbHelper.close();
										dialog.cancel();
									}
								})
						.setNegativeButton(this.getResources().getString(android.R.string.cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.cancel();
									}
								});
				// create alert dialog
				/** Alert dialog to be shown */
				AlertDialog alertDialog = alertDialogBuilder.create();
				// show it
				alertDialog.show();
				findViewById(R.id.sb_temp).setVisibility(View.GONE);
				findViewById(R.id.sb_pressure).setVisibility(View.GONE);
				findViewById(R.id.sb_theme).setVisibility(View.GONE);
				mDrawerLayout.closeDrawers();
				break;
			case R.id.sb_settings:
				findViewById(R.id.sb_temp).setVisibility(View.VISIBLE);
				findViewById(R.id.sb_pressure).setVisibility(View.VISIBLE);
				findViewById(R.id.sb_theme).setVisibility(View.VISIBLE);
				break;
			case R.id.sb_temp:
				/** Builder for theme selection dialog */
				AlertDialog.Builder selTempUnitBuilder = new AlertDialog.Builder(this);
				/** Inflater for theme selection dialog */
				LayoutInflater selTempUnitInflater = getLayoutInflater();
				/** View for theme selection dialog */
				View selTempUnitView = selTempUnitInflater.inflate(R.layout.settings_temp_units, null);
				selTempUnitBuilder.setView(selTempUnitView);
				/** Pointer to theme selection dialog */
				AlertDialog selTempUnit = selTempUnitBuilder.create();
				selTempUnit.setTitle(getString(R.string.sbTempUnits));

				selTempUnit.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								mPrefs.edit().putInt("temp_unit", selectTempUnit).apply();
								tempUnit = selectTempUnit;
								findViewById(R.id.sb_temp).setVisibility(View.GONE);
								findViewById(R.id.sb_pressure).setVisibility(View.GONE);
								findViewById(R.id.sb_theme).setVisibility(View.GONE);
								mDrawerLayout.closeDrawers();
								clearCharts();
								initCharts(true, true, 1); // continuous, day view, view today
								dialog.dismiss();
							}
						});

				selTempUnit.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								findViewById(R.id.sb_temp).setVisibility(View.GONE);
								findViewById(R.id.sb_pressure).setVisibility(View.GONE);
								findViewById(R.id.sb_theme).setVisibility(View.GONE);
								mDrawerLayout.closeDrawers();
								dialog.dismiss();
							}
						});

				selTempUnit.show();
				/** Radio group with radio buttons for temperature unit selection */
				RadioGroup rgTempUnit = (RadioGroup) selTempUnitView.findViewById(R.id.rg_temp);
				rgTempUnit.clearCheck();
				rgTempUnit.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						switch (checkedId) {
							case R.id.rb_centigrade:
								selectTempUnit = 0;
								break;
							case R.id.rb_fahrenheit:
								selectTempUnit = 1;
								break;
							case R.id.rb_kelvin:
								selectTempUnit = 2;
								break;
						}
					}
				});
				/** Holds user selected theme */
				selectTempUnit = tempUnit = mPrefs.getInt("temp_unit", 0);
				/** Pointer to preselected radio button */
				RadioButton rbTempUnit;
				switch (tempUnit) {
					case 0:
						rbTempUnit = (RadioButton) rgTempUnit.findViewById(R.id.rb_centigrade);
						rbTempUnit.setChecked(true);
						break;
					case 1:
						rbTempUnit = (RadioButton) rgTempUnit.findViewById(R.id.rb_fahrenheit);
						rbTempUnit.setChecked(true);
						break;
					case 2:
						rbTempUnit = (RadioButton) rgTempUnit.findViewById(R.id.rb_kelvin);
						rbTempUnit.setChecked(true);
						break;
				}
				break;
			case R.id.sb_pressure:
				/** Builder for theme selection dialog */
				AlertDialog.Builder selPressUnitBuilder = new AlertDialog.Builder(this);
				/** Inflater for theme selection dialog */
				LayoutInflater selPressUnitInflater = getLayoutInflater();
				/** View for theme selection dialog */
				View selPressUnitView = selPressUnitInflater.inflate(R.layout.settings_press_units, null);
				selPressUnitBuilder.setView(selPressUnitView);
				/** Pointer to theme selection dialog */
				AlertDialog selPressUnit = selPressUnitBuilder.create();
				selPressUnit.setTitle(getString(R.string.sbPressUnits));

				selPressUnit.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								mPrefs.edit().putInt("press_unit", selectPressUnit).apply();
								pressUnit = selectPressUnit;
								findViewById(R.id.sb_temp).setVisibility(View.GONE);
								findViewById(R.id.sb_pressure).setVisibility(View.GONE);
								findViewById(R.id.sb_theme).setVisibility(View.GONE);
								mDrawerLayout.closeDrawers();
								clearCharts();
								initCharts(true, true, 1); // continuous, day view, view today
								dialog.dismiss();
							}
						});

				selPressUnit.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								findViewById(R.id.sb_temp).setVisibility(View.GONE);
								findViewById(R.id.sb_pressure).setVisibility(View.GONE);
								findViewById(R.id.sb_theme).setVisibility(View.GONE);
								mDrawerLayout.closeDrawers();
								dialog.dismiss();
							}
						});

				selPressUnit.show();
				/** Radio group with radio buttons for pressure unit selection */
				rgPressUnit1 = (RadioGroup) selPressUnitView.findViewById(R.id.rg_press1);
				rgPressUnit2 = (RadioGroup) selPressUnitView.findViewById(R.id.rg_press2);
				rgPressUnit1.clearCheck();
				rgPressUnit2.clearCheck();

				/** Radio group listener for column 1 of an double column radio group */
				PressUnit1 = new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (checkedId != -1) {
							rgPressUnit2.setOnCheckedChangeListener(null);
							rgPressUnit2.clearCheck();
							rgPressUnit2.setOnCheckedChangeListener(PressUnit2);
						}
						switch (checkedId) {
							case R.id.rb_mBar:
								selectPressUnit = 0;
								break;
							case R.id.rb_psi:
								selectPressUnit = 1;
								break;
							case R.id.rb_atm:
								selectPressUnit = 2;
								break;
							case R.id.rb_Torr:
								selectPressUnit = 3;
								break;
						}
					}
				};

				/** Radio group listener for column 1 of an double column radio group  */
				PressUnit2 = new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (checkedId != -1) {
							rgPressUnit1.setOnCheckedChangeListener(null);
							rgPressUnit1.clearCheck();
							rgPressUnit1.setOnCheckedChangeListener(PressUnit1);
						}
						switch (checkedId) {
							case R.id.rb_kPa:
								selectPressUnit = 4;
								break;
							case R.id.rb_hPa:
								selectPressUnit = 5;
								break;
							case R.id.rb_mmHg:
								selectPressUnit = 6;
								break;
							case R.id.rb_inHg:
								selectPressUnit = 7;
								break;
						}
					}
				};

				rgPressUnit1.setOnCheckedChangeListener(PressUnit1);
				rgPressUnit2.setOnCheckedChangeListener(PressUnit2);
				/** Holds user selected theme */
				selectPressUnit = pressUnit = mPrefs.getInt("press_unit", 0);
				/** Pointer to preselected radio button */
				RadioButton rbPressUnit;
				switch (pressUnit) {
					case 0:
						rbPressUnit = (RadioButton) rgPressUnit1.findViewById(R.id.rb_mBar);
						rbPressUnit.setChecked(true);
						break;
					case 1:
						rbPressUnit = (RadioButton) rgPressUnit1.findViewById(R.id.rb_psi);
						rbPressUnit.setChecked(true);
						break;
					case 2:
						rbPressUnit = (RadioButton) rgPressUnit1.findViewById(R.id.rb_atm);
						rbPressUnit.setChecked(true);
						break;
					case 3:
						rbPressUnit = (RadioButton) rgPressUnit1.findViewById(R.id.rb_Torr);
						rbPressUnit.setChecked(true);
						break;
					case 4:
						rbPressUnit = (RadioButton) rgPressUnit2.findViewById(R.id.rb_kPa);
						rbPressUnit.setChecked(true);
						break;
					case 5:
						rbPressUnit = (RadioButton) rgPressUnit2.findViewById(R.id.rb_hPa);
						rbPressUnit.setChecked(true);
						break;
					case 6:
						rbPressUnit = (RadioButton) rgPressUnit2.findViewById(R.id.rb_mmHg);
						rbPressUnit.setChecked(true);
						break;
					case 7:
						rbPressUnit = (RadioButton) rgPressUnit2.findViewById(R.id.rb_inHg);
						rbPressUnit.setChecked(true);
						break;
				}
				mDrawerLayout.closeDrawers();
				break;
			case R.id.sb_theme:
                /** Builder for theme selection dialog */
                AlertDialog.Builder selThemeBuilder = new AlertDialog.Builder(this);
                /** Inflater for theme selection dialog */
                LayoutInflater selThemeInflater = getLayoutInflater();
                /** View for theme selection dialog */
                View selThemeView = selThemeInflater.inflate(R.layout.settings_theme, null);
                selThemeBuilder.setView(selThemeView);
                /** Pointer to theme selection dialog */
                AlertDialog selTheme = selThemeBuilder.create();
                selTheme.setTitle(getString(R.string.sbThemes));

                selTheme.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mPrefs.edit().putInt("dark_theme", selectColor).apply();
	                            findViewById(R.id.sb_temp).setVisibility(View.GONE);
	                            findViewById(R.id.sb_pressure).setVisibility(View.GONE);
	                            findViewById(R.id.sb_theme).setVisibility(View.GONE);
                                dialog.dismiss();
                                finish();
                                /** Intent to restart this app */
                                Intent intent = new Intent(appContext, WeatherStation.class);
                                startActivity(intent);
                            }
                        });

                selTheme.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
	                            findViewById(R.id.sb_temp).setVisibility(View.GONE);
	                            findViewById(R.id.sb_pressure).setVisibility(View.GONE);
	                            findViewById(R.id.sb_theme).setVisibility(View.GONE);
                                dialog.dismiss();
                            }
                        });

                selTheme.show();
                /** Radio group with radio buttons for theme selection */
                RadioGroup rgTheme = (RadioGroup) selThemeView.findViewById(R.id.rg_theme);
                rgTheme.clearCheck();
                rgTheme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        Log.d("chk", "id" + checkedId);

                        switch (checkedId) {
                            case R.id.rb_blue:
                                selectColor = 0;
                                break;
                            case R.id.rb_green:
                                selectColor = 1;
                                break;
                            case R.id.rb_gray:
                                selectColor = 2;
                                break;
                            case R.id.rb_orange:
                                selectColor = 3;
                                break;
                            case R.id.rb_red:
                                selectColor = 4;
                                break;
                            case R.id.rb_white:
                                selectColor = 5;
                                break;
                        }
                    }

                });
                /** Holds user selected theme */
                themeColor = mPrefs.getInt("dark_theme", 0);
                selectColor = themeColor;
                /** Pointer to preselected radio button */
                RadioButton rbTheme;
                switch (themeColor) {
                    case 0:
                        rbTheme = (RadioButton) rgTheme.findViewById(R.id.rb_blue);
                        rbTheme.setChecked(true);
                        break;
                    case 1:
                        rbTheme = (RadioButton) rgTheme.findViewById(R.id.rb_green);
                        rbTheme.setChecked(true);
                        break;
                    case 2:
                        rbTheme = (RadioButton) rgTheme.findViewById(R.id.rb_gray);
                        rbTheme.setChecked(true);
                        break;
                    case 3:
                        rbTheme = (RadioButton) rgTheme.findViewById(R.id.rb_orange);
                        rbTheme.setChecked(true);
                        break;
                    case 4:
                        rbTheme = (RadioButton) rgTheme.findViewById(R.id.rb_red);
                        rbTheme.setChecked(true);
                        break;
                    case 5:
                        rbTheme = (RadioButton) rgTheme.findViewById(R.id.rb_white);
                        rbTheme.setChecked(true);
                        break;
                }
				mDrawerLayout.closeDrawers();
                break;
		}
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        restoreFilePath = path + files.get(position);
        isFileSelected = true;
    }

	/**
	 * Listen to weather sensor accuracy changes
	 *
	 * @param sensor
	 *            Sensor sensor.
	 * @param accuracy
	 *            int accuracy.
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/**
	 * Listen to weather sensor change events
	 * @see <a href="http://androidcookbook.com/Recipe.seam?recipeId=2385">
	 * Reading the Temperature Sensor</a>
	 * @see <a href="http://www.survivingwithandroid.com/2013/09/android-sensor-tutorial-barometer-sensor.html">
	 * Android Sensor Tutorial: Barometer Sensor</a>
	 * @see <a href="http://code.tutsplus.com/tutorials/building-apps-with-environment-sensors--pre-46879">
	 * Building Apps with Environment Sensors</a>
	 *
	 * @param event
	 *            SensorEvent event.
	 */
	public void onSensorChanged(SensorEvent event) {
		if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onSensorChanged "+event.sensor.getName()+
				" "+event.sensor.getType());

		/** Integer array for return values */
		int[] currTime = getCurrentDate();
		if (BuildConfig.DEBUG) Log.d(LOG_TAG, "event timestamp "+currTime[0]+"h "
				+" on "+currTime[1]+" of month "+currTime[2]);

		switch (event.sensor.getType()) {
			case Sensor.TYPE_AMBIENT_TEMPERATURE:
				/** Current temperature converted to user selected unit */
				float currTemp = Utils.cToU(event.values[0], tempUnit);
				tvCurrTempView.setText(String.format("%.01f",
						currTemp) + Utils.tempUnit(appContext, tempUnit));
				if (isContinuous) {
					if (tempLevelsSeries.size() < 2) {
						tempLevelsSeries.addLast(null,(Math.round(currTemp * 10000.0f) / 10000.0f)+0.1f);
						tempLevelsSeries.addLast(null,(Math.round(currTemp * 10000.0f) / 10000.0f)-0.1f);
					}
					tempLevelsSeries.addLast(null, Math.round(currTemp * 10000.0f) / 10000.0f);

					if (tempLevelsSeries.size() > plotValues) {
						tempLevelsSeries.removeFirst();
					}

					/** Min and Max values to calculate new top and bottom range values of temperature plot */
					float[] minMax = Utils.getMinMax(0);
					minTempValue = minMax[0];
					maxTempValue = minMax[1];
					tempLevelsPlot.setRangeBottomMax(minTempValue-0.1);
					tempLevelsPlot.setRangeBottomMin(minTempValue - 0.1);
					tempLevelsPlot.setRangeTopMax(maxTempValue + 0.1);
					tempLevelsPlot.setRangeTopMin(maxTempValue + 0.1);
					tempLevelsPlot.redraw();
				}
				break;
			case Sensor.TYPE_PRESSURE:
				/** Current pressure converted to user selected unit */
				float currPress = Utils.pToU(event.values[0], pressUnit);
				/** Padding for top and bottom of the plot depending on user selected unit */
				float plotPadding = Utils.pressBoundary(pressUnit);
				tvCurrPressView.setText(String.format(Utils.pressFormatTitle(pressUnit),
						currPress) + Utils.pressUnit(appContext, pressUnit));
				if (isContinuous) {
					if (pressLevelsSeries.size() < 2) {
						pressLevelsSeries.addLast(null,(Math.round(currPress * 10000.0f) / 10000.0f)+plotPadding);
						pressLevelsSeries.addLast(null,(Math.round(currPress * 10000.0f) / 10000.0f)-plotPadding);
					}
					pressLevelsSeries.addLast(null, Math.round(currPress * 10000.0f) / 10000.0f);

					if (pressLevelsSeries.size() > plotValues) {
						pressLevelsSeries.removeFirst();
					}

					/** Min and Max values to calculate new top and bottom range values of pressure plot */
					float[] minMax = Utils.getMinMax(1);
					minPressValue = minMax[0];
					maxPressValue = minMax[1];
					pressLevelsPlot.setRangeBottomMax(minPressValue - plotPadding);
					pressLevelsPlot.setRangeBottomMin(minPressValue - plotPadding);
					pressLevelsPlot.setRangeTopMax(maxPressValue + plotPadding);
					pressLevelsPlot.setRangeTopMin(maxPressValue + plotPadding);
					pressLevelsPlot.redraw();
				}
				break;
			case Sensor.TYPE_RELATIVE_HUMIDITY:
				tvCurrHumidView.setText(String.format("%.01f",
						event.values[0]) + getString(R.string.humidSign));
				if (isContinuous) {
					if (humidLevelsSeries.size() < 2) {
						humidLevelsSeries.addLast(null,(Math.round(event.values[0] * 10000.0f) / 10000.0f)+0.1f);
						humidLevelsSeries.addLast(null,(Math.round(event.values[0] * 10000.0f) / 10000.0f)-0.1f);
					}
					humidLevelsSeries.addLast(null, Math.round(event.values[0] * 10000.0f) / 10000.0f);

					if (humidLevelsSeries.size() > plotValues) {
						humidLevelsSeries.removeFirst();
					}

					/** Min and Max values to calculate new top and bottom range values of humidity plot */
					float[] minMax = Utils.getMinMax(2);
					minHumidValue = minMax[0];
					maxHumidValue = minMax[1];
					humidLevelsPlot.setRangeBottomMax(minHumidValue - 0.1);
					humidLevelsPlot.setRangeBottomMin(minHumidValue - 0.1);
					humidLevelsPlot.setRangeTopMax(maxHumidValue + 0.1);
					humidLevelsPlot.setRangeTopMin(maxHumidValue + 0.1);
					humidLevelsPlot.redraw();
				}
				break;
		}
	}

	/**
	 * Initialize charts for hourly view
	 *
	 * @param isContinuous
	 *            true -> use .useImplicitXVals(); for X values
	 *            false -> X values
	 * @param isDay
	 *            true -> get hourly records
	 *            false -> get daily records
	 * @param day
	 *            day we want to get the records from
	 *            1 = today
	 *            2 = yesterday
	 *            3 = 2 days ago
	 *            ...
	 */
	private void initCharts(boolean isContinuous, boolean isDay, int day){

		/** String with the X axis name */
		String xValueLabel = getString(R.string.currCont);
		/** List to hold the timestamps */
		ArrayList<Integer> timeStamps = new ArrayList<>();
		/** List to hold the day stamps */
		ArrayList<Integer> dayStamps = new ArrayList<>();
		/** List to hold the max temperature values */
		ArrayList<Float> tempMaxEntries = new ArrayList<>();
		/** List to hold the min temperature values */
		ArrayList<Float> tempMinEntries = new ArrayList<>();
        tempMinEntries.clear();
		/** List to hold the (average) temperature values */
		ArrayList<Float> tempEntries = new ArrayList<>();
		/** List to hold the max pressure values */
		ArrayList<Float> pressMaxEntries = new ArrayList<>();
		/** List to hold the min pressure values */
		ArrayList<Float> pressMinEntries = new ArrayList<>();
		/** List to hold the (average) pressure values */
		ArrayList<Float> pressEntries = new ArrayList<>();
		/** List to hold the max humidity values */
		ArrayList<Float> humidMaxEntries = new ArrayList<>();
		/** List to hold the min humidity values */
		ArrayList<Float> humidMinEntries = new ArrayList<>();
		/** List to hold the (average) humidity values */
		ArrayList<Float> humidEntries = new ArrayList<>();

		if(!isContinuous) {

			/** Instance of weather db helper */
			wsDbHelper = new WSDatabaseHelper(this);
			dataBase = wsDbHelper.getReadableDatabase();
			if (isDay) {
				xValueLabel = getString(R.string.currHour);
				/** Cursor filled with existing entries of today */
				Cursor dayEntry = wsDbHelper.getDay(dataBase, day);
				dayEntry.moveToFirst();

				for (int i = 0; i<dayEntry.getCount(); i++) {
					timeStamps.add(dayEntry.getInt(0));
					dayStamps.add(dayEntry.getInt(1));
					tempEntries.add(Utils.cToU(dayEntry.getFloat(3), tempUnit));
					pressEntries.add(Utils.pToU(dayEntry.getFloat(4), pressUnit));
					humidEntries.add(dayEntry.getFloat(5));
					dayEntry.moveToNext();
				}

				numOfDayRecords = 0;
				for (int i=1; i<32; i++) {
					/** Cursor filled with existing entries of today */
					dayEntry = wsDbHelper.getDay(dataBase, i);
					dayEntry.moveToFirst();
					if (dayEntry.getCount() == 0) {
						break;
					}
					numOfDayRecords++;
				}
				dayEntry.close();

			} else {
				xValueLabel = getString(R.string.currMonth);

				numOfDayRecords = 0;
				for (int i=1; i<32; i++) {
					/** Cursor filled with existing entries of today */
					Cursor dayEntry = wsDbHelper.getDay(dataBase, i);
					dayEntry.moveToFirst();
					if (dayEntry.getCount() == 0) {
						break;
					}
					numOfDayRecords++;
					dayEntry.moveToLast();
					timeStamps.add(dayEntry.getInt(0));
					dayStamps.add(dayEntry.getInt(1));
					tempMaxEntries.add(Utils.cToU(dayEntry.getFloat(6), tempUnit));
					tempMinEntries.add(Utils.cToU(dayEntry.getFloat(7), tempUnit));
					tempEntries.add(Utils.cToU(dayEntry.getFloat(8), tempUnit));
					pressMaxEntries.add(Utils.pToU(dayEntry.getFloat(9), pressUnit));
					pressMinEntries.add(Utils.pToU(dayEntry.getFloat(10), pressUnit));
					pressEntries.add(Utils.pToU(dayEntry.getFloat(11), pressUnit));
					humidMaxEntries.add(dayEntry.getFloat(12));
					humidMinEntries.add(dayEntry.getFloat(13));
					humidEntries.add(dayEntry.getFloat(14));
					dayEntry.close();
				}
			}
			dataBase.close();
			wsDbHelper.close();
		}

		// initialize chart for temperature for non-continuous update
		if (mTempSensor != null) {
			initTempChart(isContinuous, isDay, xValueLabel,
                    timeStamps, dayStamps,
                    tempEntries, tempMaxEntries, tempMinEntries);
		}

		// initialize chart for pressure for non-continuous update
		if (mPressSensor != null) {
			initPressChart(isContinuous, isDay, xValueLabel,
                    timeStamps, dayStamps,
                    pressEntries, pressMaxEntries, pressMinEntries);
		}

		// initialize chart for humidity for non-continuous update
		if (mHumidSensor != null) {
			initHumidChart(isContinuous, isDay, xValueLabel,
                    timeStamps, dayStamps,
                    humidEntries, humidMaxEntries, humidMinEntries);
		}

		// Activate the advertisements
		// Enable access to internet
		if (android.os.Build.VERSION.SDK_INT > 9) {
			/** ThreadPolicy to get permission to access internet */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
		/** View for Google Adsense ads */
		AdView mAdView = (AdView) findViewById(R.id.adView);
		/** Request for ad from Google */
		AdRequest adRequest = new AdRequest.Builder().build();
		mAdView.loadAd(adRequest);
	}

    /**
     * Clear all charts
     */
    private void clearCharts() {
        if (mTempSensor != null) {
            clearTempChart();
        }
        if (mPressSensor != null) {
            clearPressChart();
        }
        if (mHumidSensor != null) {
            clearHumidChart();
        }
    }

	/**
	 * Initialize temperature chart
	 *
	 * @param isContinuous
	 *            true -> use .useImplicitXVals(); for X values
	 *            false -> X values
	 * @param isDay
	 *            true -> get hourly records
	 *            false -> get daily records
	 * @param xValueTitle
	 *            title for X values as string
	 * @param timeStamps
	 *            list with time stamps of existing records
	 * @param dayStamps
	 *            list with day stamps of existing records if day view
	 * @param tempEntries
	 *            list with temperatures of existing records if day view
	 *            list with average temperatures if month view
	 * @param tempMaxEntries
	 *            unused if day view
	 *            list with max temperatures if month view
	 * @param tempMinEntries
	 *            unused if day view
	 *            list with min temperatures if month view
	 */
	private void initTempChart(boolean isContinuous, boolean isDay, String xValueTitle,
	                           ArrayList<Integer> timeStamps,
	                           ArrayList<Integer> dayStamps,
	                           ArrayList<Float> tempEntries,
	                           ArrayList<Float> tempMaxEntries,
	                           ArrayList<Float> tempMinEntries) {
		// find the temperature levels plot in the layout
		tempLevelsPlot = (XYPlot) findViewById(R.id.xyTempPlot);
		// setup and format temperature data series
		tempLevelsSeries = new SimpleXYSeries(getString(R.string.currTemp));

		minTempValue = -100f;
		maxTempValue = +100f;

		if (!isContinuous && tempEntries.size() != 0) {
			if (isDay) {
				minTempValue = Collections.min(tempEntries);
				maxTempValue = Collections.max(tempEntries);
			} else {
				minTempValue = Collections.min(tempMinEntries);
				maxTempValue = Collections.max(tempMaxEntries);
			}
		}
		tempLevelsPlot.setRangeBottomMax(minTempValue - 0.1);
		tempLevelsPlot.setRangeBottomMin(minTempValue - 0.1);
		tempLevelsPlot.setRangeTopMax(maxTempValue + 0.1);
		tempLevelsPlot.setRangeTopMin(maxTempValue + 0.1);

		/** Formatter for (average or current) temperature plot */
		LineAndPointFormatter tempSeriesFormatter;
		/** Formatter for max temperature plot */
		LineAndPointFormatter tempMaxSeriesFormatter;
		/** Formatter for min temperature plot */
		LineAndPointFormatter tempMinSeriesFormatter;

		if (isContinuous) {
			tempLevelsSeries.useImplicitXVals();
			tempSeriesFormatter = new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null);
			tempSeriesFormatter.getLinePaint().setStrokeWidth(10);
			tempLevelsPlot.addSeries(tempLevelsSeries, tempSeriesFormatter);
		} else {
			if (isDay) {
				if (dayStamps.size() == 0) {
					xValueTitle = xValueTitle +" "+ getString(R.string.noData);
				} else {
					xValueTitle = xValueTitle +" "+ getString(R.string.hourOfDay) +" "+ Integer.toString(dayStamps.get(0));
					for (int i=0; i<timeStamps.size(); i++) {
						tempLevelsSeries.addLast(timeStamps.get(i),tempEntries.get(i));
					}
				}
				tempSeriesFormatter = new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null);
				tempSeriesFormatter.getLinePaint().setStrokeWidth(10);
				tempLevelsPlot.addSeries(tempLevelsSeries, tempSeriesFormatter);
			} else {
				if (tempEntries.size() == 0) {
					xValueTitle = xValueTitle +" "+ getString(R.string.noData);
				} else {
					for (int i=0; i<dayStamps.size(); i++) {
						tempLevelsSeries.addLast(dayStamps.get(i),tempEntries.get(i));
					}
				}
				/* Data series for the max temperature */
				SimpleXYSeries tempMaxSeries = new SimpleXYSeries(getString(R.string.currTempMax));
				/* Data series for the min temperature */
                SimpleXYSeries tempMinSeries = new SimpleXYSeries(getString(R.string.currTempMin));
				for (int i=0; i<timeStamps.size(); i++) {
					tempMaxSeries.addLast(dayStamps.get(i), tempMaxEntries.get(i));
					tempMinSeries.addLast(dayStamps.get(i), tempMinEntries.get(i));
				}
				tempSeriesFormatter = new LineAndPointFormatter(Color.BLACK, Color.TRANSPARENT, Color.TRANSPARENT, null);
				tempSeriesFormatter.getLinePaint().setStrokeWidth(10);
				tempLevelsPlot.addSeries(tempLevelsSeries, tempSeriesFormatter);
				tempMaxSeriesFormatter = new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null);
				tempMaxSeriesFormatter.getLinePaint().setStrokeWidth(10);
				tempLevelsPlot.addSeries(tempMaxSeries, tempMaxSeriesFormatter);
				tempMinSeriesFormatter = new LineAndPointFormatter(Color.GREEN, Color.TRANSPARENT, Color.TRANSPARENT, null);
				tempMinSeriesFormatter.getLinePaint().setStrokeWidth(10);
				tempLevelsPlot.addSeries(tempMinSeries, tempMinSeriesFormatter);
			}
		}

		tempLevelsPlot.setDomainStepValue(plotValues);
		tempLevelsPlot.setTicksPerRangeLabel(3);
		tempLevelsPlot.getLayoutManager()
				.remove(tempLevelsPlot.getLegendWidget());
		tempLevelsPlot.getDomainLabelWidget().pack();
		tempLevelsPlot.setDomainValueFormat(new DecimalFormat("#"));
		tempLevelsPlot.getGraphWidget().setMarginBottom(margin30dp);
		tempLevelsPlot.getGraphWidget().setMarginTop(margin10dp);
		tempLevelsPlot.getGraphWidget().setMarginLeft(margin25dp);
		tempLevelsPlot.getGraphWidget().setMarginRight(margin10dp);
		tempLevelsPlot.getGraphWidget().getDomainLabelPaint().setTextSize(textSizeSmall);
		tempLevelsPlot.getDomainLabelWidget().getLabelPaint().setTextSize(textSizeSmall);
		tempLevelsPlot.getDomainLabelWidget().getLabelPaint().setTextSize(textSizeSmall);
		tempLevelsPlot.getDomainLabelWidget().position(0, XLayoutStyle.ABSOLUTE_FROM_CENTER,
				0, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.BOTTOM_MIDDLE);
		tempLevelsPlot.setDomainLabel(xValueTitle);

		tempLevelsPlot.getRangeLabelWidget().getLabelPaint().setTextSize(textSizeMedium);
		tempLevelsPlot.getGraphWidget().getRangeLabelPaint().setTextSize(textSizeSmall);
	    tempLevelsPlot.setRangeLabel(Utils.tempUnit(this, tempUnit));
		tempLevelsPlot.getRangeLabelWidget().position(0.04f, XLayoutStyle.RELATIVE_TO_LEFT,
				-0.1f, YLayoutStyle.RELATIVE_TO_BOTTOM,
				AnchorPosition.LEFT_BOTTOM);
		tempLevelsPlot.getRangeLabelWidget().setOrientation(TextOrientationType.HORIZONTAL);
		tempLevelsPlot.getRangeLabelWidget().pack();

		// Change border color
		tempLevelsPlot.getBackgroundPaint().setColor(colorDark);
		// Change grid background color
		tempLevelsPlot.getGraphWidget().getGridBackgroundPaint().setColor(colorBright);
		// Change grid color
		tempLevelsPlot.getGraphWidget().getRangeGridLinePaint().setColor
				(Color.TRANSPARENT);
		tempLevelsPlot.getGraphWidget().getRangeSubGridLinePaint().setColor
				(Color.TRANSPARENT);
		tempLevelsPlot.getGraphWidget().getDomainGridLinePaint().setColor
				(Color.TRANSPARENT);
		tempLevelsPlot.getGraphWidget().getDomainSubGridLinePaint().setColor
				(Color.TRANSPARENT);
		// Change plot background color
		tempLevelsPlot.getGraphWidget().getBackgroundPaint().setColor(colorDark);
		// Change axis values text color and increment value
		if (isContinuous) {
			tempLevelsPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);
		} else {
			tempLevelsPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.WHITE);
			tempLevelsPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 2);
		}
		tempLevelsPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.WHITE);

		tempLevelsPlot.redraw();
	}

	/**
	 * Clean up temperature chart for a new initialization
	 */
	private void clearTempChart() {
		if (tempLevelsSeries.size() != 0) {
			for (int i=0; i<=tempLevelsSeries.size(); i++) {
				tempLevelsSeries.removeFirst();
			}
		}
		tempLevelsPlot.removeSeries(tempLevelsSeries);
		tempLevelsPlot.removeMarkers();
		tempLevelsPlot.clear();
	}

	/**
	 * Initialize pressure chart
	 *
	 * @param isContinuous
	 *            true -> use .useImplicitXVals(); for X values
	 *            false -> X values
	 * @param isDay
	 *            true -> get hourly records
	 *            false -> get daily records
	 * @param xValueTitle
	 *            title for X values as string
	 * @param timeStamps
	 *            list with time stamps of existing records
	 * @param dayStamps
	 *            list with day stamps of existing records if day view
	 * @param pressEntries
	 *            list with pressures of existing records if day view
	 *            list with average pressures if month view
	 * @param pressMaxEntries
	 *            unused if day view
	 *            list with max pressures if month view
	 * @param pressMinEntries
	 *            unused if day view
	 *            list with min pressures if month view
	 */
	private void initPressChart(boolean isContinuous, boolean isDay, String xValueTitle,
	                            ArrayList<Integer> timeStamps,
	                            ArrayList<Integer> dayStamps,
	                            ArrayList<Float> pressEntries,
	                            ArrayList<Float> pressMaxEntries,
	                            ArrayList<Float> pressMinEntries) {
		// find the pressure levels plot in the layout
		pressLevelsPlot = (XYPlot) findViewById(R.id.xyPressPlot);
		// setup and format pressure data series
		pressLevelsSeries = new SimpleXYSeries(getString(R.string.currPress));

		minPressValue = Utils.pToU(900f, pressUnit);
		maxPressValue = Utils.pToU(1100f, pressUnit);
		/** Padding for top and bottom of the plot depending on user selected unit */
		float plotPadding = Utils.pressBoundary(pressUnit);

		if (!isContinuous && pressEntries.size() != 0) {
			if (isDay) {
				minPressValue = Collections.min(pressEntries);
				maxPressValue = Collections.max(pressEntries);
			} else {
				minPressValue = Collections.min(pressMinEntries);
				maxPressValue = Collections.max(pressMaxEntries);
			}
		}
		pressLevelsPlot.setRangeBottomMax(minPressValue - plotPadding);
		pressLevelsPlot.setRangeBottomMin(minPressValue - plotPadding);
		pressLevelsPlot.setRangeTopMax(maxPressValue + plotPadding);
		pressLevelsPlot.setRangeTopMin(maxPressValue + plotPadding);

		/** Formatter for (average) pressure plot */
		LineAndPointFormatter pressSeriesFormatter;
		/** Formatter for max pressure plot */
		LineAndPointFormatter pressMaxSeriesFormatter;
		/** Formatter for min pressure plot */
		LineAndPointFormatter pressMinSeriesFormatter;

		if (isContinuous) {
			pressLevelsSeries.useImplicitXVals();
			pressSeriesFormatter = new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null);
			pressSeriesFormatter.getLinePaint().setStrokeWidth(10);
			pressLevelsPlot.addSeries(pressLevelsSeries, pressSeriesFormatter);
		} else {
			if (isDay) {
				if (dayStamps.size() == 0) {
					xValueTitle = xValueTitle +" "+ getString(R.string.noData);
				} else {
					xValueTitle = xValueTitle +" "+ getString(R.string.hourOfDay) +" "+ Integer.toString(dayStamps.get(0));
					for (int i=0; i<timeStamps.size(); i++) {
						pressLevelsSeries.addLast(timeStamps.get(i),pressEntries.get(i));
					}
				}
				pressSeriesFormatter = new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null);
				pressSeriesFormatter.getLinePaint().setStrokeWidth(10);
				pressLevelsPlot.addSeries(pressLevelsSeries, pressSeriesFormatter);
			} else {
				if (pressEntries.size() == 0) {
					xValueTitle = xValueTitle +" "+ getString(R.string.noData);
				} else {
					for (int i=0; i<dayStamps.size(); i++) {
						pressLevelsSeries.addLast(dayStamps.get(i),pressEntries.get(i));
					}
				}
				/* Data series for the max barometric pressure */
				SimpleXYSeries pressMaxSeries = new SimpleXYSeries(getString(R.string.currPressMax));
				/* Data series for the min barometric pressure */
                SimpleXYSeries pressMinSeries = new SimpleXYSeries(getString(R.string.currPressMin));
				for (int i=0; i<timeStamps.size(); i++) {
					pressMaxSeries.addLast(dayStamps.get(i), pressMaxEntries.get(i));
					pressMinSeries.addLast(dayStamps.get(i), pressMinEntries.get(i));
				}
				pressSeriesFormatter = new LineAndPointFormatter(Color.BLACK, Color.TRANSPARENT, Color.TRANSPARENT, null);
				pressSeriesFormatter.getLinePaint().setStrokeWidth(10);
				pressLevelsPlot.addSeries(pressLevelsSeries, pressSeriesFormatter);
				pressMaxSeriesFormatter = new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null);
				pressMaxSeriesFormatter.getLinePaint().setStrokeWidth(10);
				pressLevelsPlot.addSeries(pressMaxSeries, pressMaxSeriesFormatter);
				pressMinSeriesFormatter = new LineAndPointFormatter(Color.GREEN, Color.TRANSPARENT, Color.TRANSPARENT, null);
				pressMinSeriesFormatter.getLinePaint().setStrokeWidth(10);
				pressLevelsPlot.addSeries(pressMinSeries, pressMinSeriesFormatter);
			}
		}

		pressLevelsPlot.setDomainStepValue(plotValues);
		pressLevelsPlot.setTicksPerRangeLabel(3);
	    pressLevelsPlot.getLayoutManager()
				.remove(pressLevelsPlot.getLegendWidget());
		pressLevelsPlot.getDomainLabelWidget().pack();
		if (isContinuous) {
			pressLevelsPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 2);
		} else {
			if (pressLevelsSeries.size() < 16) {
				pressLevelsPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1);
			} else {
				pressLevelsPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 2);
			}
		}
		pressLevelsPlot.setDomainValueFormat(new DecimalFormat("#"));
		pressLevelsPlot.getGraphWidget().setMarginBottom(margin30dp);
		pressLevelsPlot.getGraphWidget().setMarginTop(margin10dp);
		pressLevelsPlot.getGraphWidget().setMarginLeft(margin40dp);
		pressLevelsPlot.getGraphWidget().setMarginRight(margin10dp);
		pressLevelsPlot.getGraphWidget().getDomainLabelPaint().setTextSize(textSizeSmall);
		pressLevelsPlot.getDomainLabelWidget().getLabelPaint().setTextSize(textSizeSmall);
		pressLevelsPlot.getDomainLabelWidget().getLabelPaint().setTextSize(textSizeSmall);
	    pressLevelsPlot.getDomainLabelWidget().position(0, XLayoutStyle.ABSOLUTE_FROM_CENTER,
			    0, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.BOTTOM_MIDDLE);
		pressLevelsPlot.setDomainLabel(xValueTitle);

	    pressLevelsPlot.getRangeLabelWidget().getLabelPaint().setTextSize(textSizeMedium);
		pressLevelsPlot.getGraphWidget().getRangeLabelPaint().setTextSize(textSizeSmall);
		pressLevelsPlot.setRangeLabel(Utils.pressUnit(this, pressUnit));

		pressLevelsPlot.setRangeStepMode(XYStepMode.SUBDIVIDE);
		DecimalFormat rangeFormat = new DecimalFormat(Utils.pressFormatRange(pressUnit));
		rangeFormat.setMinimumFractionDigits(Utils.pressFormatRangeDigit(pressUnit));
		pressLevelsPlot.setRangeValueFormat(rangeFormat);
		pressLevelsPlot.getRangeLabelWidget().position(0.04f,XLayoutStyle.RELATIVE_TO_LEFT,
				-0.15f,YLayoutStyle.RELATIVE_TO_BOTTOM,
				AnchorPosition.LEFT_BOTTOM);
		pressLevelsPlot.getRangeLabelWidget().setOrientation(TextOrientationType.HORIZONTAL);
	    pressLevelsPlot.getRangeLabelWidget().pack();

		// Change border color
		pressLevelsPlot.getBackgroundPaint().setColor(colorDark);
		// Change grid background color
		pressLevelsPlot.getGraphWidget().getGridBackgroundPaint().setColor(colorBright);
		// Change grid color
		pressLevelsPlot.getGraphWidget().getRangeGridLinePaint().setColor
				(Color.TRANSPARENT);
		pressLevelsPlot.getGraphWidget().getRangeSubGridLinePaint().setColor
				(Color.TRANSPARENT);
		pressLevelsPlot.getGraphWidget().getDomainGridLinePaint().setColor
				(Color.TRANSPARENT);
		pressLevelsPlot.getGraphWidget().getDomainSubGridLinePaint().setColor
				(Color.TRANSPARENT);
		// Change plot background color
		pressLevelsPlot.getGraphWidget().getBackgroundPaint().setColor(colorDark);
		// Change axis values text color and increment value
		if (isContinuous) {
			pressLevelsPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);
		} else {
			pressLevelsPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.WHITE);
			pressLevelsPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 2);
		}
		pressLevelsPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.WHITE);

		pressLevelsPlot.redraw();
	}

	/**
	 * Clean up pressure chart for a new initialization
	 */
	private void clearPressChart() {
		if (pressLevelsSeries.size() != 0) {
			for (int i=0; i<=pressLevelsSeries.size(); i++) {
				pressLevelsSeries.removeFirst();
			}
		}
		pressLevelsPlot.removeSeries(pressLevelsSeries);
		pressLevelsPlot.removeMarkers();
		pressLevelsPlot.clear();
	}

	/**
	 * Initialize humidity chart
	 *
	 * @param isContinuous
	 *            true -> use .useImplicitXVals(); for X values
	 *            false -> X values
	 * @param isDay
	 *            true -> get hourly records
	 *            false -> get daily records
	 * @param xValueTitle
	 *            title for X values as string
	 * @param timeStamps
	 *            list with time stamps of existing records
	 * @param dayStamps
	 *            list with day stamps of existing records if day view
	 * @param humidEntries
	 *            list with humidity of existing records if day view
	 *            list with average humidity if month view
	 * @param humidMaxEntries
	 *            unused if day view
	 *            list with max humidity if month view
	 * @param humidMinEntries
	 *            unused if day view
	 *            list with min humidity if month view
	 */
	private void initHumidChart(boolean isContinuous, boolean isDay, String xValueTitle,
	                            ArrayList<Integer> timeStamps,
	                            ArrayList<Integer> dayStamps,
	                            ArrayList<Float> humidEntries,
	                            ArrayList<Float> humidMaxEntries,
	                            ArrayList<Float> humidMinEntries) {
		// find the humidity levels plot in the layout
		humidLevelsPlot = (XYPlot) findViewById(R.id.xyHumidPlot);
		// setup and format humidity data series
		humidLevelsSeries = new SimpleXYSeries(getString(R.string.currHumid));

		minHumidValue = -100f;
		maxHumidValue = +100f;

		if (!isContinuous && humidEntries.size() != 0) {
			if (isDay) {
				minHumidValue = Collections.min(humidEntries);
				maxHumidValue = Collections.max(humidEntries);
			} else {
				minHumidValue = Collections.min(humidMinEntries);
				maxHumidValue = Collections.max(humidMaxEntries);
			}
		}
		humidLevelsPlot.setRangeBottomMax(minHumidValue - 0.1);
		humidLevelsPlot.setRangeBottomMin(minHumidValue - 0.1);
		humidLevelsPlot.setRangeTopMax(maxHumidValue + 0.1);
		humidLevelsPlot.setRangeTopMin(maxHumidValue + 0.1);

		/** Formatter for (average) humidity plot */
		LineAndPointFormatter humidSeriesFormatter;
		/** Formatter for max humidity plot */
		LineAndPointFormatter humidMaxSeriesFormatter;
		/** Formatter for min humidity plot */
		LineAndPointFormatter humidMinSeriesFormatter;

		if (isContinuous) {
			humidLevelsSeries.useImplicitXVals();
			humidSeriesFormatter = new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null);
			humidSeriesFormatter.getLinePaint().setStrokeWidth(10);
			humidLevelsPlot.addSeries(humidLevelsSeries, humidSeriesFormatter);
		} else {
			if (isDay) {
				if (dayStamps.size() == 0) {
					xValueTitle = xValueTitle +" "+ getString(R.string.noData);
				} else {
					xValueTitle = xValueTitle +" "+ getString(R.string.hourOfDay) +" "+ Integer.toString(dayStamps.get(0));
					for (int i=0; i<timeStamps.size(); i++) {
						humidLevelsSeries.addLast(timeStamps.get(i),humidEntries.get(i));
					}
				}
				humidSeriesFormatter = new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null);
				humidSeriesFormatter.getLinePaint().setStrokeWidth(10);
				humidLevelsPlot.addSeries(humidLevelsSeries, humidSeriesFormatter);
			} else {
				if (humidEntries.size() == 0) {
					xValueTitle = xValueTitle +" "+ getString(R.string.noData);
				} else {
					for (int i=0; i<dayStamps.size(); i++) {
						humidLevelsSeries.addLast(dayStamps.get(i),humidEntries.get(i));
					}
				}
				/* Data series for the max humidity */
                SimpleXYSeries humidMaxSeries = new SimpleXYSeries(getString(R.string.currHumidMax));
				/* Data series for the min humidity */
                SimpleXYSeries humidMinSeries = new SimpleXYSeries(getString(R.string.currHumidMin));
				for (int i=0; i<timeStamps.size(); i++) {
					humidMaxSeries.addLast(dayStamps.get(i), humidMaxEntries.get(i));
					humidMinSeries.addLast(dayStamps.get(i), humidMinEntries.get(i));
				}
				humidSeriesFormatter = new LineAndPointFormatter(Color.BLACK, Color.TRANSPARENT, Color.TRANSPARENT, null);
				humidSeriesFormatter.getLinePaint().setStrokeWidth(10);
				humidLevelsPlot.addSeries(humidLevelsSeries, humidSeriesFormatter);
				humidMaxSeriesFormatter = new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null);
				humidMaxSeriesFormatter.getLinePaint().setStrokeWidth(10);
				humidLevelsPlot.addSeries(humidMaxSeries, humidMaxSeriesFormatter);
				humidMinSeriesFormatter = new LineAndPointFormatter(Color.GREEN, Color.TRANSPARENT, Color.TRANSPARENT, null);
				humidMinSeriesFormatter.getLinePaint().setStrokeWidth(10);
				humidLevelsPlot.addSeries(humidMinSeries, humidMinSeriesFormatter);
			}
		}

		humidLevelsPlot.setDomainStepValue(plotValues);
		humidLevelsPlot.setTicksPerRangeLabel(3);
		humidLevelsPlot.getLayoutManager().remove(humidLevelsPlot.getLegendWidget());
		humidLevelsPlot.getDomainLabelWidget().pack();
		if (isContinuous) {
			humidLevelsPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 2);
		} else {
			if (humidLevelsSeries.size() < 16) {
				humidLevelsPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1);
			} else {
				humidLevelsPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 2);
			}
		}
		humidLevelsPlot.setDomainValueFormat(new DecimalFormat("#"));
		humidLevelsPlot.getGraphWidget().setMarginBottom(margin30dp);
		humidLevelsPlot.getGraphWidget().setMarginTop(margin10dp);
		humidLevelsPlot.getGraphWidget().setMarginLeft(margin20dp);
		humidLevelsPlot.getGraphWidget().setMarginRight(margin10dp);
		humidLevelsPlot.getGraphWidget().getDomainLabelPaint().setTextSize(textSizeSmall);
		humidLevelsPlot.getDomainLabelWidget().getLabelPaint().setTextSize(textSizeSmall);
		humidLevelsPlot.getGraphWidget().getRangeLabelPaint().setTextSize(textSizeSmall);
		humidLevelsPlot.getDomainLabelWidget().getLabelPaint().setTextSize(textSizeSmall);
		humidLevelsPlot.getDomainLabelWidget().position(0, XLayoutStyle.ABSOLUTE_FROM_CENTER,
				0, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.BOTTOM_MIDDLE);
		humidLevelsPlot.setDomainLabel(xValueTitle);

		humidLevelsPlot.getRangeLabelWidget().getLabelPaint().setTextSize(textSizeMedium);
		humidLevelsPlot.setRangeLabel(getString(R.string.humidSign));
		humidLevelsPlot.getRangeLabelWidget().position(0.04f, XLayoutStyle.RELATIVE_TO_LEFT,
				-0.15f, YLayoutStyle.RELATIVE_TO_BOTTOM,
				AnchorPosition.LEFT_BOTTOM);
		humidLevelsPlot.getRangeLabelWidget().setOrientation(TextOrientationType.HORIZONTAL);
		humidLevelsPlot.getRangeLabelWidget().pack();

		// Change border color
		humidLevelsPlot.getBackgroundPaint().setColor(colorDark);
		// Change grid background color
		humidLevelsPlot.getGraphWidget().getGridBackgroundPaint().setColor(colorBright);
		// Change grid color
		humidLevelsPlot.getGraphWidget().getRangeGridLinePaint().setColor
				(Color.TRANSPARENT);
		humidLevelsPlot.getGraphWidget().getRangeSubGridLinePaint().setColor
				(Color.TRANSPARENT);
		humidLevelsPlot.getGraphWidget().getDomainGridLinePaint().setColor
				(Color.TRANSPARENT);
		humidLevelsPlot.getGraphWidget().getDomainSubGridLinePaint().setColor
				(Color.TRANSPARENT);
		// Change plot background color
		humidLevelsPlot.getGraphWidget().getBackgroundPaint().setColor(colorDark);
		// Change axis values text color and increment value
		if (isContinuous) {
			humidLevelsPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);
		} else {
			humidLevelsPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.WHITE);
			humidLevelsPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 2);
		}
		humidLevelsPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.WHITE);

		humidLevelsPlot.redraw();
	}

	/**
	 * Clean up humidity chart for a new initialization
	 */
	private void clearHumidChart() {
		if (humidLevelsSeries.size() != 0) {
			for (int i=0; i<=humidLevelsSeries.size(); i++) {
				humidLevelsSeries.removeFirst();
			}
		}
		humidLevelsPlot.removeSeries(humidLevelsSeries);
		humidLevelsPlot.removeMarkers();
		humidLevelsPlot.clear();
	}

	/**
	 * Get current date & time as string
	 *
	 * @return <code>int[]</code>
	 *            Date and time as integer values
	 *            int[0] = hour
	 *            int[1] = day
	 *            int[2] = month
	 */
	public static int[] getCurrentDate() {
		/** Integer array for return values */
		int[] currTime = new int[3];
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();

		/** Today's day */
		currTime[1] = cal.get(Calendar.DATE);

		/** Current hour */
		currTime[0] = cal.get(Calendar.HOUR_OF_DAY);

		/** Today's month */
		currTime[2] = cal.get(Calendar.MONTH) + 1;
		return currTime;
	}

	/**
	 * Read all entries from the database and write them in CSV format to the external storage
	 * File is saved in /WeatherStation/MM-dd-WeatherStation.csv
	 * MM = month
	 * dd = today's day
	 *
	 * @return <code>boolean</code>
	 *              true if file could be created
	 *              false if there was an error
	 */
	private boolean exportDatabase() {

		/**First of all we check if the external storage of the device is available for writing.
		 * Remember that the external storage is not necessarily the sd card. Very often it is
		 * the device storage.
		 */
		/** Status of external storage (mounted or not) */
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			return false;
		}
		else {
			//We use our own directory for saving our .csv file.
			/** Current time as integer array */
			int[] currTime = getCurrentDate();
			exportFilePath = path + Integer.toString(currTime[2]) + "-" +
					Integer.toString(currTime[1]) + "-" +
					"WeatherStation.csv";
			/** Pointer to directory */
			File file = new File(path);
			if (!file.exists()) {
				if (!file.mkdirs()) {
					// Big problem as we cannot create the path
					if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Cannot create folder on ext memory");
                    return false;
				}
			}
			file = new File(exportFilePath);
			if (file.exists()) {
				if (!file.delete()) {
					// Big problem as we cannot delete the old file
					if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Cannot delete existing file");
                    return false;
				}
			}

			/** PrintWriter to write into CSV file */
			PrintWriter printWriter = null;
			try
			{
				//if (!file.createNewFile()) return false;
				printWriter = new PrintWriter(new FileWriter(file));

				/** Instance of weather db helper */
				wsDbHelper = new WSDatabaseHelper(this);
				dataBase = wsDbHelper.getReadableDatabase();

				/** Cursor holding all rows of the database */
				Cursor allRows = wsDbHelper.getAll(dataBase);

				allRows.moveToFirst();

				//Write the name of the table and the name of the columns (comma separated values) in the .csv file.
                /** String with the recorded values or description */
				String record = "Timestamp,Datestamp,Daynumber," +
						"Temperature,Pressure,Humidity," +
						"MaxTemperature,MinTemperature,AverageTemperature," +
						"MaxPressure,MinPressure,AveragePressure," +
						"MaxHumidity,MinHumidity,AverageHumidity";
				printWriter.println(record); //write the record in the .csv file
				for (int i=0; i<allRows.getCount(); i++)
				{
					/** The timestamps */
					int timeStamp = allRows.getInt(1);
					/** The day stamps */
					int dateStamp = allRows.getInt(2);
					/** The day number */
					int dayNumber = allRows.getInt(3);
					/** The temperature value */
					float hourTemp = allRows.getFloat(4);
					/** The pressure value */
					float hourPress = allRows.getFloat(5);
					/** The humidity value */
					float hourHumid = allRows.getFloat(6);
					/** The max temperature value */
					float maxTemp = allRows.getFloat(7);
					/** The min temperature value */
					float minTemp = allRows.getFloat(8);
					/** The average temperature value */
					float avgTemp = allRows.getFloat(9);
					/** The max pressure value */
					float maxPress = allRows.getFloat(10);
					/** The min pressure value */
					float minPress = allRows.getFloat(11);
					/** The average pressure value */
					float avgPress = allRows.getFloat(12);
					/** The max humidity value */
					float maxHumid = allRows.getFloat(13);
					/** The min humidity value */
					float minHumid = allRows.getFloat(14);
					/** The average humidity value */
					float avgHumid = allRows.getFloat(15);

					/**Create the line to write in the .csv file.
					 * We need a String where values are comma separated.
					 * The field date (Long) is formatted in a readable text. The amount field
					 * is converted into String.
					 */
					record = Integer.toString(timeStamp) + "," + Integer.toString(dateStamp) + "," + Integer.toString(dayNumber) + "," +
							Float.toString(hourTemp) + "," + Float.toString(hourPress) + "," + Float.toString(hourHumid) + "," +
							Float.toString(maxTemp) + "," + Float.toString(minTemp) + "," + Float.toString(avgTemp) + "," +
							Float.toString(maxPress) + "," + Float.toString(minPress) + "," + Float.toString(avgPress) + "," +
							Float.toString(maxHumid) + "," + Float.toString(minHumid) + "," + Float.toString(avgHumid) + ",";
					printWriter.println(record); //write the record in the .csv file
					allRows.moveToNext();
				}
				printWriter.flush();
				allRows.close();
				dataBase.close();
				wsDbHelper.close();
			}
			catch(Exception exc) {
				//if there are any exceptions, return false
				return false;
			}
			finally {
				if(printWriter != null) printWriter.close();
			}

			//If there are no errors, return true.
			return true;
		}
	}

	/**
	* Backup database in JSON format on SD card
	 * File is saved in /WeatherStation/MM-dd-WeatherStation.JSON
	 * MM = month
	 * dd = today's day
	*
	* @return <code>boolean</code>
	*            true => write to file successful
	*            false => write to file failed
	*/
	private boolean backupDBasJSON() {

		/**First of all we check if the external storage of the device is available for writing.
		 * Remember that the external storage is not necessarily the sd card. Very often it is
		 * the device storage.
		 */
		/** Status of external storage (mounted or not) */
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			return false;
		}
		else {
			//We use our own directory for saving our .JSON file.
			/** Current time as integer array */
			int[] currTime = getCurrentDate();
			exportFilePath = path + Integer.toString(currTime[2]) + "-" +
					Integer.toString(currTime[1]) + "-" +
					"WeatherStation.JSON";
			/** Pointer to directory/file */
			File file = new File(path);
			if (!file.exists()) {
				if (!file.mkdirs()) {
					// Big problem as we cannot create the path
					if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Cannot create folder on ext memory");
					return false;
				}
			}
			file = new File(exportFilePath);
			if (file.exists()) {
				if (!file.delete()) {
					// Big problem as we cannot delete the old file
					if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Cannot delete existing file");
					return false;
				}
			}

			/** JSON object to hold the JSON objects and arrays with the recorded items */
			JSONArray jsonToday = new JSONArray();

			try
			{
				/** Instance of weather db helper */
				wsDbHelper = new WSDatabaseHelper(this);
				dataBase = wsDbHelper.getReadableDatabase();

				/** Cursor holding all rows of the database */
				Cursor allRows = wsDbHelper.getAll(dataBase);

				allRows.moveToFirst();

				for (int i=0; i<allRows.getCount(); i++)
				{
					/** JSON object to hold recorded measures */
					JSONObject obj = new JSONObject();
					obj.put("ts", allRows.getInt(1));
					obj.put("ds", allRows.getInt(2));
					obj.put("dn", allRows.getInt(3));
					obj.put("t", allRows.getFloat(4));
					obj.put("p", allRows.getFloat(5));
					obj.put("h", allRows.getFloat(6));
					obj.put("mat", allRows.getFloat(7));
					obj.put("mit", allRows.getFloat(8));
					obj.put("avt", allRows.getFloat(9));
					obj.put("map", allRows.getFloat(10));
					obj.put("mip", allRows.getFloat(11));
					obj.put("avp", allRows.getFloat(12));
					obj.put("mah", allRows.getFloat(13));
					obj.put("minh", allRows.getFloat(14));
					obj.put("avh", allRows.getFloat(15));
					jsonToday.put(obj);
					allRows.moveToNext();
				}

				allRows.close();
				dataBase.close();
				wsDbHelper.close();
			}
			catch(Exception exc) {
				//if there are any exceptions, return false
				return false;
			}
			try {
                /** File writer to JSON file */
				FileWriter fileJSON = new FileWriter(exportFilePath);
				fileJSON.write(jsonToday.toString());
				fileJSON.flush();
				fileJSON.close();
			} catch (IOException e) {
				return false;
			}

			//If there are no errors, return true.
			return true;
		}
	}

    /**
     * Show dialog with available backup files
     */
    private void restoreFileDialog() {
        /** File pointer to directory */
        File directory = new File(path);

        // clear files array
        files.clear();
        // get all the files from a directory
        /** List with files in directory */
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                /** String with filename extension */
                String filenameArray[] = file.getName().split("\\.");
                if (filenameArray[filenameArray.length-1].equalsIgnoreCase("JSON")) {
                    files.add(file.getName());
                }
            }
        }

        if (files.size() != 0) {
            /** Builder for restore file selection dialog */
            AlertDialog.Builder fileListBuilder = new AlertDialog.Builder(this);
            /** Inflater for restore file selection dialog */
            LayoutInflater fileListInflater = getLayoutInflater();
            /** View for restore file selection dialog */
            View fileListView = fileListInflater.inflate(R.layout.restore_dialog, null);
            fileListBuilder.setView(fileListView);
            /** Pointer to restore file selection dialog */
            AlertDialog fileList = fileListBuilder.create();
            fileList.setTitle(getString(R.string.sbRestore));

            fileList.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.bRestore),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (isFileSelected) {
                                dialog.dismiss();
                                if (restoreDBfromJSON()) {
	                                Utils.myAlert(appContext, getString(R.string.errorRestoreTitle), getString(R.string.errorRestore));
                                } else {
	                                Utils.myAlert(appContext, getString(R.string.succRestoreTitle), getString(R.string.succRestore, restoreFilePath));
                                }
                            } else {
	                            Utils.myAlert(appContext, getString(R.string.errorRestoreTitle), getString(R.string.noFileSelected));
                            }
                        }
                    });

            fileList.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            isFileSelected = false;
                            dialog.dismiss();
                        }
                    });

            /** Pointer to list view with the files */
            ListView lvFileList = (ListView) fileListView.findViewById(R.id.lv_FileList);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    files );

            lvFileList.setAdapter(arrayAdapter);
            lvFileList.setOnItemClickListener(this);

            fileList.show();

        } else {
	        Utils.myAlert(this,
		            getResources().getString(R.string.errorRestoreTitle),
		            getResources().getString(R.string.noRestoreFile));
        }

    }

    /**
	 * Restore database from JSON format file
	 *
	 * @return <code>boolean</code>
	 *            true => restore to database successful
	 *            false => restore to database failed
	 */
    private boolean restoreDBfromJSON() {

		/** List to hold the timestamps */
		ArrayList<Integer> timeStamp = new ArrayList<>();
		/** List to hold the day stamps */
		ArrayList<Integer> dateStamp = new ArrayList<>();
		/** List to hold the day numbers */
		ArrayList<Integer> dayNumber = new ArrayList<>();
		/** List to hold the temperature values */
		ArrayList<Float>  hourTemp = new ArrayList<>();
		/** List to hold the pressure values */
		ArrayList<Float> hourPress = new ArrayList<>();
		/** List to hold the humidity values */
		ArrayList<Float> hourHumid = new ArrayList<>();
		/** List to hold the max temperature values */
		ArrayList<Float> maxTemp = new ArrayList<>();
		/** List to hold the min temperature values */
		ArrayList<Float> minTemp = new ArrayList<>();
		/** List to hold the average temperature values */
		ArrayList<Float> avgTemp = new ArrayList<>();
		/** List to hold the max pressure values */
		ArrayList<Float> maxPress = new ArrayList<>();
		/** List to hold the min pressure values */
		ArrayList<Float> minPress = new ArrayList<>();
		/** List to hold the average pressure values */
		ArrayList<Float> avgPress = new ArrayList<>();
		/** List to hold the max humidity values */
		ArrayList<Float> maxHumid = new ArrayList<>();
		/** List to hold the min humidity values */
		ArrayList<Float> minHumid = new ArrayList<>();
		/** List to hold the average humidity values */
		ArrayList<Float> avgHumid = new ArrayList<>();

		try {
			/** Pointer to directory/file */
			File file = new File(restoreFilePath);

			/** Stream to read file */
			FileInputStream stream = new FileInputStream(file);
			/** String that holds the JSON object */
			String jsonStr = null;
			try {
				/** File channel for reading stream */
				FileChannel fc = stream.getChannel();
				/** Buffer for reading */
				MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

				jsonStr = Charset.defaultCharset().decode(bb).toString();
			} catch (Exception e) {
				return true;
			}
			finally {
				stream.close();
			}

			/** JSON array with the backup data */
			JSONArray jsonFromBackup = new JSONArray(jsonStr);

			// looping through all nodes
			for (int i = 0; i < jsonFromBackup.length(); i++) {
				/** JSON object for a single data record */
				JSONObject obj = jsonFromBackup.getJSONObject(i);

				timeStamp.add(obj.getInt("ts"));
				dateStamp.add(obj.getInt("ds"));
				dayNumber.add(obj.getInt("dn"));
				hourTemp.add((float) obj.getDouble("t"));
				hourPress.add((float) obj.getDouble("p"));
				hourHumid.add((float) obj.getDouble("h"));
				maxTemp.add((float) obj.getDouble("mat"));
				minTemp.add((float) obj.getDouble("mit"));
				avgTemp.add((float) obj.getDouble("avt"));
				maxPress.add((float) obj.getDouble("map"));
				minPress.add((float) obj.getDouble("mip"));
				avgPress.add((float) obj.getDouble("avp"));
				maxHumid.add((float) obj.getDouble("mah"));
				minHumid.add((float) obj.getDouble("minh"));
				avgHumid.add((float) obj.getDouble("avh"));
			}
		} catch (Exception e) {
			return true;
		}

		/** Instance of weather db helper */
		wsDbHelper = new WSDatabaseHelper(this);
		dataBase = wsDbHelper.getWritableDatabase();

		wsDbHelper.cleanDB(dataBase);
        // Write restore data to the database
		for (int i=0; i<timeStamp.size(); i++) {
			wsDbHelper.addDay(dataBase, timeStamp.get(i), dateStamp.get(i), dayNumber.get(i),
					hourTemp.get(i), hourPress.get(i), hourHumid.get(i),
					maxTemp.get(i), minTemp.get(i), avgTemp.get(i),
					maxPress.get(i), minPress.get(i), avgPress.get(i),
					maxHumid.get(i), minHumid.get(i), avgHumid.get(i));
		}
		dataBase.close();
		wsDbHelper.close();
		return false;
	}
}
