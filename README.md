# Terrarium Control Unit running in a JVM on a Raspberry Pi 3+

## The Hardware
The Raspberry Pi3+ has a Wifi and Bluetooth module for remote communication and a 40-pins GPIO connector with the following connections:

<img src="doc/TCU-connector-Pi4J-layout.jpg" width="50%" style="margin-bottom=-10">

The TCU controls the devices:
* AM2302 Temperature and Humidity sensor
* DS18B20 Temperature sensor
* LCD unit (16x2) for displaying sensor information
* 12 on/off relais for switching lights, pump, sprayer, mist, fan-in and fan-out

## The Software
The motor of the TCU is the `TCU.java` file. It initializes and configures the hardware and logic defined in the `Terraria.java` file,
starts a Timer and schedules an EachSecondTask, EachMinuteTask and an EachHourTask.

The EachSecondTask executes the logic that needs to be executed every second.  
The EachMinuteTask executes the logic that needs to be executed every minute.  
The EachHourTask executes the logic that needs to be executed every hour.  

### The Logic
There are two types of logic implemented:
* Timers
* Rules

#### Timer logic
There are two types of timers:
* On/Off Timers  
These timers have an on-time expressed as HH:mm and a off-time expressed as HH:mm
* On Timer for a period of seconds  
These timers have an on-time expressed as HH:mm and no off-time but a period of max 3600 seconds

