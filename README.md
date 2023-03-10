# Terrarium Control Unit running in a JVM on a Raspberry Pi 3+

## The Hardware
The Raspberry Pi3+ has a Wifi and Bluetooth module for remote communication and a 40-pins GPIO connector with the following connections:

<img src="doc/TCU-connector-Pi4J-layout.jpg" width="50%" style="margin-bottom=-10">

The TCU controls the devices:
* AM2302 Temperature and Humidity sensor
* DS18B20 Temperature sensor
* LCD unit (16x2) for displaying sensor information
* 12 on/off relais for switching lights, pump, sprayer, mist, fan-in and fan-out

### The Logic
There are two types of logic implemented:
* Timers
* Rules

#### Timer logic
There are two types of timers:
* On/Off Timers  
These timers have an on-time expressed as HH:mm and a off-time expressed as HH:mm
* Period Timers   
These timers have an on-time expressed as HH:mm and no off-time but a period of max 3600 seconds

#### Rule logic
There are two types of rules:
* Rule that manages the temperature
    * When the temperature drops below the ideal temperature what are the actions to take
    * When the temperature rises above the ideal temperature what are the actions to take
* Rule that manages the actions after the Sprayer device has been activated (drying rule).
    * When the sprayer has been switched off then after a delay-period what are the actions to take

#### Specific device related logic
* the Mist device  
When the Mist device is switched on then the Fan-in and Fan-out devices are always switched off
* the Sprayer device  
When the Sprayer device is switched on then the Fan-in and Fan-out devices are always switched off

## The Software
The software consist of two main parts:
* Terrarium Management
* Terrarium Management Configuration
    * using Bluetooth
    * using Wifi

### Terrarium Management
The motor of the TCU is the `TCU.java` file. It initializes and configures the hardware and logic defined in the `Terraria.java` file,
starts a Timer and schedules an EachSecondTask, EachMinuteTask and an EachHourTask.

* The EachSecondTask (`EachSecondTask.java`)executes the logic that needs to be executed every second.  
&nbsp;&nbsp;&nbsp;&nbsp;Since there are Period Timers that have a accuracy in seconds each second the timers are checked.  

* The EachMinuteTask (`EachMinuteTask.java`) executes the logic that needs to be executed every minute.  
&nbsp;&nbsp;&nbsp;&nbsp;The rules are checked each minute.  

* The EachHourTask (`EachHourTask.java`) executes the logic that needs to be executed every hour.  
&nbsp;&nbsp;&nbsp;&nbsp;The Lifecycle counter that can be linked to a Light device is updated each hour.  
&nbsp;&nbsp;&nbsp;&nbsp;The Trace function is switched on on a full hour.  

### Terrarium Management Configuration
The data that can be set is defined in the `TerrariumConfig.java` class. This class is converted to JSON and stored on disk in the `settings.json` file on each change in the configuration. It is read in whenever the Pi is rebooted.  
For the TCU configuration two services are implemented each with its own API:
* a Bluetooth service
* a REST webservice using Wifi

#### using Bluetooth
The Bluetooth service implements a BluetoothSocket listener (see `BTServer.java`). Each Bluetooth device must have its own unique UUID. This UUID is defined in the `config.properties` file. It also needs a host name, also defined in the `config.properties` file.  
When the TCU is started this name is broadcasted so any Bluetooth client can receive it.

The Bluetooth service is a Request/Response message service. The Request received is a JSON string converted to a Command object (`Command.java`) which contains a unique messageId, a command and a JsonObject which contains the data that is linked to the command.

#### Bluetooth API

__Command__ : getProperties

__Data__ : None

__Response__ :
<pre><code>{"nr_of_timers":23,"nr_of_programs":2,"devices":[
{"device":"light1", "nr_of_timers":1, "lc_counted":false},
....
{"device":"spare", "nr_of_timers":5, "lc_counted":false}
]}</code></pre>

---
__Command__ : `getSensors`

__Data__ : None

__Response__ :
<pre><code>{"clock": "08-03-2023 19:19", 
 "sensors": [ {"location":"room", "temperature":21, "humidity":45 },{"location":"terrarium", "temperature":26} ]
}</code></pre>

---
__Command__ : `setSensors`

__Data__ : 
<pre><code>{"roomtemp":21, "terrtemp":26}</code></pre>

__Response__ : None

---
__Command__ : `getState`

__Data__ : None

__Response__ :
<pre><code>{"trace":"off","state": [
{"name":"light1", "onPeriod":-1, "manual":false},
{"name":"uvlight", "onPeriod":-1, "lifetime": 4398, "manual":false},
...
{"name":"spare", "onPeriod":0, "manual":false}
]}</code></pre>

---
__Command__ : `setDeviceOn`

