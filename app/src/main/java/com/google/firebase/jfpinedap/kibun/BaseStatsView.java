package com.google.firebase.jfpinedap.kibun;

import android.Manifest;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import data.model.Notification;

public class BaseStatsView extends AppCompatActivity {

    private static final String TAG = "BaseStatsView";

    private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

    public static final int JOB_ID = 99887;

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;

    private String mUsername;

    private String NOTIFICATION_TITLE = "KIBUN";
    private String CONTENT_TEXT = "¿Cómo estás?";

    // Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersDatabaseReference;
    private ValueEventListener mValueEventListener;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    public static String PACKAGE_NAME;

    private LocationManager locationManager;

    private Location location;

    private JobScheduler mScheduler;

    private SharedPreferences mPrefs;


    FloatingActionMenu actionMenu;
    TextView mText;
    TextView mUserText;
    private ArrayList<Notification> mUserNotifications;
    private GraphView mGraph;
    private static final HashMap<String, Integer> moodOptions = new HashMap<>();
    static {
        moodOptions.put("vsad", 1);
        moodOptions.put("sad", 2);
        moodOptions.put("meh", 3);
        moodOptions.put("happy", 4);
        moodOptions.put("vhappy", 5);
    }
    private  enum ViewType {History, Week};
    private ViewType mViewType = ViewType.History;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats_view);

        // Preferencias del usuario
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Iniciar job para mostrar notificaciones
        iniciarJob();

        // Servicios de ubicación
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        }

        PACKAGE_NAME = getApplicationContext().getPackageName();

        mUsername = ANONYMOUS;

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mUsersDatabaseReference = mFirebaseDatabase.getReference().child("users");

        mText = (TextView)findViewById(R.id.text);
        mUserText = (TextView)findViewById(R.id.user_name);

        mUserNotifications = new ArrayList<>();
        actionMenu = (FloatingActionMenu)findViewById(R.id.floatingMenu);
        actionMenu.setClosedOnTouchOutside(true);
        setTitle("Tu Histórico");
        initializeFloatingMenuButtons();
        initializeGraph();
        mText.setText("");

        mAuthStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null){
                // user is signed in
                Toast.makeText(BaseStatsView.this, R.string.sing_in_lable, Toast.LENGTH_SHORT).show();
                onSignedInInitialize(user.getDisplayName());
            } else{
                // user is signed out
                onSignedOutCleanup();
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(false, true)
                                .setAvailableProviders(Arrays.asList(
                                        new AuthUI.IdpConfig.EmailBuilder().build(),
                                        new AuthUI.IdpConfig.GoogleBuilder().build(),
                                        new AuthUI.IdpConfig.FacebookBuilder().build(),
                                        new AuthUI.IdpConfig.TwitterBuilder().build()))
//                                            new AuthUI.IdpConfig.GitHubBuilder().build(),
//                                            new AuthUI.IdpConfig.PhoneBuilder().build(),
//                                            new AuthUI.IdpConfig.AnonymousBuilder().build()))
                                .setTosAndPrivacyPolicyUrls("https://kibun-db.firebaseapp.com/terms_and_conditions.html",
                        "https://kibun-db.firebaseapp.com/privacy_policy.html")
                                .setLogo(R.mipmap.ic_launcher)
                                .setTheme(R.style.BlueTheme)
                                .build(),
                        RC_SIGN_IN);
            }

        };
    }

    void initializeFloatingMenuButtons(){
        FloatingActionButton buttonWeek = (FloatingActionButton) findViewById(R.id.bSemana);

        buttonWeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setTitle("Semana ");
                mViewType = ViewType.Week;
                showInfo();
                actionMenu.close(true);

            }
        });

        FloatingActionButton buttonHistory = (FloatingActionButton) findViewById(R.id.bHistorico);

        buttonHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setTitle("Tu Histórico");
                mViewType = ViewType.History;
                showInfo();
                actionMenu.close(true);

            }
        });
    }

    void initializeGraph(){
        mGraph = (GraphView) findViewById(R.id.graph);

        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinY(1);
        mGraph.getViewport().setMaxY(5);
        switch (mViewType){
            case History:
                mGraph.getGridLabelRenderer().setHorizontalAxisTitle("Notificaciones");
                break;
            case Week:
                mGraph.getGridLabelRenderer().setHorizontalAxisTitle("Días de la semana");
                break;
        }
        mGraph.getGridLabelRenderer().setPadding(20);
        mGraph.getGridLabelRenderer().setVerticalAxisTitle("Estado de ánimo");
    }


    private void iniciarJob(){
        cancelarJob();
        int frecuencia = mPrefs.getInt("frecuencia", 4);
        if(frecuencia > 0) {
            mScheduler = (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(this, JobNotifications.class);
            long frecuenciaTiempo;
            if (frecuencia == 5){
                frecuenciaTiempo = 60*1000;
            } else {
                frecuenciaTiempo = 24 * 3600 * 1000 / frecuencia;
            }
            JobInfo jobInfo = new JobInfo.Builder (JOB_ID, componentName).setMinimumLatency(frecuenciaTiempo).build();
            mScheduler.schedule(jobInfo);
        }
    }

    private void cancelarJob(){
        if (mScheduler!=null){
            mScheduler.cancel(JOB_ID);
            mScheduler = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.sing_in_short_lable, Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.sing_in_canceled_lable, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        deattachReadNotifications();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, ActivitySettings.class));
                return true;
            case R.id.notification:
                sendNotification();
                return true;
            /*case R.id.location:
                obtenerUbicacion();
                return true;
                */
            case R.id.sign_out_menu:
                // sing out
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        mUserText.setText(mUsername);
        attachReadNotifications();
    }


    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        deattachReadNotifications();
    }


    void attachReadNotifications(){

        if (mValueEventListener == null) {
            FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();


            mValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    mUserNotifications.clear();
                    for(DataSnapshot notificationSnapshot: dataSnapshot.getChildren()){
                        Notification n = notificationSnapshot.getValue(Notification.class);
                        //Log.d("PRUEBA1", n.mood);
                        if(n.mood != null){
                            mUserNotifications.add(n);
                        }

                    }
                    showInfo();


                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("NOTIFI", "Failed to read value.", error.toException());
                }
            };
            mUsersDatabaseReference.child(Objects.requireNonNull(currentFirebaseUser).getUid()).child("user_notifications").addValueEventListener(mValueEventListener);
        }
    }

    public void showInfo(){
        mGraph.removeAllSeries();
        if(mUserNotifications.size() < 4) {
            mText.setText("Aún no tienes suficientes datos, responde mínimo a 4 notificaciones para ver la gráfica :)");
        }else {
            switch (mViewType){
                case History:
                    showHistoryGraph();
                    mGraph.getGridLabelRenderer().setHorizontalAxisTitle("Notificaciones");
                    break;
                case Week:
                    showWeekGraph();
                    mGraph.getGridLabelRenderer().setHorizontalAxisTitle("Días de la semana");
                    break;
            }
        }
    }


    private void deattachReadNotifications() {
        if (mValueEventListener != null) {
            FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            mUsersDatabaseReference.removeEventListener(mValueEventListener);
            mValueEventListener = null;
        }
    }

    public void showHistoryGraph(){
        DataPoint[] dataPoints = getHistoryDataPoints();
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        mGraph.addSeries(series);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMinX(1);
        mGraph.getViewport().setMaxX(dataPoints.length + 1);
        mGraph.getViewport().setMinY(1);
        mGraph.getViewport().setMaxY(5);
        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(mGraph);
        mGraph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
    }

    public void showWeekGraph(){
        DataPoint[] dataPoints = getWeekDataPoints();
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(dataPoints);
        mGraph.addSeries(series);

        // styling
        series.setValueDependentColor(new ValueDependentColor<DataPoint>() {
            @Override
            public int get(DataPoint data) {
                return Color.rgb((int) data.getX()*255/4, (int) Math.abs(data.getY()*255/6), 100);
            }
        });

        series.setSpacing(10);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        mGraph.getViewport().setMaxX(dataPoints.length + 1);
        mGraph.getViewport().setMinY(0);
        mGraph.getViewport().setMaxY(5);
        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(mGraph);
        staticLabelsFormatter.setHorizontalLabels(new String[] {"", "D", "L", "M", "M", "J", "V", "S", ""});
        mGraph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
        /*mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    int intValue = (int) value;
                    switch(intValue){
                        case 1:
                            return "D";
                        case 2:
                            return "L";
                        case 3:
                            return  "M";
                        case 4:
                            return "Mi";
                        case 5:
                            return "J";
                        case 6:
                            return "V";
                        case 7:
                            return "S";
                        default:
                            return "P";
                    }

                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });*/
    }



    public DataPoint[] getHistoryDataPoints(){
        int nNotifications = mUserNotifications.size();
        Collections.sort((List)mUserNotifications);
        DataPoint[] dataPoints = new DataPoint[nNotifications];
        for(int i = 0; i < dataPoints.length; i++){
            int intMood = moodOptions.get(mUserNotifications.get(i).mood);
            Date notificationDate = mUserNotifications.get(i).getDate();
            dataPoints[i] = new DataPoint(i + 1, intMood);
        }
        return dataPoints;
    }

    public DataPoint[] getWeekDataPoints(){
        HashMap<Integer, ArrayList<Integer>> notificationsByDay = new HashMap<>();
        DataPoint[] dataPoints= new DataPoint[7];
        Calendar calendar = Calendar.getInstance();
        for(int i = 1; i <= 7; i++){
            notificationsByDay.put(i, new ArrayList<>());
        }

        for (Notification notification:  mUserNotifications){
            calendar.setTime(notification.getDate());
            int notificationDay = calendar.get(Calendar.DAY_OF_WEEK);
            int intMood = moodOptions.get(notification.mood);
            notificationsByDay.get(notificationDay).add(intMood);
        }

        for(int i = 0; i < notificationsByDay.size(); i++){
            double averageMood = average(notificationsByDay.get(i + 1));
            dataPoints[i] = new DataPoint(i + 1, averageMood);
        }

        return dataPoints;

    }

    double average(ArrayList<Integer> list){
        if(list.isEmpty())
            return 0;
        double avg = 0;
        for(int element: list)
            avg += element;
        avg = avg / list.size();
        return avg;
    }


    private void sendNotification() {
        RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.view_expanded_notification);
        //RemoteViews expandedView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.view_expanded_notification);
        expandedView.setTextViewText(R.id.timestamp, DateUtils.formatDateTime(this, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME));
        // adding action to vsad button
        Intent vsadIntent = new Intent(this, NotificationIntentService.class);
        vsadIntent.setAction("vsad");
        expandedView.setOnClickPendingIntent(R.id.vsad_button, PendingIntent.getService(this, 1, vsadIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        // adding action to sad button
        Intent sadIntent = new Intent(this, NotificationIntentService.class);
        sadIntent.setAction("sad");
        expandedView.setOnClickPendingIntent(R.id.sad_button, PendingIntent.getService(this, 2, sadIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        // adding action to meh button
        Intent mehIntent = new Intent(this, NotificationIntentService.class);
        mehIntent.setAction("meh");
        expandedView.setOnClickPendingIntent(R.id.meh_button, PendingIntent.getService(this, 3, mehIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        // adding action to happy button
        Intent happyIntent = new Intent(this, NotificationIntentService.class);
        happyIntent.setAction("happy");
        expandedView.setOnClickPendingIntent(R.id.happy_button, PendingIntent.getService(this, 4, happyIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        // adding action to vhappy button
        Intent vhappyIntent = new Intent(this, NotificationIntentService.class);
        vhappyIntent.setAction("vhappy");
        expandedView.setOnClickPendingIntent(R.id.vhappy_button, PendingIntent.getService(this, 5, vhappyIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.view_collapsed_notification);
        collapsedView.setTextViewText(R.id.timestamp, DateUtils.formatDateTime(this, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        // these are the three things a NotificationCompat.Builder object requires at a minimum
        builder.setSmallIcon(R.drawable.ic_cyber)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(CONTENT_TEXT)
                // setting the custom collapsed and expanded views
                //.setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                // setting style to DecoratedCustomViewStyle() is necessary for custom views to display
                .setStyle(new android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle());
        // retrieves android.app.NotificationManager
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Objects.requireNonNull(notificationManager).notify(234, builder.build());
    }

    public void setLocation(Location location){
        this.location = location;
    }

    private void obtenerUbicacion(){
        ContentResolver contentResolver = getBaseContext().getContentResolver();
        boolean gpsStatus = Settings.Secure.isLocationProviderEnabled(contentResolver, LocationManager.GPS_PROVIDER);
        if (gpsStatus && ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            LocationListener locationListener = new MyLocationListener(this);
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 10, locationListener);
        }

        if(location != null){
            mostrarUbicacion("latitud: " + location.getLatitude());
        } else {
            mostrarUbicacion("Información no disponible.");
        }
    }

    public void mostrarUbicacion(String mensaje){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Mensaje: " + mensaje)
                .setTitle("Location");
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
