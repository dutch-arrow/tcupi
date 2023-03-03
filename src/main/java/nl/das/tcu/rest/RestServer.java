/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 12 Oct 2022.
 */


package nl.das.tcu.rest;

import org.slf4j.LoggerFactory;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.util.Headers;
import nl.das.tcu.Util;
import nl.das.tcu.rest.handlers.DeviceStateHandler;
import nl.das.tcu.rest.handlers.HistoryHandler;
import nl.das.tcu.rest.handlers.PropertiesHandler;
import nl.das.tcu.rest.handlers.RulesHandler;
import nl.das.tcu.rest.handlers.SensorsHandler;
import nl.das.tcu.rest.handlers.TimersHandler;

/**
 * The REST server is build on the Undertow Embedded HTTP server (see: https://undertow.io/)
 */
public class RestServer {

	private Undertow server;
	private static RestServer instance;

	private RestServer(String host, int port) {
		this.server = Undertow.builder()
			.addHttpListener(port, host)
			.setHandler(setRoot()).build();
	}

	public static RestServer getInstance(String host, int port) {
 		if (instance == null) {
			instance = new RestServer(host, port);
		} else {
			System.out.println("Instance is not NULL");
		}
		return instance;
	}

	public void start() {
		Util.println("RestServer started");
		this.server.start();
	}

	private static HttpHandler setRoutes() {
		HttpHandler routes = Handlers.path()
	        // REST API path
	        .addPrefixPath("/", Handlers.routing()
	        		.get("properties", new BlockingHandler(new PropertiesHandler())) //device/{device}/on
	        		.get("state", new BlockingHandler(new DeviceStateHandler()))
	        		.post("device/{device}/on", new BlockingHandler(new DeviceStateHandler()))
	        		.post("device/{device}/off", new BlockingHandler(new DeviceStateHandler()))
	        		.post("device/{device}/on/{period}", new BlockingHandler(new DeviceStateHandler()))
	        		.post("device/{device}/manual", new BlockingHandler(new DeviceStateHandler()))
	        		.post("device/{device}/auto", new BlockingHandler(new DeviceStateHandler()))
	        		.post("counter/{device}/{hoursOn}", new BlockingHandler(new DeviceStateHandler()))
	        		.get("sensors", new BlockingHandler(new SensorsHandler()))
	        		.post("sensors/{room}/{terr}", new BlockingHandler(new SensorsHandler()))
	        		.post("sensors/auto", new BlockingHandler(new SensorsHandler()))
	        		.get("timers/{device}", new BlockingHandler(new TimersHandler()))
	        		.post("timers", new BlockingHandler(new TimersHandler()))
	        		.get("ruleset/{nr}", new BlockingHandler(new RulesHandler()))
	        		.post("ruleset/{nr}", new BlockingHandler(new RulesHandler()))
	        		.get("sprayerrule", new BlockingHandler(new RulesHandler()))
	        		.post("sprayerrule", new BlockingHandler(new RulesHandler()))
	        		.get("history/temperature", new BlockingHandler(new HistoryHandler()))
	        		.get("history/state", new BlockingHandler(new HistoryHandler()))
	        		.get("history/temperature/{fname}", new BlockingHandler(new HistoryHandler()))
	        		.get("history/state/{fname}", new BlockingHandler(new HistoryHandler()))
	        		.setFallbackHandler(RestServer::notFoundHandler))
	        ;
		 return routes;
	}

	private static  HttpHandler setRoot() {
		return new AccessLogHandler(setRoutes(),
			new Slf4jAccessLogReceiver(LoggerFactory.getLogger("nl.das.accesslog")), "common",
			RestServer.class.getClassLoader());
	}

	public static void notFoundHandler (HttpServerExchange exchange) {
		exchange.setStatusCode(404);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
		exchange.getRequestPath();
		exchange.getResponseSender().send("Page '" + exchange.getRequestPath() + "' Not Found!!");
	}

}
