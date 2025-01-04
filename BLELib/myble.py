# BLE Lib
# Mike Sevbo 2024
# version 0.3
# Supports Lego Hubs Invenror / Spike Prime with legacy firmware

import bluetooth
import random
import struct
import time
import micropython
import ubinascii
import hub
from micropython import const

_IRQ_GATTC_SERVICE_DONE = const(10)
_IRQ_GATTC_WRITE_DONE = const(17)

_FLAG_READ = const(0x0002)
_FLAG_WRITE_NO_RESPONSE = const(0x0004)
_FLAG_WRITE = const(0x0008)
_FLAG_NOTIFY = const(0x0010)
_FLAG_READ_NOTIFY = const(0x0012)
_FLAG_WRITE_WRITE_NO_RESPONSE = const(0x000C)
_FLAG_WRITE_WRITE_NO_RESPONSE_NOTIFY = const(0x001C)

hub_type = hub.info().get("product_variant")
if (hub_type == 1):
    print("Inventor mode")
    from mindstorms import MSHub, Motor, ColorSensor, DistanceSensor

    hubprime = MSHub()
    _IRQ_GATTS_WRITE = 3
    _IRQ_SCAN_RESULT = 5
    _IRQ_SCAN_DONE = 6
    _IRQ_PERIPHERAL_CONNECT = 7
    _IRQ_PERIPHERAL_DISCONNECT = 8
    _IRQ_GATTC_SERVICE_RESULT = 9
    _IRQ_GATTC_CHARACTERISTIC_RESULT = 11
    _IRQ_GATTC_READ_RESULT = 15
    _IRQ_GATTC_NOTIFY = 18
    _IRQ_GATTC_CHARACTERISTIC_DONE = 12
    _IRQ_GATTC_DESCRIPTOR_RESULT = 13
    _IRQ_GATTC_DESCRIPTOR_DONE = 14
    _IRQ_GATTC_INDICATE = 19
else:
    print("Spike mode")
    from spike import PrimeHub, Motor, ColorSensor, DistanceSensor

    hubprime = PrimeHub()
    """
    # For old Spike firmware
    _IRQ_GATTS_WRITE = 1<<2
    _IRQ_SCAN_RESULT = 1 << 4
    _IRQ_SCAN_DONE = 1 << 5
    _IRQ_PERIPHERAL_CONNECT = 1 << 6
    _IRQ_PERIPHERAL_DISCONNECT = 1 << 7
    _IRQ_GATTC_SERVICE_RESULT = 1 << 8
    _IRQ_GATTC_CHARACTERISTIC_RESULT = 1 << 9
    _IRQ_GATTC_DESCRIPTOR_RESULT = 1 << 10
    _IRQ_GATTC_DESCRIPTOR_DONE = 14 #?
    _IRQ_GATTC_READ_RESULT = 1 << 11
    _IRQ_GATTC_NOTIFY = 1 << 13
    _IRQ_GATTC_CHARACTERISTIC_DONE = 1 << 12
    """
    _IRQ_GATTS_WRITE = 3
    _IRQ_SCAN_RESULT = 5
    _IRQ_SCAN_DONE = 6
    _IRQ_PERIPHERAL_CONNECT = 7
    _IRQ_PERIPHERAL_DISCONNECT = 8
    _IRQ_GATTC_SERVICE_RESULT = 9
    _IRQ_GATTC_CHARACTERISTIC_RESULT = 11
    _IRQ_GATTC_READ_RESULT = 15
    _IRQ_GATTC_NOTIFY = 18
    _IRQ_GATTC_CHARACTERISTIC_DONE = 12
    _IRQ_GATTC_DESCRIPTOR_RESULT = 13
    _IRQ_GATTC_DESCRIPTOR_DONE = 14
    _IRQ_GATTC_INDICATE = 19

_NOTIFY_ENABLE = const(1)
_INDICATE_ENABLE = const(2)

