package baybuddies.hackday.com.baybuddies;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

  private static final String TAG = "MainActivity";
  private GoogleApiClient mGoogleApiClient;
  private LocationRequest mLocationRequest;
  private String mLastUpdateTime;
  private String emailAddress;

  private static String[] PERMISSIONS_LOCATION = {android.Manifest.permission.ACCESS_FINE_LOCATION,
      android.Manifest.permission.ACCESS_COARSE_LOCATION};
  private static final int REQUEST_LOCATION = 1;

  private String imgUrl;
  private String modifiedEmailAddress;
  private String name;

  private String publicurl;

  DatabaseReference mDatabase;

  private RecyclerView mRecyclerView;
  private MyRecyclerAdapter adapter;
  private ProgressBar progressBar;

  private Person own;
  private List<Person> list;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.nearby_friends_find);

    // Initialize recycler view
    mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    progressBar = (ProgressBar) findViewById(R.id.progress_bar);
    progressBar.setVisibility(View.VISIBLE);


    mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(LocationServices.API)
        .build();

    mDatabase = FirebaseDatabase.getInstance().getReference();
    loadProfileDetails();
    fetchDataFromServer();

    list = new ArrayList<>();
  }


  private void loadProfileDetails() {
    SharedPreferences prefs = getSharedPreferences(Constants.MyPREFERENCES, Context.MODE_PRIVATE);
    own = new Person();
    imgUrl = prefs.getString(Constants.Image, "");
    emailAddress = prefs.getString(Constants.Email, "");
    own.email = emailAddress;
    name = prefs.getString(Constants.Name, "");
    publicurl = prefs.getString(Constants.Profile, "");
    modifiedEmailAddress = emailAddress.replace("@", "-");
    modifiedEmailAddress = modifiedEmailAddress.replace(".", "-");

    own.imageurl = imgUrl;
    own.publicurl = publicurl;
    own.name = name;


  }

  @Override
  protected void onStart() {
    super.onStart();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mGoogleApiClient.isConnected()) {
      mGoogleApiClient.disconnect();
    }
  }

  private static Double toRad(Double value) {
    return value * Math.PI / 180;
  }

  private boolean isNearByPerson(Person own, Person other, double threshold) {

    final double R = 6372.8;
    double dLat = Math.toRadians(other.lat - own.lat);
    double dLon = Math.toRadians(other.longi - own.longi);
    own.lat = Math.toRadians(own.lat);
    other.lat = Math.toRadians(other.lat);

    double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(own.lat) * Math.cos(other.lat);
    double c = 2 * Math.asin(Math.sqrt(a));
    double distance = R * c;
    Log.d("TAG", "Distance = " + distance);
    if (distance < threshold) { return true; }
    return false;

    /*
    final int R = 6371;
    Double latDistance = toRad(own.lat - other.lat);
    Double lonDistance = toRad(own.longi - other.longi);
    Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(toRad(own.lat)) * Math.cos(toRad(other.lat)) * Math.sin(lonDistance / 2) * Math.sin(
        lonDistance / 2);
    Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    Double distance = R * c;
*/
  }


  private boolean isNearUser(Person own, Person other, double threshold)
  {
    Location loc1 = new Location("");
    loc1.setLatitude(own.lat);
    loc1.setLongitude(own.longi);

    Location loc2 = new Location("");
    loc2.setLatitude(other.lat);
    loc2.setLongitude(other.longi);

    float distanceInMeters = loc1.distanceTo(loc2);
    Log.d("TAG", "Distance between "+ own.email + " and "+other.email+ " is" +distanceInMeters);
    if(distanceInMeters<=threshold)
      return true;
    return false;
  }

  private void loadData() {
    // Download complete. Let us update UI
    progressBar.setVisibility(View.GONE);
    adapter = new MyRecyclerAdapter(LocationActivity.this, list);
    mRecyclerView.setAdapter(adapter);
  }

  private void fetchData(DataSnapshot dataSnapshot) {
    list.clear();
    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {

      Person other = new Person();
      other.imageurl = (String) dataSnapshot1.child("image").getValue();
      other.name = (String) dataSnapshot1.child("name").getValue();
      other.publicurl = (String) dataSnapshot1.child("publicurl").getValue();
      other.email = (String) dataSnapshot1.child("email").getValue();
      other.lat = (Double) dataSnapshot1.child("lat").getValue();
      other.longi = (Double) dataSnapshot1.child("long").getValue();

      if(other.email != null)
      {
        if (!other.email.equals(own.email) && isNearUser(own, other, 150)) {
          Log.d("TAG", "this user is inside = " + other.email);
          list.add(other);
        } else {
          Log.d("TAG", "this user is outside = " + other.email);
        }
      }
      else {
        Log.d("TAG", "Email is null ");
      }
    }

    if (list.size() > 0) {
      loadData();
    }
  }


  private void fetchDataFromServer() {
    mDatabase.addChildEventListener(new ChildEventListener() {
      @Override
      public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        fetchData(dataSnapshot);
      }

      @Override
      public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        fetchData(dataSnapshot);
      }

      @Override
      public void onChildRemoved(DataSnapshot dataSnapshot) {

      }

      @Override
      public void onChildMoved(DataSnapshot dataSnapshot, String s) {

      }

      @Override
      public void onCancelled(DatabaseError databaseError) {

      }

    });
  }

  @Override
  public void onConnected(Bundle bundle) {
    mLocationRequest = LocationRequest.create();
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    mLocationRequest.setInterval(10000);
    mLocationRequest.setFastestInterval(3000);
    Log.i(TAG, "Connection Connected");

    if (ActivityCompat.checkSelfPermission(this,
                                           android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


      Log.i(TAG, "Contact permissions has NOT been granted. Requesting permissions.");
      requestLocationPermissions();


      return;
    }
    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    Log.i(TAG, "requestLocationUpdates ");
  }


  /**
   * Requests the Contacts permissions.
   * If the permission has been denied previously, a SnackBar will prompt the user to grant the
   * permission, otherwise it is requested directly.
   */
  private void requestLocationPermissions() {
    // BEGIN_INCLUDE(contacts_permission_request)
    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                            android.Manifest.permission.ACCESS_FINE_LOCATION)
        || ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                               android.Manifest.permission.ACCESS_COARSE_LOCATION)) {

      // Provide an additional rationale to the user if the permission was not granted
      // and the user would benefit from additional context for the use of the permission.
      // For example, if the request has been denied previously.
      Log.i(TAG,
            "Displaying contacts permission rationale to provide additional context.");

      ActivityCompat
          .requestPermissions(LocationActivity.this, PERMISSIONS_LOCATION,
                              REQUEST_LOCATION);


    } else {
      // Contact permissions have not been granted yet. Request them directly.
      ActivityCompat
          .requestPermissions(LocationActivity.this, PERMISSIONS_LOCATION,
                              REQUEST_LOCATION);
    }
    // END_INCLUDE(contacts_permission_request)
  }


  /**
   * Callback received when a permissions request has been completed.
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {

    if (requestCode == REQUEST_LOCATION) {
      Log.i(TAG, "Received response for location permissions request.");

      // We have requested multiple permissions for contacts, so all of them need to be
      // checked.
      if (verifyPermissions(grantResults)) {
        Log.i(TAG, "Contacts permissions granted.");

        if (ActivityCompat.checkSelfPermission(this,
                                               android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
          // TODO: Consider calling
          //    ActivityCompat#requestPermissions
          // here to request the missing permissions, and then overriding
          //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
          //                                          int[] grantResults)
          // to handle the case where the user grants the permission. See the documentation
          // for ActivityCompat#requestPermissions for more details.
          return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
      } else {
        Log.i(TAG, "Contacts permissions were NOT granted.");

      }

    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }


  public static boolean verifyPermissions(int[] grantResults) {
    // At least one result must be checked.
    if (grantResults.length < 1) {
      return false;
    }

    // Verify that each required permission has been granted, otherwise return false.
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }


  @Override
  public void onConnectionSuspended(int i) {
    Log.i(TAG, "Connection Suspended");
    mGoogleApiClient.connect();
  }

  @Override
  public void onLocationChanged(Location location) {

    // upload to firebase

    own.lat = location.getLatitude();
    own.longi = location.getLongitude();


    mDatabase.child("user").child(modifiedEmailAddress).child("name").setValue(name);
    mDatabase.child("user").child(modifiedEmailAddress).child("image").setValue(imgUrl);
    mDatabase.child("user").child(modifiedEmailAddress).child("lat").setValue(location.getLatitude());
    mDatabase.child("user").child(modifiedEmailAddress).child("long").setValue(location.getLongitude());
    mDatabase.child("user").child(modifiedEmailAddress).child("publicurl").setValue(publicurl);
    mDatabase.child("user").child(modifiedEmailAddress).child("email").setValue(emailAddress);

    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
    Toast.makeText(this, "Updated: " + mLastUpdateTime, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
  }
}
