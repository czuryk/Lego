from pybricks.hubs import InventorHub
from pybricks.pupdevices import Motor
from pybricks.parameters import Port, Color, Button, Axis
from pybricks.tools import wait, StopWatch
import ustruct

# Standard MicroPython modules
from usys import stdin, stdout
from uselect import poll
import urandom

stopwatch1 = StopWatch()
stopwatch2 = StopWatch()

hub = InventorHub()

# We use two motors for test here
motor1 = Motor(Port.A)
motor2 = Motor(Port.B)

interval1 = 150
interval2 = 3000

# Optional: Register stdin for polling. This allows
# you to wait for incoming data without blocking.
keyboard = poll()
keyboard.register(stdin)

stdout.buffer.write(b"rdy")
while True:
    # Force program stop
    if Button.CENTER in hub.buttons.pressed():
        break

    # Let the remote program know we are ready for a command.

    # Optional: Check available input.
    
    while not keyboard.poll(0):
        # get and simplify gyro data
        vx = hub.imu.angular_velocity(axis=Axis.X)
        vxf = round(vx*10)
        vy = hub.imu.angular_velocity(axis=Axis.Y)
        vyf = round(vy*10)
        vz = hub.imu.angular_velocity(axis=Axis.Z)
        vzf = round(vz*10)

        # Fast timer
        time1 = stopwatch1.time()
        if time1 >= interval1:
            # send hub gyro data
            print("A:"+str(vxf) + ":" + str(vyf) + ":" + str(vzf))
            stopwatch1.reset()

        # Slow timer
        time2 = stopwatch2.time()
        if time2 >= interval2:
            send hub voltage
            print("V:", hub.battery.voltage())
            stopwatch2.reset()        
        # Optional: Do something here.

        wait(1)

    try:
        cmd = stdin.buffer.read(4)
    except: # Exception as e:
        print("Buffer read error")
        continue
    else:
        a, b, c, d = ustruct.unpack("BBBB", cmd)
        if a == 0xFF and b == 0xFE: 
            break

        a = (a - 127)
        b = (b - 127)
        c = (c - 127)
        d = (d - 127)
        # print all values
        if c != 0: print(a, b, c, d)
          
        motor1.dc(a)
        motor2.dc(b)
