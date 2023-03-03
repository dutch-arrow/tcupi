/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 21 Jul 2022.
 */


package nl.das.tcu.rest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import nl.das.tcu.Util;
import nl.das.tcu.objects.Ruleset;
import nl.das.tcu.objects.SprayerRule;
import nl.das.tcu.objects.Terrarium;
import nl.das.tcu.objects.Timer;

/**
 *
 */
public class BTServer {

	private StreamConnectionNotifier scn;
	private StreamConnection sc;

	public BTServer(String name, UUID uuid) throws IOException {
		LocalDevice.getLocalDevice().setDiscoverable(DiscoveryAgent.GIAC);
		String url = "btspp://localhost:" + uuid.toString() + ";name=" + name + ";encrypt=false;authenticate=false";
		Util.println("Connecting to '" + url + "'....");
		// Create a server connection (a notifier)
		this.scn = (StreamConnectionNotifier) Connector.open(url);
	}

	public void start() throws IOException {
		Util.println("BTServer started");
		while(true) {
			LocalDevice.getLocalDevice().setDiscoverable(DiscoveryAgent.GIAC);
			// Accept a new client connection
			this.sc = this.scn.acceptAndOpen();
			// New client connection accepted; get a handle on it
			RemoteDevice rd = RemoteDevice.getRemoteDevice(this.sc);
			Util.println("New Bluetooth client connection... " + rd.getFriendlyName(false));
			// Read input message, in this example a String
			DataInputStream dataIn = this.sc.openDataInputStream();
			DataOutputStream dataOut = this.sc.openDataOutputStream();
			int chr;
			StringBuffer sb = new StringBuffer();
			while ((chr = dataIn.read()) != -1) {
				if (chr == 0x03) {
					handleCommand(sb.toString(), dataOut);
					sb = new StringBuffer();
				} else {
					sb.append((char)chr);
				}
			}
			Util.println("Bluetooth connection closed");
		}
	}