# Hubs
_UART_UUID = bluetooth.UUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
_UART_TX = (
    bluetooth.UUID("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"),
    _FLAG_READ | _FLAG_NOTIFY,
)
_UART_RX = (
    bluetooth.UUID("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
    _FLAG_WRITE | _FLAG_WRITE_NO_RESPONSE,
)
_UART_SERVICE = (
    _UART_UUID,
    (_UART_TX, _UART_RX),
)

# Apps
_UART_UUID2 = bluetooth.UUID("7E400001-B5A3-F393-E0A9-E50E24DCCA9E")
_UART_TX2 = (
    bluetooth.UUID("7E400003-B5A3-F393-E0A9-E50E24DCCA9E"),
    _FLAG_READ | _FLAG_NOTIFY,
)
_UART_RX2 = (
    bluetooth.UUID("7E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
    _FLAG_WRITE | _FLAG_WRITE_NO_RESPONSE,
)
_UART_SERVICE2 = (
    _UART_UUID2,
    (_UART_TX2, _UART_RX2),
)

_IRQ_CENTRAL_CONNECT = 1
_IRQ_CENTRAL_DISCONNECT = 2

# Advertising payloads are repeated packets of the following form:
# 1 byte data length (N + 1)
# 1 byte type (see constants below)
# N bytes type-specific data

_ADV_TYPE_FLAGS = const(0x01)
_ADV_TYPE_NAME = const(0x09)
_ADV_TYPE_UUID16_COMPLETE = const(0x3)
_ADV_TYPE_UUID32_COMPLETE = const(0x5)
_ADV_TYPE_UUID128_COMPLETE = const(0x7)
_ADV_TYPE_UUID16_MORE = const(0x2)
_ADV_TYPE_UUID32_MORE = const(0x4)
_ADV_TYPE_UUID128_MORE = const(0x6)
_ADV_TYPE_APPEARANCE = const(0x19)


class Connections:
    _conns = list()
    _tx = dict()
    _rx = dict()
    _device_type = dict()

    def __init__(self):
        self._conns = []
        self._tx = {}
        self._rx = {}
        self._device_type = {}

    def addConn(self, handle, tx, rx, device_type):
        self._conns.append(handle)
        self._tx[handle] = tx
        self._rx[handle] = rx
        self._device_type[handle] = device_type

    def getConns(self):
        return self._conns

    def getConn(self, handle):
        return (tuple((handle, self._tx[handle], self._rx[handle], self._device_type[handle])))

    def getConnsCount(self):
        return len(self._conns)

    def removeConn(self, handle):
        self._conns.remove(handle)
        del self._tx[handle]
        del self._rx[handle]
        del self._device_type[handle]

    def removeAllConn(self):
        for conn_handle in self._conns:
            self._conns.remove(conn_handle)
            del self._tx[conn_handle]
            del self._rx[conn_handle]
            del self._device_type[conn_handle]
        self._conns = []
        self._tx = {}
        self._rx = {}
        self._device_type = {}

    def getRx(self, handle):
        return self._rx[handle]

    def getTx(self, handle):
        return self._tx[handle]

    def getDeviceType(self, handle):
        return self._device_type[handle]

    def printConns(self):
        for conn_handle in self._conns:
            print(tuple((conn_handle, self._tx[conn_handle], self._rx[conn_handle], self._device_type[conn_handle])))


class Encode:
    def decode_field(self, payload, adv_type):
        i = 0
        result = []
        while i + 1 < len(payload):
            if payload[i + 1] == adv_type:
                result.append(payload[i + 2: i + payload[i] + 1])
            i += 1 + payload[i]
        return result

    def decode_name(self, payload):
        n = self.decode_field(payload, _ADV_TYPE_NAME)
        return str(n[0], "utf-8") if n else ""

    def company_identifier(self, payload):
        decode = self.decode_field(payload, const(0xFF))
        if not decode: return 0
        return ubinascii.hexlify(struct.pack('<h', *struct.unpack('>h', decode[0])))

    def company_data(self, payload):
        decode = self.decode_field(payload, const(0xFF))
        if not decode: return []
        return decode[0][2:]

    def decode_services(self, payload):
        services = []
        for u in self.decode_field(payload, _ADV_TYPE_UUID16_COMPLETE): services.append(
            bluetooth.UUID(struct.unpack("<h", u)[0]))
        for u in self.decode_field(payload, _ADV_TYPE_UUID32_COMPLETE): services.append(
            bluetooth.UUID(struct.unpack("<d", u)[0]))
        for u in self.decode_field(payload, _ADV_TYPE_UUID128_COMPLETE): services.append(bluetooth.UUID(u))
        return services

    def encode_bytes(self, byte_array):
        return struct.pack('%sb' % len(byte_array), *byte_array)


# PowerUp hub class
class PowerUp:
    def __init__(self):
        self._encode = Encode()
        self._is_remote_connected = False
        self._is_train_connected = False

    remote_device_uuids = (
    bluetooth.UUID("00001623-1212-efde-1623-785feabcd123"), bluetooth.UUID("00001624-1212-efde-1623-785feabcd123"),
    bluetooth.UUID("00001624-1212-efde-1623-785feabcd123"))  # primary, tx, rx
    _state = [''] * 7
    _pressed = tuple()

    Left_Button = [{0x0: str(), 0x1: 'LEFT_PLUS'}, {0x0: str(), 0x1: 'LEFT'}, {0x0: str(), 0x1: 'LEFT_MINUS'}]
    Right_Button = [{0x0: str(), 0x1: 'RIGHT_PLUS'}, {0x0: str(), 0x1: 'RIGHT'}, {0x0: str(), 0x1: 'RIGHT_MINUS'}]
    Center_Button = {0x0: str(), 0x1: 'CENTER'}

    LEFT_PLUS = 'LEFT_PLUS'
    LEFT = 'LEFT'
    LEFT_MINUS = 'LEFT_MINUS'
    RIGHT_PLUS = 'RIGHT_PLUS'
    RIGHT = 'RIGHT'
    RIGHT_MINUS = 'RIGHT_MINUS'
    CENTER = 'CENTER'

    COLOR_OFF = const(0x00)
    COLOR_PINK = const(0x01)
    COLOR_PURPLE = const(0x02)
    COLOR_BLUE = const(0x03)
    COLOR_LIGHTBLUE = const(0x04)
    COLOR_LIGHTGREEN = const(0x05)
    COLOR_GREEN = const(0x06)
    COLOR_YELLOW = const(0x07)
    COLOR_ORANGE = const(0x08)
    COLOR_RED = const(0x09)
    COLOR_WHITE = const(0x0A)

    def readState(self, data):
        if data[0] == 0x5 and data[2] == 0x8 and data[3] == 0x2:
            self._state[6] = self.Center_Button[data[4]]
        if data[0] == 0x7 and data[2] == 0x45:
            if data[3] == 0x0:
                self._state[0] = self.Left_Button[0][data[4]]
                self._state[1] = self.Left_Button[1][data[5]]
                self._state[2] = self.Left_Button[2][data[6]]
            if data[3] == 0x1:
                self._state[3] = self.Right_Button[0][data[4]]
                self._state[4] = self.Right_Button[1][data[5]]
                self._state[5] = self.Right_Button[2][data[6]]
        self._pressed = tuple([i for i in self._state if i != str()])

    def setSpeed(self, speed):
        value = struct.pack('BBBBBB', 0x81, 0x00, 0x11, 0x51, 0x00, speed)
        return struct.pack("BB", len(value) + 2, 0x00) + value

    def setSpeed2(self, speed):
        value = struct.pack('BBBBBB', 0x81, 0x01, 0x11, 0x51, 0x00, speed)
        return struct.pack("BB", len(value) + 2, 0x00) + value

    def setRemoteColor(self, color):
        return self._encode.encode_bytes([0x08, 0x00, 0x81, 0x34, 0x11, 0x51, 0x00, color])

    def setHubColor(self, color):
        value = struct.pack('BBBBBB', 0x81, 0x32, 0x11, 0x51, 0x00, color)
        return struct.pack("BB", len(value) + 2, 0x00) + value

    def getMessageWithLen(self, value):
        return struct.pack("BB", len(value) + 2, 0x00) + value


# Generate a payload to be passed to gap_advertise(adv_data=...).
def advertising_payload(limited_disc=False, br_edr=False, name=None, services=None, appearance=0):
    payload = bytearray()

    def _append(adv_type, value):
        nonlocal payload
        payload += struct.pack("BB", len(value) + 1, adv_type) + value

    _append(
        _ADV_TYPE_FLAGS,
        struct.pack("B", (0x01 if limited_disc else 0x02) + (0x18 if br_edr else 0x04)),
    )

    if name:
        _append(_ADV_TYPE_NAME, name)

    if services:
        for uuid in services:
            b = bytes(uuid)
            if len(b) == 2:
                _append(_ADV_TYPE_UUID16_COMPLETE, b)
            elif len(b) == 4:
                _append(_ADV_TYPE_UUID32_COMPLETE, b)
            elif len(b) == 16:
                _append(_ADV_TYPE_UUID128_COMPLETE, b)

    # See org.bluetooth.characteristic.gap.appearance.xml
    if appearance:
        _append(_ADV_TYPE_APPEARANCE, struct.pack("<h", appearance))

    return payload


class MyBle:
    def __init__(self, ble=None):
        if ble == None:
            ble = bluetooth.BLE()
        self._ble = ble
        self._ble.active(True)
        self._ble.irq(self._irq)
        ((self._handle_tx, self._handle_rx),) = self._ble.gatts_register_services((_UART_SERVICE,))
        self._connections = Connections()
        self._reset()
        self._lego_name = "LegoHub"
        self._payload = advertising_payload(name=self._lego_name, services=[_UART_UUID], appearance=0)
        self._notify_callback = None
        self._debug_level = 0
        self._encode = Encode()

    def _reset(self):
        # Cached name and address from a successful scan.
        self._name = None
        self._addr_type = None
        self._addr = None

        # Callbacks for completion of various operations.
        # These reset back to None after being invoked.
        self._scan_callback = None
        self._conn_callback = None
        self._read_callback = None

        # Persistent callback for when new data is notified from the device.
        # self._notify_callback = show_logo

        # Connected device.
        self._conn_handle = None
        self._start_handle = None
        self._end_handle = None
        self._tx_handle = None
        self._rx_handle = None
        self._device_type = None

        self._n = 0

    def _on_scan(self, addr_type, addr, search_name):
        if addr_type is not None:
            if (self._debug_level == 1 or self._debug_level == 2): print("Found peripheral:", addr_type, addr,
                                                                         search_name)
            time.sleep_ms(500)
            self.connect()
        else:
            self.timed_out = True
            print("No uart peripheral 'name: {}, addr: {}' found.".format(self._search_name, self._search_addr))

    def scan_connect(self, search_name=None, search_addr=None, search_uuids=tuple()):
        self.timed_out = False
        self.scan(search_name=search_name, search_addr=search_addr, search_uuids=search_uuids, callback=self._on_scan)
        while not self.is_connected() and not self.timed_out:
            # print("Waiting for connection... connected:{}, timed out:{}".format(self.is_connected(), self.timed_out))
            time.sleep_ms(100)
        return not self.timed_out

    def _irq(self, event, data):
        if event not in (_IRQ_SCAN_DONE, _IRQ_SCAN_RESULT, _IRQ_PERIPHERAL_CONNECT, _IRQ_PERIPHERAL_DISCONNECT,
                         _IRQ_GATTC_SERVICE_RESULT, _IRQ_GATTC_SERVICE_DONE, _IRQ_GATTC_CHARACTERISTIC_RESULT,
                         _IRQ_GATTC_WRITE_DONE, _IRQ_GATTC_NOTIFY, _IRQ_GATTC_READ_RESULT,
                         _IRQ_GATTC_CHARACTERISTIC_DONE,
                         _IRQ_CENTRAL_CONNECT, _IRQ_CENTRAL_DISCONNECT, _IRQ_GATTS_WRITE, _IRQ_GATTC_DESCRIPTOR_RESULT,
                         _IRQ_GATTC_DESCRIPTOR_DONE):
            if (self._debug_level == 1 or self._debug_level == 2): print("Unexpected event: ", event, hex(event))

        # Track connections so we can send notifications.
        if event == _IRQ_CENTRAL_CONNECT:
            conn_handle, _, _ = data
            if (self._debug_level == 1 or self._debug_level == 2): print("New Central connection", conn_handle)
            self._connections.addConn(conn_handle, 0, 0, 0)
            self._connected = True

        elif event == _IRQ_CENTRAL_DISCONNECT:
            conn_handle, _, _ = data
            if (self._debug_level == 1 or self._debug_level == 2): print("Disconnected from central", conn_handle)
            self._connections.removeConn(conn_handle)
            self._connected = False
            # Start advertising again to allow a new connection.
            self._advertise()

        elif event == _IRQ_SCAN_RESULT:
            addr_type, addr, adv_type, rssi, adv_data = data
            if (self._debug_level == 2): print(
                'type:{} addr:{} name:{} adv_type: {} rssi:{} data:{}'.format(addr_type, ubinascii.hexlify(addr), name,
                                                                              adv_type, rssi,
                                                                              ubinascii.hexlify(adv_data)))
            name = self._encode.decode_name(adv_data) or "?"
            device_type = self._encode.company_data(adv_data)
            company_identifier = self._encode.company_identifier(adv_data)
            svcs = self._encode.decode_services(adv_data)
            if len(svcs) > 0:
                primary_uuid = svcs[0]
            else:
                primary_uuid = ""

            if len(self._search_uuids) == 3:
                search_uuid = self._search_uuids[0]
            else:
                search_uuid = ""
            if name == self._search_name or addr == self._search_addr or primary_uuid == _UART_UUID or primary_uuid == _UART_UUID2 or (
                    primary_uuid == search_uuid and search_uuid != ""):
                self._addr_type = addr_type
                self._addr = bytes(addr)  # Note: addr buffer is owned by caller so need to copy it.
                self._name = name
                if len(device_type) > 0:
                    self._device_type = device_type[1]
                else:
                    self._device_type = 0

                if self._device_type == 0:
                    if primary_uuid == _UART_UUID: self._device_type = 1
                    if primary_uuid == _UART_UUID2: self._device_type = 2
                self._company_identifier = company_identifier
                self._primary_uuid = primary_uuid
                # ... and stop scanning. This triggers the IRQ_SCAN_DONE and the on_scan callback.
                self._ble.gap_scan(None)

        elif event == _IRQ_GATTC_CHARACTERISTIC_DONE:
            time.sleep_ms(600)
            # Called once service discovery is complete.
            # Note: Status will be zero on success, implementation-specific value otherwise.
            conn_handle, status = data
            # print('Discover characteristics Done. Status: ', str(status))
            if self._start_handle and self._end_handle:
                self._ble.gattc_discover_descriptors(
                    self._conn_handle, self._start_handle, self._end_handle
                )

        elif event == _IRQ_SCAN_DONE:
            if self._scan_callback:
                if self._addr:
                    # Found a device during the scan (and the scan was explicitly stopped).
                    self._scan_callback(self._addr_type, self._addr, self._name)
                    self._scan_callback = None
                else:
                    # Scan timed out.
                    self._scan_callback(None, None, None)

        elif event == _IRQ_PERIPHERAL_CONNECT:
            # Connect successful.
            if (self._debug_level == 1 or self._debug_level == 2): print("Peripheral Connect")
            conn_handle, addr_type, addr = data
            if addr_type == self._addr_type and addr == self._addr:
                self._conn_handle = conn_handle
                self._ble.gattc_discover_services(self._conn_handle)

        elif event == _IRQ_PERIPHERAL_DISCONNECT:
            # Disconnect (either initiated by us or the remote end).
            conn_handle, _, _ = data
            if conn_handle == self._conn_handle:
                # If it was initiated by us, it'll already be reset.
                self._reset()
                if (self._debug_level == 1 or self._debug_level == 2): print("Disconnect from peripheral")
                self.timed_out = True

        elif event == _IRQ_GATTC_SERVICE_RESULT:
            # Connected device returned a service.
            conn_handle, start_handle, end_handle, uuid = data
            self._n += 1
            if conn_handle == self._conn_handle and (
                    uuid == self._primary_uuid or uuid == _UART_UUID or uuid == _UART_UUID2):
                self._start_handle, self._end_handle = start_handle, end_handle
                time.sleep_ms(500)
                self._ble.gattc_discover_characteristics(self._conn_handle, start_handle, end_handle)

        elif event == _IRQ_GATTC_SERVICE_DONE:
            time.sleep_ms(600)
            # Service query complete.
            if self._start_handle and self._end_handle:
                self._ble.gattc_discover_characteristics(
                    self._conn_handle, self._start_handle, self._end_handle
                )
            else:
                if (self._debug_level == 1 or self._debug_level == 2): print("Failed to find uart service.")
                self.timed_out = True

        elif event == _IRQ_GATTC_CHARACTERISTIC_RESULT:
            # Connected device returned a characteristic.
            conn_handle, def_handle, value_handle, properties, uuid = data
            if len(self._search_uuids) == 3:
                uart_tx = self._search_uuids[1]
                uart_rx = self._search_uuids[2]
            else:
                uart_tx = ""
                uart_rx = ""

            if conn_handle == self._conn_handle:
                if (uuid == _UART_RX[0] or uuid == _UART_RX2[0] or uuid == uart_rx) and (
                        properties == _FLAG_WRITE or properties == _FLAG_WRITE_WRITE_NO_RESPONSE):
                    self._rx_handle = value_handle
                    if (self._debug_level == 2): print("RX handle: " + str(value_handle))
                elif (uuid == _UART_TX[0] or uuid == _UART_TX2[0] or uuid == uart_tx) and (
                        properties == _FLAG_READ or properties == _FLAG_READ_NOTIFY or properties == _FLAG_NOTIFY):
                    self._tx_handle = value_handle
                    if (self._debug_level == 2): print("TX handle: " + str(value_handle))
                # Combined mode only TX+RX
                elif (uuid == _UART_RX[0] or uuid == _UART_TX[0] or uuid == _UART_RX2[0] or uuid == _UART_TX2[
                    0] or uuid == uart_tx or uuid == uart_rx) and (_FLAG_WRITE_WRITE_NO_RESPONSE_NOTIFY):
                    self._rx_handle = value_handle
                    self._tx_handle = value_handle
                    if (self._debug_level == 2): print("RX/TX handle: " + str(value_handle))

        elif event == _IRQ_GATTC_DESCRIPTOR_RESULT:
            # Called for each descriptor found by gattc_discover_descriptors().
            conn_handle, dsc_handle, uuid = data
            print('event == _IRQ_GATTC_DESCRIPTOR_RESULT')
            print(conn_handle, dsc_handle, uuid)
            print('Discover descriptors')
            print("Handle: " + str(dsc_handle) + " UUID: " + str(uuid))

        elif event == _IRQ_GATTC_DESCRIPTOR_DONE:
            # Called once service discovery is complete.
            # Note: Status will be zero on success, implementation-specific value otherwise.
            conn_handle, status = data
            # print('Discover descriptors Done. Status: ', str(status))

        elif event == _IRQ_GATTC_WRITE_DONE:
            conn_handle, value_handle, status = data
            # print("Status: ", status)
            if (self._debug_level == 1 or self._debug_level == 2): print("TX complete")

        elif event == _IRQ_GATTC_NOTIFY:
            # print("_IRQ_GATTC_NOTIFY")
            conn_handle, value_handle, notify_data = data
            notify_data = bytes(notify_data)
            if (self._debug_level == 2): print(notify_data)

            # TODO: Need update this logic
            # if conn_handle == self._conn_handle and value_handle == self._tx_handle:
            if self._notify_callback:
                self._notify_callback(notify_data, conn_handle)

        elif event == _IRQ_GATTC_READ_RESULT:
            # print("_IRQ_GATTC_READ_RESULT")
            # A read completed successfully.
            conn_handle, value_handle, char_data = data

            # TODO: Need update this logic
            if conn_handle == self._conn_handle and value_handle in (
            self._rx_handle, self._buta_handle, self._butb_handle):
                if (self._debug_level == 2): print("handle,READ data", value_handle, bytes(char_data))
                self._read_callback(value_handle, bytes(char_data))

        elif event == _IRQ_GATTS_WRITE:
            conn_handle, value_handle = data
            value = self._ble.gatts_read(value_handle)
            if (self._debug_level == 2): print("Gatts Write")

            # Check connected device type Hub or App remote, then update the connections
            if len(value) == 3:
                a = struct.unpack("BBB", value)
                if a[0] == 0xfe and a[1] == 0x01 and a[2] == 0x10:
                    self._connections._device_type[conn_handle] = 2
                    if (self._debug_level == 1 or self._debug_level == 2): print("App remote connected")
                if a[0] == 0xfa and a[1] == 0x01 and a[2] == 0x10:
                    self._connections._device_type[conn_handle] = 1
                    if (self._debug_level == 1 or self._debug_level == 2): print("Lego hub connected")

            if value_handle == self._handle_rx and self._write_callback:
                self._write_callback(value, conn_handle)

    def send(self, data):
        for conn_handle in self._connections.getConns():
            if (self._debug_level == 2): print("Send data for cons: " + str(conn_handle))
            self._ble.gatts_notify(conn_handle, self._handle_tx, data)

    # Returns true if we've successfully connected and discovered uart characteristics.
    def is_connected(self):
        return (
                self._conn_handle is not None
                and self._tx_handle is not None
                and self._rx_handle is not None
        )

    def _advertise(self, interval_us=100000):
        if (self._debug_level == 1 or self._debug_level == 2): print("Starting advertising")
        self._payload = advertising_payload(name=self._lego_name, services=[_UART_UUID], appearance=0)
        self._ble.gap_advertise(interval_us, adv_data=self._payload)

    # Find a device advertising the uart service.
    def scan(self, search_name=0, search_addr=0, search_uuids=tuple(), callback=0, timeout=20000):
        if callback == 0: callback = self._on_scan
        self._addr_type = None
        self._addr = None
        self._search_name = search_name
        self._search_addr = search_addr
        self._search_uuids = search_uuids
        self._scan_callback = callback
        self._ble.gap_scan(timeout, 30000, 30000)

    # Connect to the specified device (otherwise use cached address from a scan).
    def connect(self, addr_type=None, addr=None, callback=None):
        self._addr_type = addr_type or self._addr_type
        self._addr = addr or self._addr
        self._conn_callback = callback
        if self._addr_type is None or self._addr is None:
            return False
        if (self._debug_level == 1 or self._debug_level == 2): print("Connecting...")
        self._ble.gap_connect(self._addr_type, self._addr)
        return True

    # Disconnect from selected device.
    def disconnect(self, handle):
        self._ble.gap_disconnect(self.handle)
        self._connections.removeConn(handle)
        # self._reset()

    # Disconnect from all devices.
    def disconnectAll(self):
        for conn_handle in self._connections.getConns():
            if (self._debug_level == 1 or self._debug_level == 2): print("Disconnect from: " + str(conn_handle))
            self._ble.gap_disconnect(conn_handle)
            time.sleep_ms(50)
        self._connections.removeAllConn()
        self._reset()

    # Send data over the UART
    def write(self, v, handle, response=False):
        if self._connections.getConnsCount() == 0:
            return
        self._ble.gattc_write(handle, self._connections.getRx(handle), v, 1 if response else 0)

    def on_write(self, callback):
        self._write_callback = callback

    def enable_notify(self):
        if not self.is_connected():
            return
        if (self._debug_level == 1 or self._debug_level == 2): print("Enable notify")
        time.sleep_ms(500)
        self._ble.gattc_write(self._conn_handle, self._tx_handle + 1, struct.pack('<h', _NOTIFY_ENABLE), 0)
        self._ble.gattc_write(self._conn_handle, self._rx_handle + 1, struct.pack('<h', _NOTIFY_ENABLE), 0)
        time.sleep_ms(300)
        self.write(self._encode.encode_bytes([0xFA, 0x01, 0x10]), self._conn_handle)  # Notify that this is Lego hub

    def read(self, handle, callback):
        if not self.is_connected():
            return
        self._read_callback = callback
        try:
            self._ble.gattc_read(self._conn_handle, handle)
        except:
            pass

    # Set handler for when data is received over the UART.
    def on_notify(self, callback):
        if (self._debug_level == 2): print("Notify event")
        self.enable_notify()
        self._notify_callback = callback
        # print("callback", callback)

# ===== End of library ===== #
