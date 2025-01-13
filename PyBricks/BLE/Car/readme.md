### Project Overview

This example includes an implementation of a program to control a car with Lego Hub (Inventor or Spike Prime Hub with PyBrick firmware on board), using an Android smartphone as the joystick.

You can find the sample Android control application [here](https://github.com/czuryk/Lego/tree/main/PyBricks/BLE/Android).

### Features

This program implements functions for controlling a LEGO car, including steering and movement. It also transmits data to the smartphone, such as distance sensor readings and gyroscope data.

Additional functionalities:
- Upon connecting to the hub, the program starts automatically.  
- When disconnected from the hub, the program terminates to allow reconnection via BLE.  
