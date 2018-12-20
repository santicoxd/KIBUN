package com.google.firebase.jfpinedap.kibun;

import android.app.IntentService;

import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;


public class NotificationIntentService extends IntentService {

    private String mood;

    private DatabaseReference mDatabase;

    private JobScheduler mScheduler;

    public NotificationIntentService() {
        super("notificationIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        switch (intent.getAction()) {

            case "vsad":
                mood = "vsad";
                Handler vsadHandler = new Handler(Looper.getMainLooper());
                vsadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), R.string.vsad, Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case "sad":
                mood = "sad";
                Handler sadHandler = new Handler(Looper.getMainLooper());
                sadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), R.string.sad, Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case "meh":
                mood = "meh";
                Handler mehHandler = new Handler(Looper.getMainLooper());
                mehHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(),R.string.meh, Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case "happy":
                mood = "happy";
                Handler happyHandler = new Handler(Looper.getMainLooper());
                happyHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), R.string.happy, Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case "vhappy":
                mood = "vhappy";
                Handler vhappyHandler = new Handler(Looper.getMainLooper());
                vhappyHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), R.string.vhappy, Toast.LENGTH_LONG).show();
                    }
                });
                break;
        }
        saveNotification(mood);
        iniciarJob();
    }

    public void saveNotification(String notification){
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        long id = new java.util.Date().getTime();
        String id_notification = Objects.toString(id);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("users").child(currentFirebaseUser.getUid()).child("user_notifications").child(id_notification).setValue(new java.util.Date());
        mDatabase.child("users").child(currentFirebaseUser.getUid()).child("user_notifications").child(id_notification).child("mood").setValue(notification);
        mDatabase.child("users").child(currentFirebaseUser.getUid()).child("user_notifications").child(id_notification).child("location").setValue("Test");
        closeNotification((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
    }

    private void closeNotification(NotificationManager notificationManager){
        // notification will be dismissed when tapped
        notificationManager.cancelAll();
    }

    private void iniciarJob(){
        mScheduler = (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);
        mScheduler.cancel(BaseStatsView.JOB_ID);

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int frecuencia = mPrefs.getInt("frecuencia", 4);
        if(frecuencia > 0) {
            ComponentName componentName = new ComponentName(this, JobNotifications.class);
            long frecuenciaTiempo;
            if (frecuencia == 5){
                frecuenciaTiempo = 60*1000;
            } else {
                frecuenciaTiempo = 24 * 3600 * 1000 / frecuencia;
            }
            JobInfo jobInfo = new JobInfo.Builder (BaseStatsView.JOB_ID, componentName).setMinimumLatency(frecuenciaTiempo).build();
            mScheduler.schedule(jobInfo);
        }
    }
}
