package com.ifti.addressbook;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.NameValuePair;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.message.BasicNameValuePair;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class AddressList extends AppCompatActivity {

    private ListView lvEvents;
    private ArrayList<Event> events;
    private CustomEventAdapter adapter;

    private Button btnCreate, btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_list);

        getSupportActionBar().hide();

        events = new ArrayList<>();
        lvEvents = findViewById(R.id.listEvents);
        loadData();

        btnCreate = findViewById(R.id.btnCreate);
        btnExit = findViewById(R.id.btnExit);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(AddressList.this, MainActivity.class);
                startActivity(i);
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAffinity();
            }
        });
    }

    private void loadData() {
        events.clear();

        String existingKey = "";
        String value = "";

        String[] keys2 = {"action", "id", "semester", "key", "event"};
        String[] values2 = {"restore", "2026160060", "20231", existingKey, value};
        httpRequest(keys2, values2);

//        System.out.println("Here 1");
    }

    private void showDialog(String message, String title, String key, String value) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setTitle(title);
        builder.setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
//handle delete

                String[] keys2 = {"action", "id", "semester", "key", "event"};
                String[] values2 = {"remove", "2026160060", "20231", key, value};
                httpRequest(keys2, values2);
                dialog.cancel();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
    public void onRestart() {
        super.onRestart();
        //adapter.notifyDataSetChanged();
        loadData();
    }
    @SuppressLint("StaticFieldLeak")
    private void httpRequest(final String[] keys, final String[] values) {
        new AsyncTask<Void, Void, String>() {

            @SuppressLint("StaticFieldLeak")
            @Override
            protected String doInBackground(Void... voids) {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                for (int i = 0; i < keys.length; i++) {
                    params.add(new BasicNameValuePair(keys[i], values[i]));
                }
                String url = "https://muthosoft.com/univ/cse489/index.php";
                String data = "";
                try {
                    data = JSONParser.getInstance().makeHttpRequest(url, "POST", params);
                    return data;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected void onPostExecute(String data) {
                if (data != null) {
                    updateEventListByServerData(data);
                }
            }
        }.execute();
    }

    public void updateEventListByServerData(String data) {
        try {
            JSONObject jo = new JSONObject(data);
            if (jo.has("events")) {
                JSONArray ja = jo.getJSONArray("events");
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject event = ja.getJSONObject(i);
                    String eventKey = event.getString("e_key");
                    String eventValue = event.getString("e_value");
// split eventValue to show in event list
                    String[] fieldValues = eventValue.split("---");

                    String name = fieldValues[0];
                    String email = fieldValues[1];
                    String phoneHome = fieldValues[2];
                    String phoneOffice = fieldValues[3];
                    String encodedImage = fieldValues[4];

                    byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);

                    Bitmap photo = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    Event e = new Event(eventKey, name, email, phoneHome, phoneOffice, photo, eventValue);
                    events.add(e);

                }
            }
            adapter = new CustomEventAdapter(this, events);
            lvEvents.setAdapter(adapter);

            lvEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                //Position = Real Position
                public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    System.out.println("Position Key: " + position);

                    Intent i = new Intent(AddressList.this, MainActivity.class);
                    i.putExtra("EVENT_KEY", events.get(position).key);
                    i.putExtra("EVENT_VALUE", events.get(position).value);
                    startActivity(i);
                }
            });
            // handle the long-click on an event-list item
            lvEvents.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    String message = "Do you want to delete event - " + events.get(position).name + " ?";
                    showDialog(message, "Delete Event", events.get(position).key, events.get(position).value);
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}