package course.labs.locationlab;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class PlaceViewActivity extends ListActivity implements LocationListener {
	private static final long FIVE_MINS = 5 * 60 * 1000;

	private static String TAG = "Lab-Location";

	private Location mLastLocationReading;
	private PlaceViewAdapter mAdapter;

	// default minimum time between new readings
	private long mMinTime = 5000;

	// default minimum distance between old and new readings.
	private float mMinDistance = 1000.0f;

	private LocationManager mLocationManager;

	// A fake location provider used for testing
	private MockLocationProvider mMockLocationProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
			
		// Set up the location manager
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		// Set up the app's user interface
		// This class is a ListActivity, so it has its own ListView
		// ListView's adapter should be a PlaceViewAdapter
		mAdapter = new PlaceViewAdapter(getApplicationContext());

		// add a footerView to the ListView
		// You can use footer_view.xml to define the footer
		ListView mListView = getListView();

		// Add divider between each entry
		mListView.setFooterDividersEnabled(true);

		// Inflate footerView for footer_view.xml file
		LayoutInflater inflater = LayoutInflater.from(PlaceViewActivity.this); // 1
		TextView footerView = (TextView)inflater.inflate(R.layout.footer_view, null);

		// Add footerView to ListView
		mListView.addFooterView(footerView);
		
		footerView.setOnClickListener(new OnClickListener() {

			@Override 
			public void onClick(View v){

				// When the footerView's onClick() method is called, it must issue the
				// following log call				
				log("Entered footerView.OnClickListener.onClick()");

				boolean boolLocationDataAvailableAndFresh = (mLastLocationReading != null) && (age(mLastLocationReading) <= FIVE_MINS);
				
				// check that this is new location in the list.
				if (boolLocationDataAvailableAndFresh){
				// footerView must respond to user clicks.
				// Must handle 3 cases:
					
					// Now that we have downloaded the data, check if the location is currently in our list.
					boolean boolLocationInList = mAdapter.intersects(mLastLocationReading);									
										
				if (!boolLocationInList){
					
					log("Starting Place Download");
						
						new PlaceDownloaderTask(PlaceViewActivity.this).execute(mLastLocationReading);

				}// end if

				else{
					// 2) The current location has been seen before - issue Toast message.
					// Issue the following log call:

					log("You already have this location badge");
					Toast.makeText(v.getContext(), "You already have this location badge", Toast.LENGTH_LONG).show();

				}// end else

				}// end if
				// 3) There is no current location - response is up to you. The best
				// solution is to disable the footerView until you have a location.
				// Issue the following log call:
				else{
					
					log("Location data is not available");
					Toast.makeText(v.getContext(), "Sorry, Location data not available. Try again later.", Toast.LENGTH_LONG).show();
					
				}// end else


			}// end onItemClick

		});// end onClickListener

		// M: Attach the adapter to this ListActivity's ListView
		this.setListAdapter(mAdapter);
		
	}

	@Override
	protected void onResume() {
		super.onResume();

		/// M: Creates a mock location provider implementing the addTestProvider function.
		// in the MockLocationProvider function the addTestProvider function is called and 
		// used to create a new custom type of location provider for testing purposes.
		mMockLocationProvider = new MockLocationProvider(
				LocationManager.NETWORK_PROVIDER, this);

		// Check NETWORK_PROVIDER for an existing location reading.
		// Only keep this last reading if it is fresh - less than 5 minutes old.
		// M: Check if age is greater than 5 mins
		mLastLocationReading = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (age(mLastLocationReading) > FIVE_MINS){
		
			// Age of last reading is too old, discard.
			mLastLocationReading = null;
			
		}// end if
		

		// register to receive location updates from NETWORK_PROVIDER
		// M: The location manager is instantiated already, so next step is to register for
		// network location updates.
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance,(LocationListener)this);


	}

	@Override
	protected void onPause() {

		mMockLocationProvider.shutdown();

		// unregister for location updates
		mLocationManager.removeUpdates((LocationListener)this);


		super.onPause();
	}

	// Callback method used by PlaceDownloaderTask
	public void addNewPlace(PlaceRecord place) {

		log("Entered addNewPlace()");
		mAdapter.add(place);

	}

	@Override
	public void onLocationChanged(Location currentLocation) {

		// Handle location updates
		// Cases to consider
		// 1) If there is no last location, keep the current location.
		if(mLastLocationReading == null){
		
			// M: Update the lastLocationReading to be the current location
			mLastLocationReading = currentLocation;
			
		}// end mLastLocationReading == null
		else{
		
			// 2) If the current location is older than the last location, ignore
			// the current location
			// 3) If the current location is newer than the last locations, keep the
			// current location.
			if (age(mLastLocationReading) > age(currentLocation)){
				mLastLocationReading = currentLocation; // M: Update the lastLocationReading to be the current location
			}
			else{
				// do nothing. The LastLocation is newer than the currentLocation
			}

		}// end else


	}

	@Override
	public void onProviderDisabled(String provider) {
		// not implemented
	}

	@Override
	public void onProviderEnabled(String provider) {
		// not implemented
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// not implemented
	}

	private long age(Location location) {
		if (location !=null)
			return System.currentTimeMillis() - location.getTime();
		else
			return FIVE_MINS*2;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.print_badges:
			ArrayList<PlaceRecord> currData = mAdapter.getList();
			for (int i = 0; i < currData.size(); i++) {
				log(currData.get(i).toString());
			}
			return true;
		case R.id.delete_badges:
			mAdapter.removeAllViews();
			return true;
		case R.id.place_one:
			mMockLocationProvider.pushLocation(37.422, -122.084);
			return true;
		case R.id.place_invalid:
			mMockLocationProvider.pushLocation(0, 0);
			return true;
		case R.id.place_two:
			mMockLocationProvider.pushLocation(38.996667, -76.9275);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private static void log(String msg) {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.i(TAG, msg);
	}

}