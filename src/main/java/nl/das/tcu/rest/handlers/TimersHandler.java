/*
 * Copyright © 2023 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 25 Jan 2023.
 */


package nl.das.tcu.rest.handlers;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import nl.das.tcu.Util;
import nl.das.tcu.objects.Terrarium;
import nl.das.tcu.objects.TimerArray;

/**
 *
 */
public class TimersHandler implements HttpHandler {

	@Override
	public void handleRequest (HttpServerExchange exchange) throws Exception {
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		if (exchange.getRequestMethod().toString().equalsIgnoreCase("GET")) {
        	try {
        		String path = exchange.getRelativePath();
        		if (path.startsWith("/timers/")) {
        			String device = HandlerUtils.getParm(exchange.getQueryParameters(), "device");
    				JsonReader jsonReader = Json.createReader(new StringReader("{\"timers\":" + jsonb.toJson(Terrarium.getInstance().getTimersForDevice(device)) + "}"));
    				JsonObject object = jsonReader.readObject();
    				exchange.getResponseSender().send(Util.parser().toJson(object));
        		}
			} catch (Exception e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		} else if (exchange.getRequestMethod().toString().equalsIgnoreCase("POST")) {
        	try {
        		String json = new String(exchange.getInputStream().readAllBytes());
        		Terrarium.getInstance().replaceTimers(Util.parser().fromJson(json, TimerArray.class).getTimers());
				exchange.getResponseSender().send("{}");
			} catch (Exception e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		}
	}

}
