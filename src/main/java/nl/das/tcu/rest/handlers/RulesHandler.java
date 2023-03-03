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
import nl.das.tcu.objects.Ruleset;
import nl.das.tcu.objects.SprayerRule;
import nl.das.tcu.objects.Terrarium;

/**
 *
 */
public class RulesHandler implements HttpHandler {

	@Override
	public void handleRequest (HttpServerExchange exchange) throws Exception {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		if (exchange.getRequestMethod().toString().equalsIgnoreCase("GET")) {
        	try {
        		String path = exchange.getRelativePath();
        		if (path.startsWith("/ruleset/")) {
        			int nr = Integer.parseInt(HandlerUtils.getParm(exchange.getQueryParameters(), "nr"));
    				exchange.getResponseSender().send(Util.parser().toJson(Terrarium.getInstance().getRuleset(nr)));
        		} else if (path.equalsIgnoreCase("/sprayerrule")) {
    				exchange.getResponseSender().send(Util.parser().toJson(Terrarium.getInstance().getSprayerRule()));
        		}
			} catch (Exception e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		} else if (exchange.getRequestMethod().toString().equalsIgnoreCase("POST")) {
        	try {
        		String path = exchange.getRelativePath();
        		if (path.startsWith("/ruleset/")) {
            		String json = new String(exchange.getInputStream().readAllBytes());
        			int nr = Integer.parseInt(HandlerUtils.getParm(exchange.getQueryParameters(), "nr"));
        			Terrarium.getInstance().replaceRuleset(nr, Util.parser().fromJson(json, Ruleset.class));
        			Terrarium.getInstance().saveSettings();
        		} else if (path.equalsIgnoreCase("/sprayerrule")) {
            		String json = new String(exchange.getInputStream().readAllBytes());
	        		Terrarium.getInstance().setSprayerRule(Util.parser().fromJson(json, SprayerRule.class));
	        		Terrarium.getInstance().saveSettings();
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
