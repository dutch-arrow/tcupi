/*
 * Copyright © 2023 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 25 Jan 2023.
 */


package nl.das.tcu.rest.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import nl.das.tcu.Util;
import nl.das.tcu.objects.Terrarium;

/**
 *
 */
public class SensorsHandler implements HttpHandler {

	@Override
	public void handleRequest (HttpServerExchange exchange) throws Exception {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		if (exchange.getRequestMethod().toString().equalsIgnoreCase("GET")) {
        	try {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
				exchange.getResponseSender().send(Util.parser().toJson(Terrarium.getInstance().getSensors()));
			} catch (Exception e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		} else if (exchange.getRequestMethod().toString().equalsIgnoreCase("POST")) {
        	try {
        		String path = exchange.getRelativePath();
        		if (path.equalsIgnoreCase("/sensors/auto")) {
        			Terrarium.getInstance().setTestOff();
        		} else if (path.startsWith("/sensors/")) {
        			int room = Integer.parseInt(HandlerUtils.getParm(exchange.getQueryParameters(), "room"));
        			int terr = Integer.parseInt(HandlerUtils.getParm(exchange.getQueryParameters(), "terr"));
        			Terrarium.getInstance().setSensors(room, terr);
        		}
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
				exchange.getResponseSender().send("{}");
			} catch (Exception e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		}
	}

}
