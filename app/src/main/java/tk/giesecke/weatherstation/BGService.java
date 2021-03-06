package tk.giesecke.weatherstation;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BGService
 * background service to get hourly updates from the sensors
 * and save them into the database
 *
 * @author Bernd Giesecke
 * @version 1.3 August 23, 2015
 */
public class BGService extends Service implements SensorEventListener {

	/** Debug tag */
	private static final String LOG_TAG = "WeatherStation-BG";

	/** SensorManager to get info about available sensors */
	private SensorManager mSensorManager;
	/** Access to temp sensor */
	private Sensor mTempSensor;
	/** Access to pressure sensor */
	private Sensor mPressSensor;
	/* Access to humidity sensor */
	private Sensor mHumidSensor;
	/** Last temperature for hourly recording */
	private float lastTempValue;
	/** Last pressure for hourly recording */
	private float lastPressValue;
	/** Last humidity for hourly recording */
	private float lastHumidValue;

	/** Instance of weather db helper */
    private WSDatabaseHelper wsDbHelper;
	/** Access to weather db  */
    private SQLiteDatabase dataBase;
	/** Retry counter for adding entry to database. Fail after trying 20 times */
    private int retryCounter;
	/** Flag if midnight passed and shift is done. */
    private boolean isShiftDone;

	public BGService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onCreate");

		// preset values
		lastTempValue = 0;
		lastPressValue = 0;
		lastHumidValue = 0;

