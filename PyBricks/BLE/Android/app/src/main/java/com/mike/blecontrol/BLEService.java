package com.mike.blecontrol;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.os.Handler;

import com.welie.blessed.AdvertiseError;
import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothPeripheralManager;
import com.welie.blessed.BluetoothPeripheralManagerCallback;
import com.welie.blessed.GattStatus;
import com.welie.blessed.ReadResponse;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteOrder;
import java.util.Objects;
import java.util.UUID;
import java.util.HashMap;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

import androidx.annotation.NonNull;

class BLEService {
    public static final UUID CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DECSRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID SERVICE_UUID = UUID.fromString("7E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID READ_CHARACTERISTIC_UUID = UUID.fromString("7E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("7E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private BluetoothPeripheralManager peripheralManager;
    private final HashMap<BluetoothGattService, BLEService> serviceImplementations = new HashMap<>();
    private @NotNull final BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, SERVICE_TYPE_PRIMARY);
    private @NotNull final BluetoothGattCharacteristic readChar = new BluetoothGattCharacteristic(READ_CHARACTERISTIC_UUID, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
    private @NotNull final BluetoothGattCharacteristic writeChar = new BluetoothGattCharacteristic(WRITE_CHARACTERISTIC_UUID, PROPERTY_WRITE | PROPERTY_WRITE_NO_RESPONSE, PERMISSION_WRITE);
    private @NotNull final Handler handler = new Handler();
    //private @NotNull final Runnable notifyRunnable = this::sendNotify;
    private static BLEService instance = null;
    int count = 0;

    public static synchronized BLEService getInstance(Context context) {
        if (instance == null) {
            instance = new BLEService(context.getApplicationContext());
        }
        return instance;
    }

    BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
        return new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_DECSRIPTOR_UUID, PERMISSION_READ | PERMISSION_WRITE);
    }
    BluetoothGattDescriptor getCharacteristicUserDescriptionDescriptor() {
        return new BluetoothGattDescriptor(CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID, PERMISSION_READ | PERMISSION_WRITE);
    }

    protected void notifyCharacteristicChanged(final byte[] value, @NotNull final BluetoothGattCharacteristic characteristic) {
        peripheralManager.notifyCharacteristicChanged(value, characteristic);
    }

    boolean noCentralsConnected() {
        return peripheralManager.getConnectedCentrals().size() == 0;
    }

    private final BluetoothPeripheralManagerCallback peripheralManagerCallback = new BluetoothPeripheralManagerCallback() {
        @Override
        public void onServiceAdded(@NotNull GattStatus status, @NotNull BluetoothGattService service) {}

        public ReadResponse onCharacteristicRead(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
            Log.i("my", characteristic.getUuid().toString());
            if (characteristic.getUuid().equals(READ_CHARACTERISTIC_UUID)) {
                return new ReadResponse(GattStatus.SUCCESS, new byte[]{0x00, 0x40});
            }
            return super.onCharacteristicRead(central, characteristic);
        }

        public GattStatus onCharacteristicWrite(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            BluetoothBytesParser parser = new BluetoothBytesParser(value, ByteOrder.LITTLE_ENDIAN);
            Log.i("my", String.valueOf(parser));
            MainActivity.onCentralCharacteristicChanged(String.valueOf(parser));
            //MainActivity.changeControlState(String.valueOf(parser));
            return super.onCharacteristicWrite(central, characteristic, value);
        }

        public void onCharacteristicWriteCompleted(@NonNull BluetoothCentral central, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            //Log.i("my", "Characteristic write complete");
        }

        public ReadResponse onDescriptorRead(@NotNull BluetoothCentral central, @NotNull BluetoothGattDescriptor descriptor) {
            return super.onDescriptorRead(central, descriptor);
        }

        public GattStatus onDescriptorWrite(@NotNull BluetoothCentral central, @NotNull BluetoothGattDescriptor descriptor, byte[] value) {
            return super.onDescriptorWrite(central, descriptor, value);
        }

        public void onNotifyingEnabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
            Log.i("my", "Notify enabled");
            if (characteristic.getUuid().equals(READ_CHARACTERISTIC_UUID)) {
                //sendNotify();
            }
        }

        public void onNotifyingDisabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
            Log.i("my", "Notify disabled");
            if (characteristic.getUuid().equals(READ_CHARACTERISTIC_UUID)) {
                stopNotifying();
            }
        }

        public void onNotificationSent(@NotNull BluetoothCentral central, byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            //Log.i("my", "Notify send");
            super.onNotificationSent(central, value, characteristic, status);
        }

        public void onCentralConnected(@NotNull BluetoothCentral central) {
            Log.i("my", "Central connected");
            //super.onCentralConnected(central);
            MainActivity.onCentralConnected(central);
        }

        public void onCentralDisconnected(@NotNull BluetoothCentral central) {
            Log.i("my", "Central disconnected");
            if (noCentralsConnected()) {
                stopNotifying();
            }
            //super.onCentralDisconnected(central);
            MainActivity.onCentralDisconnected(central);
        }
        public void onAdvertisingStarted(@NotNull AdvertiseSettings settingsInEffect) {}

        public void onAdvertiseFailure(@NotNull AdvertiseError advertiseError) {}

        public void onAdvertisingStopped() {}
    };

    private void stopNotifying() {
        //handler.removeCallbacks(notifyRunnable);
    }

    public void sendNotify(byte[] b) {
        count += 1;
        if (count > 9) count = 0;
        //notifyCharacteristicChanged(String.valueOf(count).getBytes(), readChar);
        notifyCharacteristicChanged(b, readChar);
        //handler.postDelayed(notifyRunnable, 1000);
        //Log.i("my", String.valueOf(count));
    }

    public void startAdvertising() {
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(new ParcelUuid(service.getUuid()))
                .build();

        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        peripheralManager.startAdvertising(advertiseSettings, advertiseData, scanResponse);
    }
    public void stopAdvertising() {
        peripheralManager.stopAdvertising();
    }

    public void disconnectCentrals() {
        peripheralManager.close();
    }

    @SuppressLint("MissingPermission")
    BLEService(Context context) {
        service.addCharacteristic(readChar);
        service.addCharacteristic(writeChar);
        readChar.addDescriptor(getClientCharacteristicConfigurationDescriptor());

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e("my", "bluetooth not supported");
            return;
        }

        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.e("my", "not supporting advertising");
            return;
        }

        // Set the adapter name as this is used when advertising
        bluetoothAdapter.setName(Build.MODEL);
        Log.i("my", Build.MODEL);

        peripheralManager = new BluetoothPeripheralManager(context, bluetoothManager, peripheralManagerCallback);
        peripheralManager.add(service);
        //startAdvertising(service.getUuid());
    }
}
