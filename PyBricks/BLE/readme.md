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
    <h2>Project Structure</h2>
    <p>The test project consists of three subfolders:</p>
    <ol>
        <li>
            <strong>Sample Android Application:</strong>  
            Demo app allows controlling custom LEGO models using joysticks.
        </li>
        <li>
            <strong>Test Pybricks Program:</strong>  
            Offers basic functionality for sending and receiving data.
        </li>
        <li>
            <strong>PyBricks Car Program:</strong>  
            Manages throttle, steering, and a distance sensor, while also transmitting hub parameters such as voltage and gyroscope data.
        </li>
    </ol>
    <h2>Usage Recommendations</h2>
    <p>
        As Pybricks already supports connecting to an Xbox controller and controlling models with it, using an Xbox controller is recommended for simpler models. However, this project was developed for creating more advanced robots with additional sensors.
    </p>
    <p>
        This implementation enables the collection, storage, and analysis of telemetry data from the hub and sensors, supporting more advanced logic and functionality.
    </p>

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

### Connection to Android Smartphone

Connecting to an Android smartphone is only possible in the following cases:  
1. The hub is not already connected to a computer or another device.  
2. The program on the hub is not currently running.  
