/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 16 Aug 2021.
 */

package nl.das.tcu.hw;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.pi4j.io.gpio.Pin;
import com.pi4j.wiringpi.Gpio;

/**
 * Implements the DHT22 / AM2302 reading in Java using Pi4J.
 *
 * See sensor specification sheet for details.
 */
public class DHT22 {

	/**
	 * Name of the sensor.
	 */
	private String name = "MyDHT22";
	/**
	 * Time in nanoseconds to separate ZERO and ONE signals.
	 */
	private static final int LONGEST_ZERO = 50000;
	/**
	 * Minimum time in milliseconds to wait between reads of sensor.
	 */
	public static final int MIN_MILLISECS_BETWEEN_READS = 2500;
	/**
	 * PI4J Pin number.
	 */
	private int pinNumber;
	/**
	 * 40 bit Data from sensor
	 */
	private byte[] data = null;
	/**
	 * Value of last successful humidity reading.
	 */
	private Double humidity = 0.0;
	/**
	 * Value of last successful temperature reading.
	 */
	private Double temperature = 0.0;
	/**
	 * Last read attempt
	 */
	private Long lastRead = null;
	/**
	 * Constructor with pin used for signal. See PI4J and WiringPI for pin numbering systems.....
	 *
	 * @param pin
	 */
	public DHT22(Pin pin) {
		this.pinNumber = pin.getAddress();
	}

	public DHT22(Pin pin, String name) {
		this(pin);
		this.name = name;
	}

