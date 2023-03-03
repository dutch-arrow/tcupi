/*
 * Copyright © 2023 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 25 Jan 2023.
 */


package nl.das.tcu.rest.handlers;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

/**
 *
 */
public class HistoryHandler implements HttpHandler {

	@Override
	public void handleRequest (HttpServerExchange exchange) throws Exception {
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
		if (exchange.getRequestMethod().toString().equalsIgnoreCase("GET")) {
        	try {
        		String path = exchange.getRelativePath();
    			List<String> files = new ArrayList<>();
        		if (path.equalsIgnoreCase("/history/temperature")) {
       				files = Util.listTraceFiles(Terrarium.traceFolder, "temp_");
    				JsonReader jsonReader = Json.createReader(new StringReader("{\"files\":" + jsonb.toJson(files) + "}"));
    				JsonObject object = jsonReader.readObject();
       				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    				exchange.getResponseSender().send(Util.parser().toJson(object));
        		} else if (path.equalsIgnoreCase("/history/state")) {
       				files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
    				JsonReader jsonReader = Json.createReader(new StringReader("{\"files\":" + jsonb.toJson(files) + "}"));
    				JsonObject object = jsonReader.readObject();
       				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    				exchange.getResponseSender().send(Util.parser().toJson(object));
        		} else if (path.startsWith("/history/temperature/")) {
        			String fname = HandlerUtils.getParm(exchange.getQueryParameters(), "fname");
        			String content = Files.readString(Paths.get(Terrarium.traceFolder + "/" + fname));
    				JsonReader jsonReader = Json.createReader(new StringReader("{\"content\":\"" + content.replace("\n", "\\n") + "\"}"));
    				JsonObject object = jsonReader.readObject();
        			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
    				exchange.getResponseSender().send(Util.parser().toJson(object));
        		} else if (path.startsWith("/history/state/")) {
        			String fname = HandlerUtils.getParm(exchange.getQueryParameters(), "fname");
        			String content = Files.readString(Paths.get(Terrarium.traceFolder + "/" + fname));
    				JsonReader jsonReader = Json.createReader(new StringReader("{\"content\":\"" + content.replace("\n", "\\n") + "\"}"));
    				JsonObject object = jsonReader.readObject();
        			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
    				exchange.getResponseSender().send(Util.parser().toJson(object));
        		}
			} catch (Exception e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		}
	}

}