__Data__ :
<pre><code>{"device":"light1"}</code></pre>

__Response__ : None

---
__Command__ : `setDeviceOff`

__Data__ : <pre><code>{"device":"light1"}</code></pre>

__Response__ : None

---
__Command__ : `setDeviceOnFor`

__Data__ :
<pre><code>{"device":"sprayer", "period":10}</code></pre>

__Response__ : None

---
__Command__ : `setDeviceManualOn`

__Data__ :
<pre><code>{"device":"light1"}</code></pre>

__Response__ : None

---
__Command__ : `setDeviceManualOff`

__Data__ : <pre><code>{"device":"light1"}</code></pre>

__Response__ : None

---
__Command__ : `setLifecycleCounter`

__Data__ : <pre><code>{"device":"light1", "hoursOn":4400}</code></pre>

__Response__ : None

---
__Command__ : `setTraceOn`

__Data__ : None

__Response__ : None

---
__Command__ : `setTraceOff`

__Data__ : None

__Response__ : None

---
__Command__ : `getTimersForDevice`

__Data__ : <pre><code>{"device":"light1"}</code></pre>

__Response__ : 
<pre><code>{"timers":[
{"device":"light1","index":1,"hour_on":9,"hour_off":0,"minute_on":21,"minute_off":0,"period":0,"repeat":1}
]}</code></pre>

---
__Command__ : `replaceTimers`

__Data__ : 
<pre><code>{"timers":[
{"device":"light1","index":1,"hour_on":9,"hour_off":0,"minute_on":1,"minute_off":0,"period":0,"repeat":1}
]}</code></pre>

__Response__ : None

---
__Command__ : `getRuleset`

__Data__ : <pre><code>{"rulesetnr":1}</code></pre>

__Response__ : 
<pre><code>{"active": "yes", "from": "10:30", "to": "21:00", "temp_ideal": 26,
    "rules": [
        {"value":-25, "actions":
	        [
	        	{"device": "fan_in", "on_period": -2},
	            {"device": "no device", "on_period": 0}
	        ],
        },
        {"value": 27, "actions":
	        [
	        	{"device": "fan_out", "on_period": -2},
	            {"device": "no device", "on_period": 0}
	        ],
        }
    ]
}</code></pre>

---
__Command__ : `setRuleset`