	/**
	 * Communicate with sensor to get new reading data.
	 *
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws Exception            if failed to successfully read data.
	 */
	private void getData () throws IOException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		ReadSensorFuture readSensor = new ReadSensorFuture();
		Future<byte[]> future = executor.submit(readSensor);
		// Reset data
		this.data = new byte[5];
		try {
			this.data = future.get(3, TimeUnit.SECONDS);
			readSensor.close();
		} catch (Exception e) {
			readSensor.close();
			future.cancel(true);
			executor.shutdown();
			throw new IOException(e);
		}
		readSensor.close();
		executor.shutdown();
	}

	public boolean doReadLoop () throws InterruptedException, IOException {
		Hashtable<IOException, Integer> exceptions = new Hashtable<IOException, Integer>();
		for (int i = 0; i < 10; i++) {
			try {
				if (read(false)) {
					return true;
				}
			} catch (IOException e) {
				if (Objects.isNull(exceptions.get(e))) {
					exceptions.put(e, 1);
				} else {
					exceptions.put(e, exceptions.get(e).intValue() + 1);
				}
			}
			Thread.sleep(DHT22.MIN_MILLISECS_BETWEEN_READS);
		}
		// return the most common exception.
		IOException returnException = null;
		int exceptionCount = 0;
		for (IOException e : exceptions.keySet()) {
			if (exceptions.get(e).intValue() > exceptionCount) {
				returnException = e;
			}
		}
		throw returnException;
	}

	/**
	 * Make one new sensor reading.
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean read () throws Exception {
		return read(true);
	}

	/**
	 * Make a new sensor reading
	 *
	 * @param checkParity Should a parity check be performed?
	 * @return
	 * @throws ValueOutOfOperatingRangeException
	 * @throws ParityCheckException
	 * @throws IOException
	 */
	public boolean read (boolean checkParity) throws ValueOutOfOperatingRangeException, ParityCheckException, IOException {
		checkLastReadDelay();
		this.lastRead = System.currentTimeMillis();
		getData();
		if (checkParity) {
			checkParity();
		}

		// Operating Ranges from specification sheet.
		// humidity 0-100
		// temperature -40~80
		double newHumidityValue = getReadingValueFromBytes(this.data[0], this.data[1]);
		if ((newHumidityValue < 0) || (newHumidityValue > 100)) {
			throw new ValueOutOfOperatingRangeException();
		}
		this.humidity = newHumidityValue;
		double newTemperatureValue = getReadingValueFromBytes(this.data[2], this.data[3]);
		if ((newTemperatureValue < -40) || (newTemperatureValue >= 85)) {
			throw new ValueOutOfOperatingRangeException();
		}
		this.temperature = newTemperatureValue;
		this.lastRead = System.currentTimeMillis();
		return true;
	}

	private void checkLastReadDelay () throws IOException {
		if (Objects.nonNull(this.lastRead)) {
			if (this.lastRead > (System.currentTimeMillis() - 2000)) {
				throw new IOException("Last read was under 2 seconds ago. Please wait longer between reads!");
			}
		}
	}

	protected static double getReadingValueFromBytes (final byte hi, final byte low) {
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.put(hi);
		bb.put(low);
		short shortVal = bb.getShort(0);
		double doubleValue = ((double) shortVal) / 10;

		// When highest bit of temperature is 1, it means the temperature is below 0 degree Celsius.
		if (1 == ((hi >> 7) & 1)) {
			doubleValue = (doubleValue + 3276.8) * -1d;
		}

		return doubleValue;
	}

	private void checkParity () throws ParityCheckException {
		if (!(this.data[4] == (this.data[0] + this.data[1] + this.data[2] + this.data[3]))) {
			throw new ParityCheckException();
		}
	}

	public Double getHumidity () {
		return this.humidity;
	}

	public Double getTemperature () {
		return this.temperature;
	}

	/**
	 * Callable Future for reading sensor. Allows timeout if it gets stuck.
	 */
	private class ReadSensorFuture implements Callable<byte[]>, Closeable {

		private boolean keepRunning = true;

		public ReadSensorFuture() {
			Gpio.pinMode(DHT22.this.pinNumber, Gpio.OUTPUT);
			Gpio.digitalWrite(DHT22.this.pinNumber, Gpio.HIGH);
		}

		@Override
		public byte[] call () throws Exception {
			// do expensive (slow) stuff before we start and prioritize thread.
			byte[] data = new byte[5];
			long startTime = System.nanoTime();
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			sendStartSignal();
			waitForResponseSignal();
			for (int i = 0; i < 40; i++) {
				while (this.keepRunning && (Gpio.digitalRead(DHT22.this.pinNumber) == Gpio.LOW)) {
				}
				startTime = System.nanoTime();
				while (this.keepRunning && (Gpio.digitalRead(DHT22.this.pinNumber) == Gpio.HIGH)) {
				}
				long timeHight = System.nanoTime() - startTime;
				data[i / 8] <<= 1;
				if (timeHight > LONGEST_ZERO) {
					data[i / 8] |= 1;
				}
			}
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
			return data;
		}

		private void sendStartSignal () {
			// Send start signal.
			Gpio.pinMode(DHT22.this.pinNumber, Gpio.OUTPUT);
			Gpio.digitalWrite(DHT22.this.pinNumber, Gpio.LOW);
			Gpio.delay(10);
			Gpio.digitalWrite(DHT22.this.pinNumber, Gpio.HIGH);
		}

		/**
		 * AM2302 will pull low 80us as response signal, then AM2302 pulls up 80us for preparation to send data.
		 */
		private void waitForResponseSignal () {
			Gpio.pinMode(DHT22.this.pinNumber, Gpio.INPUT);
			while (this.keepRunning && (Gpio.digitalRead(DHT22.this.pinNumber) == Gpio.HIGH)) {
			}
			while (this.keepRunning && (Gpio.digitalRead(DHT22.this.pinNumber) == Gpio.LOW)) {
			}
			while (this.keepRunning && (Gpio.digitalRead(DHT22.this.pinNumber) == Gpio.HIGH)) {
			}
		}

		@Override
		public void close () throws IOException {
			this.keepRunning = false;

			// Set pin high for end of transmission.
			Gpio.pinMode(DHT22.this.pinNumber, Gpio.OUTPUT);
			Gpio.digitalWrite(DHT22.this.pinNumber, Gpio.HIGH);
		}
	}

	public class ParityCheckException extends IOException {
		private static final long serialVersionUID = 1L;
	}

	public class ValueOutOfOperatingRangeException extends IOException {
		private static final long serialVersionUID = 1L;
	}

	public String getName () {
		return this.name;
	}
}