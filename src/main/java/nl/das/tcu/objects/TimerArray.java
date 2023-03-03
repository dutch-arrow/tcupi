/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 31 Jul 2022.
 */


package nl.das.tcu.objects;

/**
 *
 */
public class TimerArray {

	private Timer[] timers;

	public TimerArray() { }

	public TimerArray(Timer[] timers) {
		this.timers = timers;
	}

	public Timer[] getTimers () {
		return this.timers;
	}

	public void setTimers (Timer[] timers) {
		this.timers = timers;
	}

}
