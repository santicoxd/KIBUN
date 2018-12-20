package data.model;

import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;

public class Notification  implements Comparable{
    public int date;
    public int day;
    public int hours;
    public String location;
    public int minutes;
    public int month;
    public String mood;
    public int seconds;
    public long time;
    public int timezoneOffset;
    public int year;
    /*public String location;
    public String mood;*/

    public Notification (){
        //Calendar cal = Calendar.getInstance();
        //cal.setTimeInMillis(0);
        //cal.set(year, month, day, hours, minutes, seconds);

    }

    public Date getDate(){
        return new Date(time);
    }

    /*public Notification (String location, Date date, String mood){
        this.location = location;
        //this.date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date);
        this.date = date;
        this.mood = mood;
    }*/


    @Override
    public String toString(){
        return getDate().toString() + " --" + mood;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        Notification n = (Notification) o;
        return getDate().compareTo(n.getDate());
    }
}
