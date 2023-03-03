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

import nl.das.tcu.objects.Terrarium;

/**
 *
 */
public class EachHourTask extends TimerTask {

	@Override
	public void run () {
		Util.println("EachHourTask: task executed!");
		Terrarium terrarium = Terrarium.getInstance();
		terrarium.setNow(LocalDateTime.now());
		if (!terrarium.isTraceOn()) {
			// Start trace on the whole hour
			terrarium.setTrace(true);
		}
		// Each hour
		// - decrement lifecycle value
		terrarium.decreaseLifetime(1);
		terrarium.saveLifecycleCounters();
	}

}
