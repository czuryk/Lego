package com.mike.blecontrol;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;

import static java.lang.Math.floor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import android.os.Build;

import com.welie.blessed.BluetoothCentral;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {

    BluetoothGattCharacteristic writeChar;
    BluetoothGatt gatt;
    static EditText log;
    private boolean scanning = false;
    private boolean connected = false;
    private boolean listening = false;
    private static boolean isCentralConnected = false;
    private Handler handler = new Handler();
    final List<String> devices = new ArrayList<String>();
    String selectedDevice;
    Button listen;
    Button scan;
    Button connect;
    Button cmd1;
    Button cmd2;
    Button cmd3;
    Button cmd4;
    ProgressBar progressBar;
    Spinner dropdown;
    TextView vText;
    TextView rssiText;
    TextView posText;
    static SeekBar seekBar1;
    SeekBar seekBar2;
    TextView writeCountText;
    int oldA = 0;
    int oldB = 0;
    int oldC = 0;
    int oldD = 0;
    int oldE = 0;
    int oldF = 0;
    int buttons = 0;
    int oldButtons = 0;
    static int hubVolts = 0;
    static int distance = 0;
    static int yaw = 0;
    static int pitch = 0;
    static int roll = 0;
    int write_count = 0;
    boolean isVibratedJoyLeft = false;
    boolean isVibratedJoyRight = false;

    private int REQUEST_FINE_LOCATION_PERMISSION = 100;
    private int REQUEST_BLUETOOTH_SCAN_PERMISSION = 101;
    private int REQUEST_BACKGROUND_LOCATION_PERMISSION = 102;
    private int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 103;
    private int REQUEST_BLUETOOTH_ADVERTISE_PERMISSION = 1;
    Timer timer = new Timer();
    Timer timer2 = new Timer();
    int androidVersion = Integer.parseInt(Build.VERSION.RELEASE);
    BLEService bleService;
    boolean introduced = false;

    static void changeLogText(String text) {
        log.append(text + System.getProperty("line.separator"));
    }

    public static void onCentralConnected(@NotNull BluetoothCentral central) {
        isCentralConnected = true;
        log.append("Central connected." + System.getProperty("line.separator"));
    }

    public static void onCentralDisconnected(@NotNull BluetoothCentral central) {
        //isCentralConnected = false;
        log.append("Central disconnected." + System.getProperty("line.separator"));
    }

    // Server mode receive data
    public static void onCentralCharacteristicChanged(String s)
    {
        if (s.indexOf("D:") != -1) {
            distance = Integer.parseInt(s.split(":")[1]);
            return;
        }
        if (s.indexOf("V:") != -1) {
            hubVolts = Integer.parseInt(s.split(":")[1]);
            return;
        }
        if (s.indexOf("A:") != -1) {
            yaw = Integer.parseInt(s.split(":")[1]);
            pitch = Integer.parseInt(s.split(":")[2]);
            roll = Integer.parseInt(s.split(":")[3]);
            return;
        }

        log.append(s + System.getProperty("line.separator"));
    }

    static void changeControlState(String text) {
        //log.append(text + System.getProperty("line.separator"));
        int value = Integer.parseInt(text.substring(8, 10), 16);
        seekBar1.setProgress(value);
        log.append(String.valueOf(value) + System.getProperty("line.separator"));
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        log = findViewById(R.id.log);
        seekBar1 = findViewById(R.id.seekBar1);
        seekBar2 = findViewById(R.id.seekBar2);
        JoystickView joystickLeft = findViewById(R.id.joystickView_left);
        JoystickView joystickRight = findViewById(R.id.joystickView_right);
        writeCountText = findViewById(R.id.writeCountText);
        //joystickLeft.setEnabled(false);
        //joystickRight.setEnabled(false);

        listen = findViewById(R.id.listen);
        scan = findViewById(R.id.scan);
        connect = findViewById(R.id.connect);
        progressBar = findViewById(R.id.progressBar);
        dropdown = findViewById(R.id.spinner1);
        vText = findViewById(R.id.volts);
        rssiText = findViewById(R.id.rssi);
        posText = findViewById(R.id.pos);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBT();
            }
        });

        log.setLongClickable(false);
        //Log.i("my", "Android verion: " + androidVersion);

        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!listening) {
                    // TODO: fix the problem with necessury permissions for BLE server. Fixed?
                    // Start BLE server
                    bleService = BLEService.getInstance(getApplicationContext());
                    bleService.startAdvertising();
                    listening = true;
                    log.getText().clear();
                    listen.setText("Stop");
                    log.append("Start listening..." + System.getProperty("line.separator"));
                    scan.setEnabled(false);
                    connect.setEnabled(false);
                } else {
                    log.getText().clear();
                    listen.setText("Listen");
                    listening = false;
                    bleService.stopAdvertising();
                    if (isCentralConnected) {bleService.disconnectCentrals(); isCentralConnected = false;}
                    log.append("Stop listening..." + System.getProperty("line.separator"));
                    scan.setEnabled(true);
                    connect.setEnabled(true);
                }
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!connected) {
                    listen.setEnabled(false);
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(selectedDevice);
                    if ((ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                        if (checkPermissions()) return;
                    }
                    gatt = device.connectGatt(getApplicationContext(), false, bluetoothGattCallback, TRANSPORT_LE);
                    Log.i("my", "Connecting...");
                } else {
                    // Notify hub what we disconnected, so end program and free BLE to new connection. This is PyBrick specific.
                    byte[] data = new byte[5];
                    data[0] = (byte)0x06; // Service byte
                    // Terminate hub program code
                    data[1] = (byte)0xFF;
                    data[2] = (byte)0xFE;
                    data[3] = (byte)0x00;
                    data[4] = (byte)0x00;
                    writeChar(data);
                    introduced = false;
                    gatt.disconnect();
                    gatt.close();
                    gatt = null;
                    listen.setEnabled(true);
                    formRestore();
                }

            }
        });

        cmd1 = findViewById(R.id.cmd1);
        cmd2 = findViewById(R.id.cmd2);
        cmd3 = findViewById(R.id.cmd3);
        cmd4 = findViewById(R.id.cmd4);

        cmd1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate(100);
            }
        });

        cmd2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate(100);
            }
        });

        cmd3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate(100);
            }
        });

        cmd4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate(100);
            }
        });

        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                disableControl();
            }
        });

        joystickLeft.setOnTouchListener(new JoystickView.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == 2 && !isVibratedJoyLeft) {
                    isVibratedJoyLeft = true;
                    vibrate(30);
                }
                if (event.getAction() == 1 && isVibratedJoyLeft) {
                    vibrate(30);
                    isVibratedJoyLeft = false;
                }

                return false;
            }
        });

        joystickRight.setOnTouchListener(new JoystickView.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == 2 && !isVibratedJoyRight) {
                    isVibratedJoyRight = true;
                    vibrate(30);
                }
                if (event.getAction() == 1 && isVibratedJoyRight) {
                    vibrate(30);
                    isVibratedJoyRight = false;
                }

                return false;
            }
        });

        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                disableControl();
            }
        });

        // For populating readed data from hub on display
        timer2.schedule(new TimerTask() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if ((joystickLeft.getNormalizedX() == 50) && (joystickLeft.getNormalizedY() == 50) && (joystickRight.getNormalizedX() == 50) && (joystickRight.getNormalizedY() == 50)) {enableControl();}
                        if (log.getLineCount() > 100) {log.getText().clear();}
                        if ((connected) && (gatt != null)) {
                            progressBar.setProgress(distance / 10);
                            vText.setText(new DecimalFormat("#.0#v").format((float)hubVolts / 1000));
                            // Print hub gyro velocity coordinates. Convert back to decimals
                            posText.setText((yaw / 10) + "  " + (pitch / 10) + "  " + (roll / 10));
                            gatt.readRemoteRssi();
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
            }
        }, 0, 50);

        // For data writing to hub
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (writeChar == null) {return;}
                buttonState();
                // For now we send coordinates two joysticks.
                // More data - less performance
                int x1 = (int) floor((double) joystickLeft.getNormalizedX() / 100 * 255);
                int y1 = (int) floor((double) joystickLeft.getNormalizedY() / 100 * 255);
                int x2 = (int) floor((double) joystickRight.getNormalizedX() / 100 * 255);
                int y2 = (int) floor((double) joystickRight.getNormalizedY() / 100 * 255);

                //Log.i("my", "is_connected: " + connected + "gatt: " + gatt + "writeChar: " + writeChar);
                if ((connected) && (gatt != null) && (writeChar != null) || ((listening) && (isCentralConnected))) {

                    // Start program on hub
                    if (!introduced) {
                        byte[] data = new byte[1];
                        data[0] = (byte) 0x01;

                        if (writeChar(data)) {
                            introduced = true;
                        }
                    }

                    if (oldA != x1 || oldB != y1 || oldC != x2 || oldD != y2 || oldE != seekBar1.getProgress() || oldF != seekBar2.getProgress() || oldButtons != buttons) {
/*
                        // Example of full stack of data
                        byte[] data = new byte[8];
                        data[0] = (byte)0x06; // Service byte for PyBrick
                        data[1] = (byte) x1; // Left Horizontal
                        data[2] = (byte) y1; // Left Vertical
                        data[3] = (byte) x2; // Right Horizontal
                        data[4] = (byte) y2; // Right Vertical
                        data[5] = (byte) seekBar1.getProgress(); // Trim A
                        data[6] = (byte) seekBar2.getProgress(); // Trim B
                        data[7] = (byte) buttons;
 */

                        // Compact mode to increase speed of transmission
                        byte[] data = new byte[5];
                        data[0] = (byte)0x06; // Service byte for PyBrick
                        data[1] = (byte)x1; // Left Horisontal
                        data[2] = (byte)y1; // Left Vertical
                        data[3] = (byte)x2; // Right Horisontal
                        data[4] = (byte)y2; // Right Vertical

                        writeChar(data);

                        // This mode must be enabled for server mode.
                        // In server mode we swap the sending and receiving logic.

                        //if ((connected) && (gatt != null) && (writeChar != null)) {writeChar(data);}
                        //if ((listening) && (isCentralConnected)) {bleService.sendNotify(data);}

                        oldA = x1;
                        oldB = y1;
                        oldC = x2;
                        oldD = y2;
                        oldE = seekBar1.getProgress();
                        oldF = seekBar2.getProgress();
                        oldButtons = buttons;
                    }
                }
            }

        }, 0, 150);
        // The period is critical here
        // In case of less delay and large size packets the receiving packets on hub will accumulate and overflow.
        // This reduce performance and stability.
        // It needs to be adopted according to the task.

    }
    // Alternative scan function
    // If we want to pin only for one specific model and we know the model name
    private void scanBT() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        selectedDevice = "";
        String[] names = new String[]{"robot1"};
        List<ScanFilter> filters = null;
        if (names != null) {
            filters = new ArrayList<>();
            for (String name : names) {
                ScanFilter filter = new ScanFilter.Builder()
                        .setDeviceName(name)
                        .build();
                filters.add(filter);
            }
        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L)
                .build();

        if (!scanning) {
            if ((ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                if (checkPermissions()) return;
            }

            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    scanning = false;
                    scanner.stopScan(scanCallback);
                    addDropDown(devices);
                    scan.setVisibility(View.INVISIBLE);
                    connect.setVisibility(View.VISIBLE);
                }
            }, 1500);

            //scanner.startScan(filters, scanSettings, scanCallback);
            scanning = true;
            scanner.startScan(scanCallback);
            devices.clear();
            Log.i("my", "Scan started");
        } else {
            scanning = false;
            scanner.stopScan(scanCallback);
        }
    }

    final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if ((ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                if (checkPermissions()) return;
            }
            //Log.i("my", "Name: " + device.getName() + " Addr: " + device.getAddress());

            String dName = "";
            if (device.getName() != null) {
                dName = device.getAddress() + " - " + device.getName();
            } else {
                dName = device.getAddress();
            }

            if (!devices.contains(dName)) {
                devices.add(dName);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i("my", "Scan failed");
        }
    };

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            rssiText.setText(rssi + "db");
        }
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if (connected) return;
            Log.i("my", "Status: " + status + " - New state: " + String.valueOf(newState));
            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if ((ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                        if (checkPermissions()) return;
                    }
                    connected = true;
                    writeChar = null;
                    gatt.discoverServices();
                    Log.i("my", "Connected");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log.getText().clear();
                            connect.setText("Disconnect");
                            write_count = 0;
                            dropdown.setEnabled(false);
                            seekBar1.setProgress(127);
                            seekBar2.setProgress(127);
                        }
                    });
                }

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // disconnected from the GATT Server
                    gatt.close();
                    connected = false;
                    introduced = false;
                    writeChar = null;
                    //formRestore();
                    Log.i("my", "Disconnected");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log.append("Disconnected" + System.getProperty("line.separator"));
                        }
                    });
                }
            } else {
                Log.i("my", "Connection problem");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connected = false;
                        introduced = false;
                        writeChar = null;
                        formRestore();
                        //log.append("Connection unsuccessfull" + System.getProperty("line.separator"));
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final List<BluetoothGattService> services = gatt.getServices();
                Log.i("my", String.format(Locale.ENGLISH, "discovered %d", services.size()));

                for (BluetoothGattService service : services) {
                    Log.i("my", "UUID: " + service.getUuid());
                    Log.i("my", "Type: " + service.getType());
                    if (!service.getUuid().toString().equals("c5f50001-8280-46da-89f4-6d8051e4aeef")) {Log.i("my", "Skip this service"); continue;}
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        int props = characteristic.getProperties();
                        //Log.i("my", "Char UUID: " + characteristic.getUuid());

                        // Filtering Notify Service with char: c5f50002-8280-46da-89f4-6d8051e4aeef
                        if ((props & PROPERTY_NOTIFY) != 0 && characteristic.getUuid().toString().equals("c5f50002-8280-46da-89f4-6d8051e4aeef")) {
                            if ((ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                                //if (checkPermissions()) return;
                            }
                            Log.i("my", "Read|Notify characteristic found: " + characteristic.getUuid().toString());

                            gatt.setCharacteristicNotification(characteristic, true);
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                            Log.i("my", "Notify enabled.");
                        }

                        // Filtering Write Service with char: c5f50002-8280-46da-89f4-6d8051e4aeef
                        if ((((props & PROPERTY_WRITE) != 0) || ((props & PROPERTY_WRITE_NO_RESPONSE) != 0)) && characteristic.getUuid().toString().equals("c5f50002-8280-46da-89f4-6d8051e4aeef")) {
                            Log.i("my", "Write characteristic found: " + characteristic.getUuid());
                            writeChar = characteristic;
                        }
                    }
                }

            } else {
                //Log.w("my", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic
        ) {
            byte[] b = characteristic.getValue();
            if (b[0] != 0x01) {return;} // Skip unnecessary data
            String s = new String(b, StandardCharsets.UTF_8).trim();

            Log.i("my", "Notify data: " + s + " bytes: " + Arrays.toString(b));
            if (s.indexOf("D:") != -1) {
                distance = Integer.parseInt(s.split(":")[1]);
                return;
            }
            if (s.indexOf("V:") != -1) {
                hubVolts = Integer.parseInt(s.split(":")[1].trim());
                return;
            }
            if (s.indexOf("A:") != -1) {
                yaw = Integer.parseInt(s.split(":")[1].trim());
                pitch = Integer.parseInt(s.split(":")[2].trim());
                roll = Integer.parseInt(s.split(":")[3].trim());
                return;
            }

            if (s.indexOf("D:") != -1) {
                distance = Integer.parseInt(s.split(":")[1].trim());
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    log.append(s + System.getProperty("line.separator"));
                }
            });

        }

    };

    @SuppressLint("MissingPermission")
    public boolean writeChar(byte[] b) {
        if ((connected) && (gatt != null) && (writeChar != null)) {
            //Log.i("my", "Write");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    writeCountText.setText(String.valueOf(write_count));
                }
            });
            write_count++;
            writeChar.setValue(b);
            writeChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); // With response type required
