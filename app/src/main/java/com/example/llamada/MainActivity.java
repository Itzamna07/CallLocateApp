package com.example.llamada;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    String mensajeTexto = "";
    TextView tvtelefono;

    EditText ettelefono;
    String savedPhoneNumber;
    boolean callEnded = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvtelefono = findViewById(R.id.tvtelefono);
        ettelefono = findViewById(R.id.ettelefono);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        savedPhoneNumber = sharedPreferences.getString("phone_number", "");

        Button guardarButton = findViewById(R.id.btnguardar);
        guardarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savedPhoneNumber = ettelefono.getText().toString(); // Obtén el número de teléfono del EditText
                Toast.makeText(MainActivity.this, "Número guardado en preferencias: " + savedPhoneNumber, Toast.LENGTH_SHORT).show();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("phone_number", savedPhoneNumber);
                editor.apply();
            }
        });

        Button msjButton = findViewById(R.id.btnmsj);
        msjButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        PhoneCallListener phoneListener = new PhoneCallListener();
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private class PhoneCallListener extends PhoneStateListener {
        private boolean isPhoneCalling = false;
        private boolean callEnded = false;
        private Timer ringingTimer;

        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);
            String LOG_TAG = "llamada entrante";
            if (TelephonyManager.CALL_STATE_RINGING == state) {
                Log.i(LOG_TAG, "RING, número: " + phoneNumber);


            }
            if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
                Log.i(LOG_TAG, "OFFHOOK");
                isPhoneCalling = true;
            }
            if (TelephonyManager.CALL_STATE_IDLE == state) {
                Log.i(LOG_TAG, "IDLE number");
                tvtelefono.setText(phoneNumber);
                if (phoneNumber.equals(savedPhoneNumber)) {
                    Toast.makeText(MainActivity.this, "Enviando mensaje a " + phoneNumber, Toast.LENGTH_SHORT).show();
                    startLocationSending();
                }
            }
        }

        private void startLocationSending() {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mensajeTexto = "SIN PERMISOS";
                return;
            }
            LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location loc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc != null) {
                double longitudeGPS = loc.getLongitude();
                double latitudeGPS = loc.getLatitude();

                try {
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> list = geocoder.getFromLocation(latitudeGPS, longitudeGPS, 1);
                    if (!list.isEmpty()) {
                        Address DirCalle = list.get(0);
                        String direccion = DirCalle.getAddressLine(0);
                        mensajeTexto = "Estoy en http://maps.google.com/maps?&z=15&mrt=loc&t=m&q=" + latitudeGPS + "+" + longitudeGPS;

                        // Marca la llamada como terminada
                        callEnded = true;
                        // Cancela el temporizador si aún está en curso
                        if (ringingTimer != null) {
                            ringingTimer.cancel();
                        }

                        // Aquí puedes realizar acciones adicionales antes de enviar el mensaje, si es necesario.
                        sendMessage();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // La ubicación no está disponible
                mensajeTexto = "Ubicación no disponible";
            }
        }

    }


    private void sendMessage() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "SIN PERMISOS", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location loc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double longitudeGPS = loc.getLongitude();
        double latitudeGPS = loc.getLatitude();

        try {
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> list = geocoder.getFromLocation(latitudeGPS, longitudeGPS, 1);
            if (!list.isEmpty()) {
                Address DirCalle = list.get(0);
                String direccion = DirCalle.getAddressLine(0);
                mensajeTexto = "Estoy en http://maps.google.com/maps?&z=15&mrt=loc&t=m&q=" + latitudeGPS + "+" + longitudeGPS;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(savedPhoneNumber, null, mensajeTexto, null, null);
    }
}
