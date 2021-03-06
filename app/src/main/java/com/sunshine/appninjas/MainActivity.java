package com.sunshine.appninjas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import com.codepath.asynchttpclient.AsyncHttpClient;
import com.codepath.asynchttpclient.RequestParams;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Headers;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import android.util.Log;
import android.Manifest;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private final int REQUEST_LOCATION_PERMISSION = 1;
    String apiKey;
    double latitude;
    double longitude;
    int start_month;
    int start_year;

    int end_month;
    int end_year;
    ApiClient apiclient;
    GraphView graph;

    EditText startDate,endDate;
    public String startDateStr, endDateStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestLocationPermission();
        String current_location = get_location();



//        Log.i("Current location:", current_location);


        if (!isNetworkAvailable()){
            Log.i("Internet connection", "Please check your internet connectivity");
            Toast.makeText(this, "Please check your internet connectivity",
                    Toast.LENGTH_SHORT).show();
        }


        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.api_key));
        }
        PlacesClient placesClient = Places.createClient(this);

        apiclient=new ApiClient();

        Date presentdate=new Date();
        start_year=2019;
        end_year=2020;

        Spinner spinner1 = (Spinner) findViewById(R.id.feature);
        spinner1.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.feature_array, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter);

        Spinner spinner2 = (Spinner) findViewById(R.id.granularity);
        spinner2.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.granuality, android.R.layout.simple_spinner_dropdown_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);

        startDate = (EditText)findViewById(R.id.startDate);
        endDate = (EditText)findViewById(R.id.endDate);

        String todayDate = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(new Date());

        endDateStr = getTodaysDate();
        startDateStr = getPreviousYearDate();
        endDate.setText(endDateStr);
        startDate.setText(startDateStr);
        Log.i("DATES", startDateStr + " " + endDateStr);

        startDate.setOnClickListener(this);
        endDate.setOnClickListener(this);

        updateGraph();

    }

    private void updateGraph(){
        RequestParams requestParams = apiclient.GetParams(latitude,longitude,start_month,start_year,end_month,end_year);
        AsyncHttpClient client = new AsyncHttpClient();
        System.out.println(requestParams.get("latitude"));
        System.out.println(requestParams.get("longitude"));
        client.get(ApiClient.host, requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {

                Log.i("JSON OUTPUT SUCCESS",json.toString());

                JSONObject jsonObject= json.jsonObject;
                try {
                    JSONObject propertiesObj = jsonObject.getJSONObject("properties");
                    JSONObject parametersObj= propertiesObj.getJSONObject("parameter");
                    JSONObject allskyObj=parametersObj.getJSONObject("ALLSKY_SFC_SW_DWN");
                    //System.out.println(allskyObj.length());

                    Iterator<String> keysIterator=allskyObj.keys();
                    ArrayList<String> keyslist=new ArrayList<>();


                    while(keysIterator.hasNext())
                    {   String date=keysIterator.next();
                        //String date_formatted=ParseDate(date);

                        keyslist.add(date);
                    }
                    System.out.println((keyslist.size()));
                    DataPoint[] dp = new DataPoint[keyslist.size()-1];
                    HashMap<Date,Double> map=new HashMap<Date, Double>();
                    ArrayList<Double>avgList=new ArrayList<>();
                    for(int i=0;i<keyslist.size()-1;i++)
                    {


                        String key= keyslist.get(i);
                        Date date1= null;
                        try {
                            date1 = new SimpleDateFormat("yyyyMM").parse(key);
                            Double value=Double.parseDouble(allskyObj.getString(key));
                            dp[i]=new DataPoint(date1,value);
                            map.put(date1,value);

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    graph = (GraphView) findViewById(R.id.graph);
                    graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);


                    LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dp);
                    //graph.removeAllSeries();
                    graph.addSeries(series);

                    //StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
                    //staticLabelsFormatter.setHorizontalLabels(new String[] {"old", "middle", "new"});
                    graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getApplicationContext()));
                    graph.getGridLabelRenderer().setNumHorizontalLabels(3); // only 4 because of the space

// set manual x bounds to have nice steps
                    //  graph.getViewport().setMinX(d1.getTime());
                    //graph.getViewport().setMaxX(d3.getTime());
                    graph.getViewport().setXAxisBoundsManual(true);
                    series.setTitle("Solar Irradiance against time");

                    graph.getGridLabelRenderer().setHorizontalAxisTitle("Date");
                    graph.getGridLabelRenderer().setVerticalAxisTitle("Solar Radiance (" + Html.fromHtml("W/m<sup>2</sup>)"));
                    graph.getViewport().setScalable(true);
                    graph.getViewport().setScalableY(true);




// as we use dates as labels, the human rounding to nice readable numbers
// is not necessary
                    graph.getGridLabelRenderer().setHumanRounding(false);
                    for (Map.Entry<Date,Double> entry : map.entrySet()) {
                        System.out.println(entry.getKey()+" : "+entry.getValue());
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                if (headers == null){
                    Log.e("JSON Output Failure", "Network is unavailable, no repsonse fromthe server");
                }else{
                    Log.e("JSON OUTPUT FAILURE",headers.toString());
                }
            }
        });
    }

    private void updateGraphdaily(){
        RequestParams requestParams = apiclient.GetParams(latitude,longitude,start_month,start_year,end_month,end_year);
        AsyncHttpClient client = new AsyncHttpClient();
        System.out.println(requestParams.get("latitude"));
        System.out.println(requestParams.get("longitude"));
        client.get(ApiClient.host_weekly, requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {

//        if (current_location==null || current_location.isEmpty()) {
//            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
//        }else{
//            autocompleteFragment.setText(current_location);
//        }

                Log.i("JSON OUTPUT SUCCESS",json.toString());

                JSONObject jsonObject= json.jsonObject;
                try {
                    JSONObject propertiesObj = jsonObject.getJSONObject("properties");
                    JSONObject parametersObj= propertiesObj.getJSONObject("parameter");
                    JSONObject allskyObj=parametersObj.getJSONObject("ALLSKY_SFC_SW_DWN");
                    System.out.println(allskyObj.length());

                    Iterator<String> keysIterator=allskyObj.keys();
                    ArrayList<String> keyslist=new ArrayList<>();

                    while(keysIterator.hasNext())
                    {   String date=keysIterator.next();
                        //String date_formatted=ParseDate(date);
                        keyslist.add(date);
                    }
                    System.out.println((keyslist.size()));
                    DataPoint[] dp = new DataPoint[5];
                    SortedMap<Date,Double> map=new TreeMap<Date, Double>();
                    ArrayList<Double>avgList=new ArrayList<>();
                    for(int i=0;i<5;i++)
                    {


                        String key= keyslist.get(i);
                        Date date1= null;
                        try {
                            date1 = new SimpleDateFormat("yyyyMMdd").parse(key);
                            System.out.println(date1);
                            Double value=Double.parseDouble(allskyObj.getString(key));
                            System.out.println(value);
                            dp[i]=new DataPoint(date1,value);
                            map.put(date1,value);

                        } catch (ParseException e) {
                            System.out.println("ninj");
                            e.printStackTrace();
                        }
                    }
                    graph = (GraphView) findViewById(R.id.graph);
                    graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);

                    LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dp);
                    graph.refreshDrawableState();
                    graph.addSeries(series);

                    //StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
                    //staticLabelsFormatter.setHorizontalLabels(new String[] {"old", "middle", "new"});
                    graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getApplicationContext()));
                    graph.getGridLabelRenderer().setNumHorizontalLabels(3); // only 4 because of the space

