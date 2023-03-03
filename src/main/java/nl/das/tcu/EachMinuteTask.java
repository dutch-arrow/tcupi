/*
 * Copyright © 2023 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 24 Feb 2023.
 */


package nl.das.tcu;

import java.time.LocalDateTime;
import java.util.TimerTask;

import nl.das.tcu.hw.LCD;
import nl.das.tcu.objects.Terrarium;

/**
 *
 */
public class EachMinuteTask extends TimerTask {

	@Override
	public void run () {
//		Util.println("EachMinuteTask: task executed!");
		Terrarium terrarium = Terrarium.getInstance();
		terrarium.setNow(LocalDateTime.now());
		// - display temperature on LCD line 1
		terrarium.readSensorValues();
		int tterr = terrarium.getTerrariumTemperature();
		int troom = terrarium.getRoomTemperature();
		LCD.getInstance().displayLine1(troom, tterr);
		Util.traceTemperature(Terrarium.traceFolder + "/" +  Terrarium.traceTempFilename, LocalDateTime.now(), "r=%d t=%d", troom, tterr);
		// - check timers
		terrarium.checkTimers();
		// - check sprayerrule
		terrarium.checkSprayerRule();
		// - check rulesets
		terrarium.checkRules();
		// Check if tracing should be switched off (max 1 day)
		terrarium.checkTrace();
	}

}
