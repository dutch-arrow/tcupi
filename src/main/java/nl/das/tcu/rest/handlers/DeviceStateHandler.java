/*
 * Copyright © 2023 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 24 Jan 2023.
 */


package nl.das.tcu.rest.handlers;

import java.time.LocalDateTime;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import nl.das.tcu.Util;
import nl.das.tcu.objects.Terrarium;

/**
 *
 */
public class DeviceStateHandler implements HttpHandler {

	@Override
	public void handleRequest (HttpServerExchange exchange) throws Exception {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		if (exchange.getRequestMethod().toString().equalsIgnoreCase("GET")) {
        	try {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
				exchange.getResponseSender().send(Terrarium.getInstance().getState());
			} catch (Exception e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		} else if (exchange.getRequestMethod().toString().equalsIgnoreCase("POST")) {
        	try {
        		String path = exchange.getRelativePath();
        		if (path.startsWith("/device")) {
            		String device = HandlerUtils.getParm(exchange.getQueryParameters(), "device");
            		if (path.endsWith("/on")) {
            			Terrarium.getInstance().setDeviceOn(device, -1);
            		} else if (path.endsWith("/off")) {
            			Terrarium.getInstance().setDeviceOff(device);
           		} else if (path.endsWith("manual")) {
           			Terrarium.getInstance().setDeviceManualOn(device);
            		} else if (path.endsWith("auto")) {
            			Terrarium.getInstance().setDeviceManualOff(device);
            		} else {
            			long period = Long.parseLong(HandlerUtils.getParm(exchange.getQueryParameters(), "period"));
            			Terrarium.getInstance().setDeviceOn(device, Util.now(LocalDateTime.now()) + period);
            		}
        		} else if (path.startsWith("/counter")) {
        			String device = HandlerUtils.getParm(exchange.getQueryParameters(), "device");
        			int value = Integer.parseInt(HandlerUtils.getParm(exchange.getQueryParameters(), "hoursOn"));
        			Terrarium.getInstance().setLifecycleCounter(device, value);
        		}
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send("{}");
			} catch (Exception e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		}
	}
}