// set manual x bounds to have nice steps
                    //  graph.getViewport().setMinX(d1.getTime());
                    //graph.getViewport().setMaxX(d3.getTime());
                    graph.getViewport().setXAxisBoundsManual(true);
                    series.setTitle("Solar Irradiance against time");

                    graph.getGridLabelRenderer().setHorizontalAxisTitle("Date");
                    graph.getGridLabelRenderer().setVerticalAxisTitle("Solar Radiance" + Html.fromHtml("W/m<sup>2</sup>"));
                    graph.getViewport().setScalable(true);
                    graph.getViewport().setScalableY(true);




// as we use dates as labels, the human rounding to nice readable numbers
// is not necessary
                    graph.getGridLabelRenderer().setHumanRounding(false);



                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e("JSON OUTPUT FAILURE",headers.toString());

            }

        });
    }

    private String ParseDate( String date)
    {
        String year=date.substring(0,4);
        String year_formatted=year.substring(2);
        String month=date.substring(4);
        String formatted=month+"/"+year_formatted;
        return formatted;
    }

    private static String getTodaysDate(){
        String todayDate = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(new Date());
        return todayDate;
    }

    private void alertDialog() {
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setMessage("Please turn on the GPS");
        dialog.setTitle("Dialog Box");
        dialog.setPositiveButton("Okay",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
//                       Toast.makeText(getApplicationContext(),"Yes is clicked",Toast.LENGTH_LONG).show();
                        Log.i("Dialog Box", "Clicked on okay");
                    }
                });

        AlertDialog alertDialog=dialog.create();
        alertDialog.show();
    }


    private static String getPreviousYearDate() {

        Calendar calendarEnd=Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");

        // You can substract from the current Year to get the previous year last dates.
        calendarEnd.set(Calendar.YEAR,calendarEnd.get(Calendar.YEAR)-1);
        Date previousDate = calendarEnd.getTime();
        String result = dateFormat.format(previousDate);
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(EasyPermissions.hasPermissions(this, perms)) {
            //Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
//            Log.i("Permission is granted");
            Log.d("Current location", "Request is granted");
        }
        else {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public String get_location(){

        //Checking for location permissions
        if (check_permission(1)) {
            GPSTrack gps;
            gps = new GPSTrack(MainActivity.this);
             latitude = gps.getLatitude();
             longitude = gps.getLongitude();

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses.size() == 0){
//                    Toast.makeText(getApplicationContext(),
//                                        "Please turn on the GPS" ,
//                                        Toast.LENGTH_LONG)
//                                        .show();
                    alertDialog();
                    return null;
                }

                System.out.println("latt" + latitude);
                System.out.println("longitude"+longitude);
                System.out.println("****");
                System.out.println("Address" + addresses.size());

                System.out.println("Address" + addresses.get(0).getAddressLine(0));

                AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                        getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

                if (!Places.isInitialized()) {
                    Places.initialize(getApplicationContext(), getString(R.string.api_key));
                }

                autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.PHOTO_METADATAS,Place.Field.LAT_LNG,Place.Field.ADDRESS));
                autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                    @Override
                    public void onPlaceSelected(@NonNull Place place) {
                        String address1 = place.getAddress();
                        LatLng latlng=place.getLatLng();
                        latitude = latlng.latitude;
                        longitude = latlng.longitude;
                        //Log.i("location",address1);

                        System.out.println("long sai" + longitude);
                        //Double[] coordinates = get_coordinates(address1);
                       // autocompleteFragment.setText(address1);
                      //  latitude = coordinates[0];
                      //  longitude = coordinates[1];

                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = geocoder.getFromLocationName(address1, 1);
                            Address address = addresses.get(0);
                             longitude = address.getLongitude();
                             latitude = address.getLatitude();




                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        graph.removeAllSeries();
                        updateGraph();
                    }

                    @Override
                    public void onError(@NonNull Status status) {

                    }
                });


                String fullAddress = addresses.get(0).getAddressLine(0);
                autocompleteFragment.setText(fullAddress);
                return fullAddress;

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Location", "Error while fetching location from GPS");
            }
        }else{
            System.out.println("GPS Permission is not provided!");
        }

        return null;
    }

    public Double[] get_coordinates(String myLocation)
    {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocationName(myLocation, 1);
            Address address = addresses.get(0);
            double longitude = address.getLongitude();
            double latitude = address.getLatitude();

            return new Double[]{latitude,longitude};
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Double[]{0.0,0.0};
    }


    public boolean check_permission(int permission){
        switch(permission){
            case 1:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

//            case 2:
//                return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
       // Log.i("Permission :", str(permission),  "is not found");
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {

        Resources res = getResources();

        switch(adapterView.getId()){
            case R.id.feature:
                String[] featuresList = res.getStringArray(R.array.feature_array);
                Log.i("Array", "feature list:" + Arrays.toString(featuresList) + "position: " + position);
                break;

            case R.id.granularity:
                String[] granualarityList = res.getStringArray(R.array.granuality);
                Log.i("granualarityList toast", "Selected granualaity: "
                        + granualarityList[position]);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public static boolean isBeforeStartDate(String startDateStr, String endDateStr) {
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        boolean b = false;


        try {
            b = dfDate.parse(startDateStr).before(dfDate.parse(endDateStr)) || dfDate.parse(startDateStr).equals(dfDate.parse(endDateStr));
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return b;
    }

    @Override
    public void onClick(View view) {
        System.out.println("In onClick button");

        if (view == endDate) {
            final Calendar c = Calendar.getInstance();
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year,
                                              int monthOfYear, int dayOfMonth) {
                            String selectedDate = (monthOfYear + 1) + "-" + dayOfMonth + "-" + year;

                            endDateStr = selectedDate;
                            endDate.setText(endDateStr);

//                            if(!isBeforeStartDate(startDateStr, selectedDate)){
//
//                                Toast.makeText(getApplicationContext(),
//                                        "End date can't be less than Start date" + "Start DAte" + startDateStr + "end Date" + selectedDate,
//                                        Toast.LENGTH_LONG)
//                                        .show();
//                                Log.e("date", "End date can't be less than Start date.!!");
//                                Log.i("Date selection", "Inside end dateStart DAte" + startDateStr + "end Date" + endDateStr);
//
//
//                            }else{
//                                endDateStr = selectedDate;
//                                endDate.setText(endDateStr);
//                                Log.i("date", "Inside ondate set..!!");
//                            }
                        }
                    }, mYear, mMonth, mDay);
            DatePicker datePicker = datePickerDialog.getDatePicker();
            Calendar calendar = Calendar.getInstance();//get the current day
            datePicker.setMaxDate(calendar.getTimeInMillis());//set the current day as the max date
            datePickerDialog.show();
        }

        if (view == startDate) {
            final Calendar c = Calendar.getInstance();
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year,
                                              int monthOfYear, int dayOfMonth) {

                            String selectedDate = (monthOfYear + 1) + "-" + dayOfMonth + "-" + year;
                            Log.i("Date selection", "Inside start ,Start DAte" + selectedDate + "end Date" + endDateStr);

                            startDateStr = selectedDate;
                            startDate.setText(startDateStr);

//                            if(!isBeforeStartDate(selectedDate, endDateStr)){
//                                Toast.makeText(getApplicationContext(),
//                                "Start date can't be more than End date" + "Start DAte" + startDateStr + "end Date" + endDateStr,
//                                Toast.LENGTH_LONG)
//                                .show();
//                                Log.e("date", "Start date can't be more than End date.!!");
//                            }else {
//                                startDateStr = selectedDate;
//                                startDate.setText(startDateStr);
//                                Log.i("date", "Inside ondate set..!!");
//                            }

                        }
                    }, mYear, mMonth, mDay);
            DatePicker datePicker = datePickerDialog.getDatePicker();
            Calendar calendar = Calendar.getInstance();//get the current day
            datePicker.setMaxDate(calendar.getTimeInMillis());//set the current day as the max date
            datePickerDialog.show();
        }
    }
}