	/**
	 * @param string
	 * @throws IOException
	 */
	public static void handleCommand (String command, OutputStream out) throws IOException {
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
		// Analyze command
		Command cmd = jsonb.fromJson(command, Command.class);
		Response res = new Response(cmd.getMsgId(), cmd.getCmd());
		try {
			switch(cmd.getCmd()) {
			case "getSensors": {
				JsonReader jsonReader = Json.createReader(new StringReader(jsonb.toJson(Terrarium.getInstance().getSensors())));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			case "setSensors": {
				if (cmd.getData() == null) {
					throw new CommandException("No data received.");
				}
				int rt = cmd.getData().getInt("roomtemp", 0);
				if( rt == 0) {
					throw new CommandException("Integer parameter 'roomtemp' not found.");
				}
				int tt = cmd.getData().getInt("terrtemp", 0);
				if (tt == 0) {
					throw new CommandException("Integer parameter 'terrtemp' not found.");
				}
				Terrarium.getInstance().setSensors(rt, tt);
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "setTestOff": {
				Terrarium.getInstance().setTestOff();
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "getState": {
				JsonReader jsonReader = Json.createReader(new StringReader(Terrarium.getInstance().getState()));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			case "setDeviceOn": {
				if (cmd.getData() == null) {
					throw new CommandException("No data found.");
				}
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				Terrarium.getInstance().setDeviceOn(prm, -1);
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "setDeviceOff": {
				if (cmd.getData() == null) {
					throw new CommandException("No data found.");
				}
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				Terrarium.getInstance().setDeviceOff(prm);
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "setDeviceOnFor": {
				if (cmd.getData() == null) {
					throw new CommandException("No data found.");
				}
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				int per = cmd.getData().getInt("period", -1);
				if (per == -1) {
					throw new CommandException("Integer parameter 'period' not found.");
				}
				if ((per <= 0) || (per > 3600)) {
					throw new CommandException("Integer parameter 'period' must be > 0 and < 3600 seconds.");
				}
				Terrarium.getInstance().setDeviceOn(prm, Util.now(LocalDateTime.now()) + per);
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "setDeviceManualOn": {
				if (cmd.getData() == null) {
					throw new CommandException("No data found.");
				}
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				Terrarium.getInstance().setDeviceManualOn(prm);
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "setDeviceManualOff": {
				if (cmd.getData() == null) {
					throw new CommandException("No data found.");
				}
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				Terrarium.getInstance().setDeviceManualOff(prm);
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "setLifecycleCounter": {
				if (cmd.getData() == null) {
					throw new CommandException("No data found.");
				}
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				int hrs = cmd.getData().getInt("hoursOn", -1);
				if (hrs == -1) {
					throw new CommandException("Integer parameter 'hoursOn' not found.");
				}
				if (hrs <= 0 ) {
					throw new CommandException("Integer parameter 'hoursOn' must be > 0 hours.");
				}
				Terrarium.getInstance().setLifecycleCounter(prm, hrs);
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "getProperties": {
				JsonReader jsonReader = Json.createReader(new StringReader(Terrarium.getInstance().getProperties()));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			case "setTraceOn": {
				Terrarium.getInstance().setNow(LocalDateTime.now());
				Terrarium.getInstance().setTrace(true);
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "setTraceOff": {
				Terrarium.getInstance().setTrace(false);
				res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				break;
			}
			case "getTimersForDevice": {
				if (cmd.getData() == null) {
					throw new CommandException("No data found.");
				}
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				JsonReader jsonReader = Json.createReader(new StringReader("{\"timers\":" + jsonb.toJson(Terrarium.getInstance().getTimersForDevice(prm)) + "}"));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			case "replaceTimers": {
				if (cmd.getData() == null) {
					throw new CommandException("No data found.");
				}
				JsonArray ja = Json.createReader(new StringReader(cmd.getData().get("timers").toString())).readArray();
				if (ja == null) {
					throw new CommandException("JsonArray parameter 'timers' does not contain an array of Timer objects.");
				}
				try {
					Timer[] timers = jsonb.fromJson(ja.toString(), Timer[].class);
					Terrarium.getInstance().replaceTimers(timers);
					Terrarium.getInstance().saveSettings();
					res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				} catch (JsonbException e) {
					throw new CommandException("JsonArray parameter 'timers' does not contain an array of Timer json objects.");
				}
				break;
			}
			case "getRuleset": {
				int prm = cmd.getData().getInt("rulesetnr", 0);
				if( prm == 0) {
					throw new CommandException("Integer parameter 'rulesetnr' not found.");
				}
				JsonReader jsonReader = Json.createReader(new StringReader(jsonb.toJson(Terrarium.getInstance().getRuleset(prm))));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			case "saveRuleset": {
				JsonObject obj = cmd.getData().get("ruleset").asJsonObject();
				if (obj == null) {
					throw new CommandException("JsonObject parameter 'ruleset' not found.");
				}
				int prm = cmd.getData().getInt("rulesetnr", 0);
				if( prm == 0) {
					throw new CommandException("Integer parameter 'rulesetnr' not found.");
				}
				Ruleset ruleset;
				try {
					ruleset = jsonb.fromJson(obj.toString(), Ruleset.class);
					Terrarium.getInstance().replaceRuleset(prm, ruleset);
					Terrarium.getInstance().saveSettings();
					res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				} catch (JsonbException e) {
					throw new CommandException("JsonObject parameter 'ruleset' does not contain a Ruleset json object.");
				}
				break;
			}
			case "getSprayerRule": {
				JsonReader jsonReader = Json.createReader(new StringReader(jsonb.toJson(Terrarium.getInstance().getSprayerRule())));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			case "setSprayerRule": {
				try {
					SprayerRule sprayerRule = jsonb.fromJson(cmd.getData().toString(), SprayerRule.class);
					Terrarium.getInstance().setSprayerRule(sprayerRule);;
					Terrarium.getInstance().saveSettings();
					res.setResponse(JsonObject.EMPTY_JSON_OBJECT);
				} catch (JsonbException e) {
					throw new CommandException("Data does not contain a SprayerRule json object.");
				}
				break;
			}
			case "getTempTracefiles": {
				List<String> files = new ArrayList<>();
				files = Util.listTraceFiles(Terrarium.traceFolder, "temp_");
				JsonReader jsonReader = Json.createReader(new StringReader("{\"files\":" + jsonb.toJson(files) + "}"));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			case "getStateTracefiles": {
				List<String> files = new ArrayList<>();
				files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
				JsonReader jsonReader = Json.createReader(new StringReader("{\"files\":" + jsonb.toJson(files) + "}"));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			case "getTemperatureFile": {
				String content = "";
				String prm = cmd.getData().getString("fname", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'fname' not found.");
				}
				content = Files.readString(Paths.get(Terrarium.traceFolder + "/" + cmd.getData().getString("fname")));
				JsonReader jsonReader = Json.createReader(new StringReader("{\"content\":\"" + content.replace("\n", "\\n") + "\"}"));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			case "getStateFile": {
				String content = "";
				String prm = cmd.getData().getString("fname", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'fname' not found.");
				}
				content = Files.readString(Paths.get(Terrarium.traceFolder + "/" + cmd.getData().getString("fname")));
				JsonReader jsonReader = Json.createReader(new StringReader("{\"content\":\"" + content.replace("\n", "\\n") + "\"}"));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
				break;
			}
			default:
				throw new CommandException("Command '" + cmd.getCmd() + "' is not implemented.");
			}
			// Construct response
			out.write(jsonb.toJson(res).getBytes());
			out.write(0x03); // ETX character
		} catch (Exception e) {
			if (e instanceof CommandException) {
				System.err.println(e.getMessage());
				JsonReader jsonReader = Json.createReader(new StringReader("{\"error\":\"" + e.getMessage() + "\"}"));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
			} else {
				e.printStackTrace();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				JsonReader jsonReader = Json.createReader(new StringReader("{\"error\":\"" + sw.toString().replace("\n", "\\n").replace("\t", "    ") + "\"}"));
				JsonObject object = jsonReader.readObject();
				res.setResponse(object);
			}
			out.write(jsonb.toJson(res).getBytes());
			out.write(0x03); // ETX character
		}
	}
}