__Data__ : 
<pre><code>{"rulesetnr":1, "ruleset":
{"active": "yes", "from": "10:30", "to": "21:00", "temp_ideal": 26,
    "rules": [
        {"value":-25, "actions":
	        [
	        	{"device": "fan_in", "on_period": -2},
	            {"device": "no device", "on_period": 0}
	        ],
        },
        {"value": 27, "actions":
	        [
	        	{"device": "fan_out", "on_period": -2},
	            {"device": "no device", "on_period": 0}
	        ],
        }
    ]
}</code></pre>

__Response__ : 

---
__Command__ : `getSprayerRule`

__Data__ : None

__Response__ : 
<pre><code>{"delay":15,"actions":[{"device":"fan_out","on_period":900},{"device":"fan_in","on_period":900}]}</code></pre>

---
__Command__ : `setSprayerRule`

__Data__ : 
<pre><code>{"delay":15,"actions":[{"device":"fan_out","on_period":900},{"device":"fan_in","on_period":900}]}</code></pre>

__Response__ : None

---
__Command__ : `getTempTracefiles`

__Data__ : None

__Response__ : 
<pre><code>{"files":["temp_230308","temp_230309","temp_230310"]}</code></pre>

---
__Command__ :`getStateTracefiles`

__Data__ : None

__Response__ : 
<pre><code>{"files":["state_230308","state_230309","state_230310"]}</code></pre>

---
__Command__ : `getTemperatureFile`

__Data__ :
<pre><code>{"fname":"temp_230310"}</code></pre>
__Response__ :
<pre><code>{"content":"
2023-03-10 16:00:04 start
2023-03-10 16:01:04 r=21 t=26
2023-03-10 16:02:04 r=21 t=26
...
2023-03-11 16:00:00 stop"}</code></pre>

---
__Command__ : `getStatefile`

__Data__ :
<pre><code>{"fname":"state_230310"}</code></pre>
__Response__ : 
<pre><code>{"content":"
2023-03-10 05:00:00 start
2023-03-10 05:00:00 light1 0
...
2023-03-10 05:00:00 stop"}</code></pre>


#### using Wifi
The REST webservice implements a HTTPSocket listener on a IP address that the Pi OS gets from the DHCP service on the local network. The port number is 80 (see `RestServer.java`).

#### REST API
__URL__ : `GET /properties`

__Data__ : None

__Response__ :
<pre><code>{"nr_of_timers":23,"nr_of_programs":2,"devices":[
{"device":"light1", "nr_of_timers":1, "lc_counted":false},
....
{"device":"spare", "nr_of_timers":5, "lc_counted":false}
]}</code></pre>

---
__URL__ : `GET /sensors`

__Data__ : None

__Response__ :
<pre><code>{"clock": "08-03-2023 19:19", 
 "sensors": [ {"location":"room", "temperature":21, "humidity":45 },{"location":"terrarium", "temperature":26} ]
}</code></pre>

---
__URL__ : `POST /sensors/21/26`

__Data__ : None

__Response__ : None

---
__URL__ : `GET /state`

__Data__ : None

__Response__ :
<pre><code>{"trace":"off","state": [
{"name":"light1", "onPeriod":-1, "manual":false},
{"name":"uvlight", "onPeriod":-1, "lifetime": 4398, "manual":false},
...
{"name":"spare", "onPeriod":0, "manual":false}
]}</code></pre>

---
__URL__ : `POST /device/light1/on`

__Data__ : None

__Response__ : None

---
__URL__ : `POST /device/light1/off`

__Data__ : None

__Response__ : None

---
__URL__ : `POST /device/sprayer/on/10`

__Data__ : None

__Response__ : None

---
__URL__ : `POST /device/light1/manual`

__Data__ : None

__Response__ : None

---
__URL__ : `POST /device/light1/auto`

__Data__ : None

__Response__ : None

---
__URL__ : `POST /counter/light1/4400`

__Data__ : None

__Response__ : None

---
__URL__ : `GET /timers/light1`

__Data__ : None

__Response__ : 
<pre><code>{"timers":[
{"device":"light1","index":1,"hour_on":9,"hour_off":0,"minute_on":21,"minute_off":0,"period":0,"repeat":1}
]}</code></pre>

---
__URL__ : `POST /timers`

__Data__ : 
<pre><code>{"timers":[
{"device":"light1","index":1,"hour_on":9,"hour_off":0,"minute_on":1,"minute_off":0,"period":0,"repeat":1}
]}</code></pre>

__Response__ : None

---
__URL__ : `GET /ruleset/1`

__Data__ : None

__Response__ : 
<pre><code>{"active": "yes", "from": "10:30", "to": "21:00", "temp_ideal": 26,
    "rules": [
        {"value":-25, "actions":
	        [
	        	{"device": "fan_in", "on_period": -2},
	            {"device": "no device", "on_period": 0}
	        ],
        },
        {"value": 27, "actions":
	        [
	        	{"device": "fan_out", "on_period": -2},
	            {"device": "no device", "on_period": 0}
	        ],
        }
    ]
}</code></pre>

---
__URL__ : `POST /ruleset/1`

__Data__ : 
<pre><code>{"rulesetnr":1, "ruleset":
{"active": "yes", "from": "10:30", "to": "21:00", "temp_ideal": 26,
    "rules": [
        {"value":-25, "actions":
	        [
	        	{"device": "fan_in", "on_period": -2},
	            {"device": "no device", "on_period": 0}
	        ],
        },
        {"value": 27, "actions":
	        [
	        	{"device": "fan_out", "on_period": -2},
	            {"device": "no device", "on_period": 0}
	        ],
        }
    ]
}</code></pre>

__Response__ : 

---
__URL__ : `GET /sprayerrule`

__Data__ : None

__Response__ : 
<pre><code>{"delay":15,"actions":[{"device":"fan_out","on_period":900},{"device":"fan_in","on_period":900}]}</code></pre>

---
__URL__ : `POST /sprayerrule`

__Data__ : 
<pre><code>{"delay":15,"actions":[{"device":"fan_out","on_period":900},{"device":"fan_in","on_period":900}]}</code></pre>

__Response__ : None

---
__URL__ : `GET /history/temperature`

__Data__ : None

__Response__ : 
<pre><code>{"files":["temp_20230308","temp_20230309","temp_20230310"]}</code></pre>

---
__URL__ : `GET /history/state`

__Data__ : None

__Response__ : 
<pre><code>{"files":["state_20230308","state_20230309","state_20230310"]}</code></pre>

---
__URL__ : `GET /history/temperature/temp_20230310`

__Data__ : None

__Response__ :
<pre><code>{"content":"
2023-03-10 16:00:04 start
2023-03-10 16:01:04 r=21 t=26
2023-03-10 16:02:04 r=21 t=26
...
2023-03-11 16:00:00 stop"}</code></pre>

---
__URL__ : `GET /history/state/state_20230310`

__Data__ : None

__Response__ : 
<pre><code>{"content":"
2023-03-10 05:00:00 start
2023-03-10 05:00:00 light1 0
...
2023-03-10 05:00:00 stop"}</code></pre>