/*
            if ((ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                Log.i("my", "Write permission problem?");
                //checkPermissions();
                //return;
            }

 */
            boolean writeResult = gatt.writeCharacteristic(writeChar);
            if (!writeResult) {
                Log.e("my", String.format("ERROR: writeCharacteristic failed for characteristic: %s, data:  %s", writeChar.getUuid(), Arrays.toString(b)));
            }
            return writeResult;
        }
        return false;
    }

    private boolean checkPermissions() {
        boolean requested = false;
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {askForPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_FINE_LOCATION_PERMISSION); requested = true;}
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED && !requested) {askForPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION, REQUEST_BACKGROUND_LOCATION_PERMISSION); requested = true;}
        if (androidVersion > 11) if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && !requested) {askForPermission(android.Manifest.permission.BLUETOOTH_CONNECT, REQUEST_BLUETOOTH_CONNECT_PERMISSION); requested = true;}
        if (androidVersion > 11) if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && !requested) {askForPermission(android.Manifest.permission.BLUETOOTH_SCAN, REQUEST_BLUETOOTH_SCAN_PERMISSION); requested = true;}
        if (androidVersion > 11) if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED && !requested) {askForPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE, REQUEST_BLUETOOTH_ADVERTISE_PERMISSION); requested = true;}
        if (requested) {
            log.append("Operation aborted: permissions requested" + System.getProperty("line.separator"));
            return true;
        }
        return false;
    }

    private void askForPermission(String permission, Integer requestCode) {
        Log.i("my", "Requesting permission: " + permission);
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        } else {
            //Toast.makeText(this, "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
            Log.w("my", permission + " is already granted.");
        }
    }

    private void buttonState() {
        buttons = 0b00000000;

        if (cmd1.isPressed()) {
            buttons = (buttons | 0b00000001);
        }

        if (cmd2.isPressed()) {
            buttons = (buttons | 0b00000010);
        }

        if (cmd3.isPressed()) {
            buttons = (buttons | 0b00000100);
        }

        if (cmd4.isPressed()) {
            buttons = (buttons | 0b00001000);
        }
    }

    void addDropDown(List<String> items) {
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(listAdapter);

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                selectedDevice = devices.get(position);
                int i = selectedDevice.indexOf(" - ");
                if (i != -1) {
                    selectedDevice = selectedDevice.substring(0, i);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    void formRestore() {
        connect.setText("Connect");
        connect.setVisibility(View.INVISIBLE);
        scan.setVisibility(View.VISIBLE);
        connected = false;
        devices.clear();
        addDropDown(devices);
        selectedDevice = "";
        dropdown.setEnabled(true);
        progressBar.setProgress(0);
        posText.setText("0 - 0 - 0");
        vText.setText("0.0v");
        rssiText.setText("0.0db");
        hubVolts = 0;
        yaw = 0;
        pitch = 0;
        roll = 0;
        log.getText().clear();
        seekBar1.setProgress(127);
        seekBar2.setProgress(127);
        listen.setEnabled(true);
    }

    void disableControl() {
        seekBar1.setEnabled(false);
        seekBar2.setEnabled(false);
        cmd1.setEnabled(false);
        cmd2.setEnabled(false);
        cmd3.setEnabled(false);
        cmd4.setEnabled(false);
    }

    void enableControl() {
        seekBar1.setEnabled(true);
        seekBar2.setEnabled(true);
        cmd1.setEnabled(true);
        cmd2.setEnabled(true);
        cmd3.setEnabled(true);
        cmd4.setEnabled(true);
    }

    void vibrate(int inerval) {
        Vibrator vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vbr.vibrate(VibrationEffect.createOneShot(inerval, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vbr.vibrate(inerval);
        }
    }
}