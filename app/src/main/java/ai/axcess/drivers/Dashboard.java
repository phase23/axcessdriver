package ai.axcess.drivers;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.blue;

public class Dashboard extends AppCompatActivity {


    TextView driver;
    TextView shiftstate;
    TextView locationstate;
    Button shift;
    Button viewinorders;
    Button llogout;
    String responseLocation;
    String fname;
    String cunq;
    String isgpson;
    private int name;
    int newstate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        SharedPreferences shared = getSharedPreferences("autoLogin", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = shared.edit();
        int j = shared.getInt("key", 0);

        registerReceiver(gpsReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        if(j > 0) {
            fname = shared.getString("sendfname", "");
            cunq = shared.getString("driver", "");
        }else {
             fname = getIntent().getExtras().getString("sendfname");
              cunq = getIntent().getExtras().getString("driver");
        }

        shift = (Button)findViewById(R.id.Startshift);
        llogout = (Button)findViewById(R.id.logout);
        viewinorders = (Button)findViewById(R.id.vieworders);

        driver = (TextView)findViewById(R.id.drivername);
        shiftstate = (TextView)findViewById(R.id.whatshift);

        driver.setText(fname);

        getShift(cunq);
        checklocationstatus();


        llogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shared.edit().clear().commit();
                Intent intent = new Intent(Dashboard.this, MainActivity.class);

                startActivity(intent);

            }

        });




        shift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                        shiftstate.setText("Please wait..");
                        int returnstate = getState();
                        Shiftactionset(cunq, returnstate);
                       checklocationstatus();

            }

        });


    }


    public void getShift(String cunq){

        String returnshift = Shiftaction(cunq );
        returnshift = returnshift.trim();

        int myNum = 0;
        try {
            myNum = Integer.parseInt(returnshift);
        } catch(NumberFormatException nfe) {
            System.out.println("Could not parse " + nfe);
        }


        System.out.println("shift url  " + myNum);
        setState(myNum);

        if(myNum == 0) {

            shiftstate.setText("Off Shift");

            shift.setBackgroundColor(Color.RED);

        }

        if(myNum == 1) {

            System.out.println("shift url  " + myNum);
            shift.setBackgroundColor(GREEN);
            shiftstate.setText("On Shift");


        }


    }


    public void Shiftactionset(String cunq, int State){
        String thisdevice = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        if(State == 0) {
            newstate = 1;
        }

        if(State == 1) {
            newstate = 0;
        }


        String url = "https://axcess.ai/barapp/driver_shiftaction.php?&action=changeshift&cunq="+cunq + "&changestate=" + newstate;
        Log.i("action url",url);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)

                .addFormDataPart("what","this" )

                .build();
        Request request = new Request.Builder()
                .url(url)//your webservice url
                .post(requestBody)
                .build();
        try {
            //String responseBody;
            okhttp3.Response response = client.newCall(request).execute();
            // Response response = client.newCall(request).execute();
            if (response.isSuccessful()){
                Log.i("SUCC",""+response.message());
            }
            String resp = response.message();
            responseLocation =  response.body().string().trim();
            Log.i("respBody:main",responseLocation);
            Log.i("MSG",resp);

            if(responseLocation.equals("updated")){
                setState(newstate);

                if(newstate == 0) {
                    shift.setBackgroundColor(RED);
                    shiftstate.setText("Off Shift");
                }

                if(newstate == 1) {
                    shift.setBackgroundColor(GREEN);
                    shiftstate.setText("On Shift");
                }



            }


        } catch (IOException e) {
            e.printStackTrace();
        }


    }//end function






    public String Shiftaction( String cunq ) {

        String thisdevice = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        String url = "https://axcess.ai/barapp/driver_shiftaction.php?&action=getshift&cunq="+cunq;
        Log.i("action url",url);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)

                .addFormDataPart("what","this" )

                .build();
        Request request = new Request.Builder()
                .url(url)//your webservice url
                .post(requestBody)
                .build();
        try {
            //String responseBody;
            okhttp3.Response response = client.newCall(request).execute();
            // Response response = client.newCall(request).execute();
            if (response.isSuccessful()){
                Log.i("SUCC",""+response.message());
            }
            String resp = response.message();
            responseLocation =  response.body().string();
            Log.i("respBody:main",responseLocation);
            Log.i("MSG",resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseLocation;
    }



    public int getState() {
        return name;
    }

    public void setState(int newName) {
        this.name = newName;
    }


    @Override
    public void onResume() {
        super.onResume();
        // This registers messageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("my-message"));
    }

    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String myout = intent.getStringExtra("send"); // -1 is going to be used as the default value
            if(myout.equals("redbtn")) {
                viewinorders.setBackgroundColor(RED);
            }

            if(myout.equals("whitebtn")) {
                viewinorders.setBackgroundColor(getResources().getColor(android.R.color.white));
            }



        }
    };

    private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                //Do your stuff on GPS status change


                new Helpers(context).checklocationstatus();
                Toast.makeText(getApplicationContext(), "GPS changed ", Toast.LENGTH_LONG).show();
            }
        }
    };



    public void checklocationstatus(){

        LocationManager lm = (LocationManager)getApplication().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            //new AlertDialog.Builder(context)
            Shiftactionset(cunq, 1);

            android.app.AlertDialog.Builder dialog = new AlertDialog.Builder(Dashboard.this);
            dialog.setCancelable(false);
            dialog.setTitle("GPS STATUS");
            dialog.setMessage("Your GPS is not enabled");
            dialog.setPositiveButton("Start GPS", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    //getApplication().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    Intent nointernet = new Intent(Dashboard.this, Startgps.class);
                    startActivity(nointernet);

                }
            })
                    .setNegativeButton("No ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Action for "Cancel".
                        }

                    });
            final AlertDialog alert = dialog.create();
            alert.show();


        }

    }




    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onPause();
    }




}