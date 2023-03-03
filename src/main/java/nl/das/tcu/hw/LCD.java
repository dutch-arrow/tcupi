/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 16 Aug 2021.
 */


package nl.das.tcu.hw;

import java.io.IOException;

import com.pi4j.component.lcd.impl.I2CLcdDisplay;
import com.pi4j.io.i2c.I2CBus;

/**
 *
 */
public class LCD {

	private I2CLcdDisplay lcd;
	private boolean notConnected;
	private static LCD instance;

	private LCD(int rows, int cols) {
		try {
			this.lcd = new I2CLcdDisplay(rows, cols, I2CBus.BUS_1, 0x27, 3, 0, 1, 2, 7, 6, 5, 4);
			this.notConnected = false;
			this.lcd.setBacklight(true, true);
			this.lcd.clear();
		} catch (IOException e) {
			this.notConnected = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static LCD getInstance() {
		return instance;
	}

	public static LCD getInstance(int rows, int cols) {
		if (instance == null) {
			instance = new LCD(rows,cols);
		}
		return instance;
	}

	public void write(int row, String text) {
		if (!this.notConnected) {
			this.lcd.write(row, text);
		}
	}

	public void clear() {
		if (!this.notConnected) {
			this.lcd.clear();
		}
	}

	public void clear(int row) {
		if (!this.notConnected) {
			this.lcd.clear(row);
		}
	}

	public void writeDegrees(int row, int col) {
		if (!this.notConnected) {
			this.lcd.write(row, col, (byte) 0xDF);
		}
	}

	public void displayLine1 (int troom, int tterr) {
		if (!this.notConnected) {
			this.lcd.clear(0);
			char degrees = 0xDF;
			this.lcd.write(0, String.format("K:%2d%cC T:%2d%cC", troom, degrees, tterr, degrees));
		}
	}
}
