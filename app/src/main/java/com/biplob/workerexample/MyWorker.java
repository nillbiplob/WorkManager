package com.biplob.workerexample;

import static android.os.Looper.getMainLooper;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ForegroundInfo;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;
import com.pratikbutani.workerexample.R;

import java.util.List;
import java.util.Locale;

public class MyWorker extends Worker {


	private static final String TAG = "MyWorker";

	/**
	 * The desired interval for location updates. Inexact. Updates may be more or less frequent.
	 */
	private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000; // 1 seconds

	/**
	 * The fastest rate for active location updates. Updates will never be more frequent
	 * than this value.
	 */
	private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
			UPDATE_INTERVAL_IN_MILLISECONDS / 2;
	private static final int NOTIFICATIONID = 8001;
	/**
	 * The current location.
	 */
	private Location mLocation;
	private Location lastLocation;


	private static int counter = 0;
	PowerManager.WakeLock wakeLock;

	/**
	 * Provides access to the Fused Location Provider API.
	 */
	private FusedLocationProviderClient mFusedLocationClient;

	private Context mContext;

	private NotificationManager notificationManager;


	Handler myHandler = new Handler(Looper.getMainLooper());

	private static final String WORKERTAG = "LocationUpdate";


	private Runnable locationRunnable = new Runnable() {
		@Override
		public void run() {

			Log.e(TAG, "locationRunnable called ");

			performTheTask();

			if (mLocation != null) {
				try {
					Log.e(TAG, "sendNotification is called ");

					setForegroundAsync(sendNotification());
				}catch (Exception e)
				{

				}
			}

			counter++;
			Log.e(TAG, "locationRunnable Counter: " + counter);

			try {

				if (counter >= 30 || !isWorkScheduled(WorkManager.getInstance().getWorkInfosByTag(WORKERTAG).get())) {

					counter = 0;
					myHandler.removeCallbacksAndMessages(null);


				} else {

					myHandler.removeCallbacksAndMessages(null);
					myHandler.postDelayed(locationRunnable, 30000);

				}
			} catch (Exception ignored) {

			}


		}
	};

	/**
	 * Callback for changes in location.
	 */
	private LocationCallback mLocationCallback;

	public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
		mContext = context;

		PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"MyApp::MyWakelockTag");

		wakeLock.acquire();

		notificationManager = (NotificationManager)
				mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);


		getLastLocation();


	}

	private void getLastLocation() {

		if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
			@Override
			public void onComplete(@NonNull Task<Location> task) {

				mLocation=task.getResult();

				Log.e(TAG, "last known location " + mLocation.getLatitude()+","+mLocation.getLongitude());

			}
		});

	}

	private boolean isWorkScheduled(List<WorkInfo> workInfos) {
		boolean running = false;
		if (workInfos == null || workInfos.size() == 0) return false;
		for (WorkInfo workStatus : workInfos) {
			running = workStatus.getState() == WorkInfo.State.RUNNING | workStatus.getState() == WorkInfo.State.ENQUEUED;
		}
		return running;
	}


	@NonNull
	@Override
	public Result doWork() {

		Log.e(TAG, "doWork: STARTING JOB..");


		myHandler.post(locationRunnable);






		return Result.success();
	}


	@Override
	public void onStopped() {
		super.onStopped();

		wakeLock.release();
	}

	private void performTheTask() {


		try {



			mLocationCallback = new LocationCallback() {
				@Override
				public void onLocationResult(LocationResult locationResult) {
					super.onLocationResult(locationResult);


				}
			};

			LocationRequest mLocationRequest = new LocationRequest();
			mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
			mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
			mLocationRequest.setSmallestDisplacement(2F);

			mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


			try {
				mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, getMainLooper());
			} catch (SecurityException unlikely) {
				//Utils.setRequestingLocationUpdates(this, false);
				Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
			}


			if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				// TODO: Consider calling
				//    ActivityCompat#requestPermissions
				// here to request the missing permissions, and then overriding
				//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
				//                                          int[] grantResults)
				// to handle the case where the user grants the permission. See the documentation
				// for ActivityCompat#requestPermissions for more details.
				return;
			}
			mFusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationToken() {
				@NonNull
				@Override
				public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
					return null;
				}

				@Override
				public boolean isCancellationRequested() {
					return false;
				}
			}).addOnCompleteListener(new OnCompleteListener<Location>() {
				@Override
				public void onComplete(@NonNull Task<Location> task) {

					mLocation =  task.getResult();


					if(mLocation!=null) {
						Log.e(TAG, "Location : in mFusedLocationClient.getCurrentLocation " + mLocation.getLatitude() + "," + mLocation.getLongitude());

						if (lastLocation != null) {
							Float lastDistance = mLocation.distanceTo(lastLocation);
							Log.e(TAG, "Location : distance " + lastDistance);


						}
						lastLocation = mLocation;
					}

				}
			});







		} catch (Exception ignored) {

		}
	}



	private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
		String strAdd = "";
		Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
		try {
			List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
			if (addresses != null) {
				Address returnedAddress = addresses.get(0);
				StringBuilder strReturnedAddress = new StringBuilder();

				for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
					strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
				}
				strAdd = strReturnedAddress.toString()+"("+LATITUDE+","+LONGITUDE+")";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return strAdd;
	}

	private ForegroundInfo sendNotification()
	{

		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = mContext.getString(R.string.app_name);
			String description = mContext.getString(R.string.app_name);
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel(mContext.getString(R.string.app_name), name, importance);
			channel.setDescription(description);
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}

		//Define sound URI
		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, mContext.getString(R.string.app_name))
				.setSmallIcon(android.R.drawable.ic_menu_mylocation)
				.setContentTitle("Location Update")
				.setSound(soundUri)
				.setVibrate(new long[]{100,100})
				.setContentText("You are at " + getCompleteAddressString(mLocation.getLatitude(), mLocation.getLongitude()))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setStyle(new NotificationCompat.BigTextStyle().bigText("You are at " + getCompleteAddressString(mLocation.getLatitude(), mLocation.getLongitude())));


		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
		// notificationId is a unique int for each notification that you must define
		notificationManager.notify(NOTIFICATIONID, builder.build());

		return new ForegroundInfo(NOTIFICATIONID,builder.build());
	}


}
