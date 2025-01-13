<h1>Pybricks Android remote control</h1>
<p>
        This example includes a sample Android application that connects to a LEGO Hub running Pybricks firmware.
    </p>
    <p>
        Since Pybricks provides its own BLE connection implementation, this project includes all the necessary functionality.
    </p>
    <p>
            This approach leverages this recommended method (https://pybricks.com/projects/tutorials/wireless/hub-to-device/pc-communication/) to implement the connection to the hub using BLE.
    </p>
    <h2>The application functions</h2>
    <ol>
        <li>
            <strong>GATT Client</strong>  
            The GATT client allows to establish a connection to the hub in advestering mode.
            <strong>Currently fully supported function.</strong>
        </li>
        <li>
            <strong>GATT Server</strong>
            Turn the Android smartphone to fully functional GATT server. This allows other GATT client to establish a connection to smartphone.
            <strong>Currently unsupported function with PyBricks firmware.</strong>
        </li>
    </ol>

<h2>Android demo app screenshot</h2>
<div align="center">
        <img src="https://github.com/czuryk/Lego/blob/main/PyBricks/BLE/Android/Screenshot_20250113_023219_BLEcontrol.jpg" />
</div>
<br>

<h2>Watch the demo on YouTube</h2>
<div align="center">
    <a href="https://youtube.com/shorts/6E0ajDd5fnM">
        <img src="https://img.youtube.com/vi/6E0ajDd5fnM/0.jpg" alt="Watch the demo" />
    </a>
</div>
<br>

<p><a href="https://youtube.com/shorts/6E0ajDd5fnM">https://youtube.com/shorts/6E0ajDd5fnM</a></p>

<h2>Important Note</h2>

<p>Please note that the first launch of the Android application might unexpectedly crash due to an incomplete implementation of the permission request logic.</p>

<p>To resolve this, locate the application in your device's app settings and manually grant the necessary permissions for BLE functionality or do this in permission request popup's. This step is required only once.</p>
