package com.google.firebase.jfpinedap.kibun;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class JobNotifications extends JobService {

    private String NOTIFICATION_TITLE = "KIBUN";
    private String CONTENT_TEXT = "¿Cómo estás?";

    @Override
    public boolean onStartJob(JobParameters params) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            sendNotification();
            jobFinished(params, false);
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
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
}
