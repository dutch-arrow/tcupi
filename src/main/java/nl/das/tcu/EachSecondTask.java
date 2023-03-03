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
public class EachSecondTask extends TimerTask {

	@Override
	public void run () {
//		Util.println("] EachSecondTask: task executed!");
		Terrarium.getInstance().setNow(LocalDateTime.now());
		Terrarium.getInstance().checkDevices();
	}

}
