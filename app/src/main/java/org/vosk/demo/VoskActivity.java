package org.vosk.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class VoskActivity extends Activity implements RecognitionListener {

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_FILE = 3;
    static private final int STATE_MIC = 4;

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    public static final int PERMISSIONS_REQUEST_BLUETOOTH_CONNECT = 2;
    public static final int PERMISSIONS_REQUEST_BLUETOOTH_SCAN = 3;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch lampSwitch;
    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private TextView resultView;
    private ListView listView;
    private Lamp lamp;
    private Bluetooth bluetooth;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);

        lamp = new Lamp();
        resultView = findViewById(R.id.result_text);
        lampSwitch = findViewById(R.id.lampSwitch);
        listView = findViewById(R.id.listView);
        setUiState(STATE_START);

        findViewById(R.id.discover).setOnClickListener(view -> startDiscover());
        findViewById(R.id.recognize_mic).setOnClickListener(view -> recognizeMicrophone());
        ((ToggleButton) findViewById(R.id.pause)).setOnCheckedChangeListener((view, isChecked) -> pause(isChecked));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            DeviceInfoModel selectedDevice = (DeviceInfoModel) parent.getItemAtPosition(position);
            String macAddress = selectedDevice.getMacAddress();
            String data =((EditText) findViewById(R.id.editTextData)).getText().toString();

            this.bluetooth.sendData(macAddress, data);
        });
        lampSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) lamp.turn_on();
            else lamp.turn_off();
        });

        LibVosk.setLogLevel(LogLevel.INFO);
        initBluetooth();
        initModel();
    }

    private void initBluetooth() {
        this.bluetooth = new Bluetooth(this, this);
    }

    private void initModel() {
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    setUiState(STATE_READY);
                }, (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Record audio permission denied, closing application", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (requestCode == PERMISSIONS_REQUEST_BLUETOOTH_CONNECT || requestCode == PERMISSIONS_REQUEST_BLUETOOTH_SCAN) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permission denied, close the activity
                Toast.makeText(this, "Bluetooth permission denied, closing application", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }

    @Override
    public void onResult(String hypothesis) {
        if (hypothesis.length() > 17) {
            resultView.append(hypothesis + "\n");
            if (hypothesis.contains("turn on")) {
                lampSwitch.setChecked(true);
            } else if (hypothesis.contains("turn off")) {
                lampSwitch.setChecked(false);
            }
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        setUiState(STATE_DONE);
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        setUiState(STATE_DONE);
    }

    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
                resultView.setText(R.string.preparing);
                resultView.setMovementMethod(new ScrollingMovementMethod());
                findViewById(R.id.recognize_mic).setEnabled(false);
                findViewById(R.id.pause).setEnabled((false));
                break;
            case STATE_READY:
                resultView.setText(R.string.ready);
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
                findViewById(R.id.recognize_mic).setEnabled(true);
                findViewById(R.id.pause).setEnabled((false));
                break;
            case STATE_DONE:
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
                findViewById(R.id.recognize_mic).setEnabled(true);
                findViewById(R.id.pause).setEnabled((false));
                ((ToggleButton) findViewById(R.id.pause)).setChecked(false);
                break;
            case STATE_FILE:
                resultView.setText(getString(R.string.starting));
                findViewById(R.id.recognize_mic).setEnabled(false);
                findViewById(R.id.pause).setEnabled((false));
                break;
            case STATE_MIC:
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.stop_microphone);
                resultView.setText(getString(R.string.say_something));
                findViewById(R.id.recognize_mic).setEnabled(true);
                findViewById(R.id.pause).setEnabled((true));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    private void setErrorState(String message) {
        resultView.setText(message);
        ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
        findViewById(R.id.recognize_mic).setEnabled(false);
    }

    private void recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_DONE);
            speechService.stop();
            speechService = null;
        } else {
            setUiState(STATE_MIC);
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

    private void pause(boolean checked) {
        if (speechService != null) {
            speechService.setPause(checked);
        }
    }


    private void connectToPairedDevice() {

    }

    private void startDiscover() {
        Button discover = findViewById(R.id.discover);
        discover.setEnabled(false);

        List<DeviceInfoModel> pairedDevices = this.bluetooth.getAllPaired();
        if (pairedDevices.size() == 0)
            pairedDevices.add(new DeviceInfoModel("Nothing found", null));
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pairedDevices));

        discover.setEnabled(true);
    }

}
