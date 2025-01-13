from pybricks.hubs import InventorHub
from pybricks.pupdevices import Motor, UltrasonicSensor
from pybricks.parameters import Port, Color, Button, Axis, Direction
from pybricks.tools import wait, StopWatch
from pybricks.robotics import Car
import ustruct

# Standard MicroPython modules
from usys import stdin, stdout
from uselect import poll
import urandom

stopwatch1 = StopWatch()
stopwatch2 = StopWatch()

hub = InventorHub()

steering = Motor(Port.A, Direction.COUNTERCLOCKWISE)
front = Motor(Port.B, Direction.CLOCKWISE)

# Get and populate distance sensor data
dist = UltrasonicSensor(Port.D)

car = Car(steering, [front])

interval1 = 150 # 150ms
interval2 = 3000 # 3s

# Optional: Register stdin for polling. This allows
# you to wait for incoming data without blocking.
keyboard = poll()
keyboard.register(stdin)

#stdout.buffer.write(b"rdy")

while True:
    #Force to stop program
    if Button.CENTER in hub.buttons.pressed():
        break

    while not keyboard.poll(0):
        # Get Gyro velocity
        # Compress the data.
        vx = hub.imu.angular_velocity(axis=Axis.X)
        vxf = round(vx*10)
        vy = hub.imu.angular_velocity(axis=Axis.Y)
        vyf = round(vy*10)
        vz = hub.imu.angular_velocity(axis=Axis.Z)
        vzf = round(vz*10)

        # quick events
        time1 = stopwatch1.time()
        if time1 >= interval1:
            # send gyro data with prefix A
            print("A:"+str(vxf) + ":" + str(vyf) + ":" + str(vzf))
            print("D:"+str(dist.distance()))
            stopwatch1.reset()
        
        # rare events
        time2 = stopwatch2.time()
        if time2 >= interval2:
            # send voltage data with prefix V
            print("V:", hub.battery.voltage())
            stopwatch2.reset()        
        wait(1)

    try:
        cmd = stdin.buffer.read(4)
    except: # Exception as e:
        print("Buffer read error")
        continue
    else:
        a, b, c, d = ustruct.unpack("BBBB", cmd)
        
        # Terminate program by command from remote
        # This bytes should be sent before remote disconnect
        if a == 0xFF and b == 0xFE: 
            break

        a = (a - 127)
        b = (b - 127)
        c = (c - 127)
        d = (d - 127)

        car.steer(a)
        car.drive_power(d)
