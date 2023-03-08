/*
 * Copyright © 2023 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 24 Feb 2023.
 */


package nl.das.tcu;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Timer;

import javax.bluetooth.UUID;

import com.pi4j.system.NetworkInfo;

import nl.das.tcu.hw.LCD;
import nl.das.tcu.objects.Terrarium;
import nl.das.tcu.rest.BTServer;
import nl.das.tcu.rest.RestServer;

/**
 *
 */
public class TCU {

	/**
	 * @param args
	 */
	public static void main (String[] args) {
		Util.println("Main started");
		// Read property file
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("config.properties"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Start httpserver in his own, low prio thread
		Util.println("Starting the REST service");
		Thread restsvr = new Thread() {
		    @Override
			public void run(){
		        RestServer svr = RestServer.getInstance("0.0.0.0", 80);
		        svr.start();
		    }
		};
		restsvr.setName("RestServer");
		restsvr.setPriority(Thread.MIN_PRIORITY);
		restsvr.start();

		// Start Bluetooth server in his own, low prio thread
		Util.println("Starting the Bluetooth server");
		Thread btsvr = new Thread() {
		    @Override
			public void run(){
		        try {
					new BTServer(props.getProperty("host"), new UUID(props.getProperty("uuid"), false)).start();
				} catch (IOException e) {
					Util.println(e.getMessage());
					e.printStackTrace();
				};
		      }
		};
		btsvr.setName("BTServer");
		btsvr.setPriority(Thread.MIN_PRIORITY);
		btsvr.start();

		// Initialize the LCD
		LCD lcd = LCD.getInstance(2, 16);
		lcd.write(0, "Initialize....");

		// Retrieve the Terrarium settings from disk
		Terrarium terrarium = null;
		try {
			String json = new String(Files.readAllBytes(Paths.get("settings.json")));
			terrarium = Terrarium.getInstance(json);
		} catch (NoSuchFileException e) {
			Util.println("No settings.json file found. All timers and rules are empty.");
			terrarium = Terrarium.getInstance();
			terrarium.init();
		} catch (IOException e) {
			e.printStackTrace();
		}
		terrarium.setNow(LocalDateTime.now());
		// Initialize the devices
		terrarium.initDevices();
		// Initialize device state
		terrarium.initDeviceState();
		// Initialize the Temperature sensors
		terrarium.initSensors();
		int tterr = terrarium.getTerrariumTemperature();
		int troom =  terrarium.getRoomTemperature();
		lcd.displayLine1(troom, tterr);
		String ip="";
		try {
	        for (String ipAddress : NetworkInfo.getIPAddresses()) {
	        	if (ipAddress.startsWith("192")) {
	        		ip = ipAddress;
	        	}
	        }
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		lcd.write(1, ip);
		// Check timers if devices should be on
		terrarium.initTimers(LocalDateTime.now());
		terrarium.initRules();

		// Start timer and schedule the seconds-task and the minute-task
		EachSecondTask t1 = new EachSecondTask();
		EachMinuteTask t2 = new EachMinuteTask();
		EachHourTask t3 = new EachHourTask();
		Timer t = new Timer("Timer");
		t.schedule(t1, 0L, 1000L); //  executes every second
		LocalDateTime now = LocalDateTime.now();
		t.schedule(t2, (60L - now.getSecond()) * 1000L, 60000L); // executes every minute
		t.schedule(t3, (3600L - ((now.getMinute() * 60L) + now.getSecond())) * 1000L, 3600000L); // executes every hour

		Util.println("Main ended");
	}
}