		// connect to temperature sensor
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		// connect to temperature sensor
		/* Access to temp sensor */
		mTempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		// connect to air pressure sensor
		/* Access to pressure sensor */
		mPressSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		// connect to humidity sensor
		/* Access to humidity sensor */
		mHumidSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);

		if (mTempSensor != null) {
			mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_FASTEST);
			lastTempValue = -9999;
		}
		if (mPressSensor != null) {
			mSensorManager.registerListener(this, mPressSensor, SensorManager.SENSOR_DELAY_FASTEST);
			lastPressValue = -9999;
		}
		if (mHumidSensor != null) {
			mSensorManager.registerListener(this, mHumidSensor, SensorManager.SENSOR_DELAY_FASTEST);
			lastHumidValue = -9999;
		}

		// Set retry counter in case adding to database fails
		retryCounter = 0;
		// Set day shift done to false */
		isShiftDone = false;

		/** IntentFilter to receive Screen on/off broadcast msgs */
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		/** BroadcastReceiver to receive Screen on/off broadcast msgs */
		BroadcastReceiver mReceiver = new ScreenReceiver();
		registerReceiver(mReceiver, filter);
	}

	/**
	 * Listen to temperature sensor accuracy changes
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

		switch (event.sensor.getType()) {
			case Sensor.TYPE_AMBIENT_TEMPERATURE:
				lastTempValue = Math.round(event.values[0] * 1000.0f) / 1000.0f;
				break;
			case Sensor.TYPE_PRESSURE:
				lastPressValue = Math.round(event.values[0] * 1000.0f) / 1000.0f;
				break;
			case Sensor.TYPE_RELATIVE_HUMIDITY:
				lastHumidValue = Math.round(event.values[0] * 1000.0f) / 1000.0f;
				break;
		}
		if (lastTempValue != -9999 && lastPressValue != -9999 && lastHumidValue != -9999)
		{
			if (BuildConfig.DEBUG) Log.d(LOG_TAG, "writing triggered: lastTempValue = "+lastTempValue
					+" lastPressValue = "+lastPressValue
					+" lastHumidValue = "+lastHumidValue);

			/** Integer array for return values */
			int[] currTime = Utils.getCurrentDate();
			if (BuildConfig.DEBUG) Log.d(LOG_TAG, "event timestamp "+currTime[0]+"h "
					+" on "+currTime[1]+" of month "+currTime[2]);

			if (currTime[0] == 0) { // it is 12am or 0h, so we shift the recorded days and then try to save the records
				if (!isShiftDone) { // We did not yet the shift of recorded days
					wsDbHelper = new WSDatabaseHelper(this);
					dataBase = wsDbHelper.getWritableDatabase();
					wsDbHelper.shiftDays(dataBase);
					dataBase.close();
					wsDbHelper.close();
					isShiftDone = true;
				}
			}
			/** Flag for addDayToDB success or failure */
			boolean result = addDayToDB(currTime[0], currTime[1], lastTempValue, lastPressValue, lastHumidValue);
			if (result) {
				if (mTempSensor != null) {
					mSensorManager.unregisterListener(this, mTempSensor);
				}
				if (mPressSensor != null) {
					mSensorManager.unregisterListener(this, mPressSensor);
				}
				if (mHumidSensor != null) {
					mSensorManager.unregisterListener(this, mHumidSensor);
				}
				stopSelf();
			} else {
				// retry until retry counter reaches 20
				lastTempValue = lastPressValue = lastHumidValue = -9999;
				if (retryCounter++ == 20) {
					if (mTempSensor != null) {
						mSensorManager.unregisterListener(this, mTempSensor);
					}
					if (mPressSensor != null) {
						mSensorManager.unregisterListener(this, mPressSensor);
					}
					if (mHumidSensor != null) {
						mSensorManager.unregisterListener(this, mHumidSensor);
					}
					stopSelf();
				}
			}
		}
	}

	/**
	 * Write measurements into data base
	 * gets all existing measurements, generates max, min and average values
	 * adds all values into data base
	 *
	 * @param timeStamp
	 *            hour of measurement
	 * @param dayStamp
	 *            hour of measurement
	 * @param currTemp
	 *            measured temperature
	 * @param currPress
	 *            measured pressure
	 * @param currHumid
	 *            measured humidity
	 * @return  <code>boolean</code>
	 *            true - addDayToDB success
	 *            false - addDayToDB failed
	 */
	private boolean addDayToDB (int timeStamp, int dayStamp,
	                         float currTemp, float currPress, float currHumid) {

		/** Array to hold existing temperature values of today */
		ArrayList<Float> tempsOfDay = new ArrayList<>();
		/** Array to hold existing pressure values of today */
		ArrayList<Float> pressOfDay = new ArrayList<>();
		/** Array to hold existing humidity values of today */
		ArrayList<Float> humidOfDay = new ArrayList<>();

		wsDbHelper = new WSDatabaseHelper(this);
		dataBase = wsDbHelper.getWritableDatabase();

		/** Cursor filled with existing entries of today */
		Cursor dayEntry = wsDbHelper.getDay(dataBase, 1);
		// Mapping of values:
		// "ts", "ds", "dn", "t", "p", "h",	"mat", "mit", "avt", "map", "mip", "avp", "mah", "mih", "avh"
		// 0     1     2     3    4    5    6       7      8      9      10    11     12     13     14

		if (dayEntry.getCount() != 0) {
			dayEntry.moveToLast();
			if (dayEntry.getInt(0) == timeStamp) { // we wrote already a record for this timestamp
				dayEntry.close();
				dataBase.close();
				wsDbHelper.close();
				return true;
			}
		}
		dayEntry.moveToFirst();
		for (int i = 0; i<dayEntry.getCount(); i++) {
			tempsOfDay.add(dayEntry.getFloat(3));
			pressOfDay.add(dayEntry.getFloat(4));
			humidOfDay.add(dayEntry.getFloat(5));
			dayEntry.moveToNext();
		}
		dayEntry.close();
		tempsOfDay.add(currTemp);
		pressOfDay.add(currPress);
		humidOfDay.add(currHumid);
		/** New calculated max values for today's temperatures */
		float currMaxTemp = Collections.max(tempsOfDay);
		/** New calculated min values for today's temperatures */
		float currMinTemp = Collections.min(tempsOfDay);
		/** New calculated average values for today's temperatures */
		float currAvgTemp = (float) calculateAverage(tempsOfDay);
		/** New calculated max values for today's pressures */
		float currMaxPress = Collections.max(pressOfDay);
		/** New calculated min values for today's pressures */
		float currMinPress = Collections.min(pressOfDay);
		/** New calculated average values for today's pressures */
		float currAvgPress = (float) calculateAverage(pressOfDay);
		/** New calculated max values for today's humidity */
		float currMaxHumid = Collections.max(humidOfDay);
		/** New calculated min values for today's humidity */
		float currMinHumid = Collections.min(humidOfDay);
		/** New calculated average values for today's humidity */
		float currAvgHumid = (float) calculateAverage(humidOfDay);

		/** Result of database operation */
		boolean result = wsDbHelper.addDay(dataBase, timeStamp, dayStamp, 1,
				currTemp, currPress, currHumid,
				currMaxTemp, currMinTemp, currAvgTemp,
				currMaxPress, currMinPress, currAvgPress,
				currMaxHumid, currMinHumid, currAvgHumid);
		dataBase.close();
		wsDbHelper.close();
		return result;

	}

	/**
	 * Calculate float average of all float entries in a list
	 *
	 * @param marks
	 *            list we want to have the average from
	 * @return <code>sum</code>
	 *            the average of all values in the list
	 */
	private double calculateAverage(List<Float> marks) {
		/** Sum of all entries in marks */
		double sum = 0f;
		/** Average of all entries in marks */
		double shortAverage = 0f;
		if(!marks.isEmpty()) {
			for (Float mark : marks) {
				sum += mark;
			}

			shortAverage = sum / marks.size();
		}
		return shortAverage;
	}
}
