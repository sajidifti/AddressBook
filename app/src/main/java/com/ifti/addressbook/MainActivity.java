package com.ifti.addressbook;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.NameValuePair;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    EditText nameFld, email, phoneHome, phoneOffice;

    Boolean update = false;
    ImageView photo;
    Button cancel, save;
    String key = "";

    String existingKey = "";

    String value = "";

    String encodedImage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();


        nameFld = findViewById(R.id.nameFld);
        email = findViewById(R.id.email);
        phoneHome = findViewById(R.id.phoneHome);
        phoneOffice = findViewById(R.id.phoneOffice);
//        select = findViewById(R.id.select);
        photo = findViewById(R.id.photo);
        cancel = findViewById(R.id.cancel);
        save = findViewById(R.id.save);
        save.setOnClickListener(view -> funcSave());
        cancel.setOnClickListener(view -> exit(view));

        Intent i = getIntent();
        if (i.hasExtra("EVENT_KEY")) {
            existingKey = i.getStringExtra("EVENT_KEY");
            value = i.getStringExtra("EVENT_VALUE");

            String[] values = value.split("---");

            nameFld.setText(values[0]);
            email.setText(values[1]);
            phoneHome.setText(values[2]);
            phoneOffice.setText(values[3]);

            encodedImage = values[4];

            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);

            Bitmap photoBit = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            photo.setImageBitmap(photoBit);

            update = true;
        }
    }

    public void funcSave() {
        String name1 = nameFld.getText().toString().trim();
        String mail1 = email.getText().toString().trim();
        String phone2 = phoneHome.getText().toString().trim();
        String phone3 = phoneOffice.getText().toString().trim();

        try {
            if (!name1.isEmpty()) {
                if (isValid(mail1)) {
                    if (isValidMobileNo(phone2)) {
                        if (isValidMobileNo(phone3)) {
                            if (key.length() == 0 && !update) {
                                key = name1 + System.currentTimeMillis();
                                System.out.println(key);
                                existingKey = key;
                            }

                            value = name1 + "---" + mail1 + "---" + phone2 + "---" + phone3 + "---" + encodedImage;

                            String[] keys = {"action", "id", "semester", "key", "event"};
                            String[] values = {"backup", "2026160060", "20231", existingKey, value};
                            httpRequest(keys, values);

                            Toast.makeText(getApplicationContext(), "Data Stored!", Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(getApplicationContext(), "Invalid phone number!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Invalid phone number!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Invalid mail!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Name cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error   Occur!", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isValid(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." +
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);
        if (email == null)
            return false;
        return pat.matcher(email).matches();
    }

    public static boolean isValidMobileNo(String str) {
        return str.matches("^(?:\\+88|88)?(01[3-9]\\d{8})$");
    }

    public void exit(View view) {
        Intent i = new Intent(MainActivity.this, AddressList.class);
        startActivity(i);
        finish();
    }

    public void selectImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            photo.setImageURI(imageUri);

            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

                // Determine the desired width and height
                int desiredWidth = 60;
                int desiredHeight = 60;

                // Retrieve the orientation from the image's EXIF metadata
                int orientation = getImageOrientation(imageUri);

                // Create a Matrix to handle the rotation and resizing
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);

                // Resize the original Bitmap
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, desiredWidth, desiredHeight, true);

                // Apply the rotation and resizing to the Bitmap
                Bitmap finalBitmap = Bitmap.createBitmap(resizedBitmap, 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix, true);

                // Convert the final Bitmap to a byte array
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] bytes = byteArrayOutputStream.toByteArray();

                // Convert the byte array to a Base64 encoded string
                encodedImage = Base64.encodeToString(bytes, Base64.DEFAULT);
                System.out.println("Encoded Image: " + encodedImage);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getImageOrientation(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ExifInterface exifInterface = new ExifInterface(inputStream);

            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @SuppressLint("StaticFieldLeak")
    private void httpRequest(final String keys[], final String values[]) {
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
                    Toast.makeText(getApplicationContext(), "Saved to remote database", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }
}