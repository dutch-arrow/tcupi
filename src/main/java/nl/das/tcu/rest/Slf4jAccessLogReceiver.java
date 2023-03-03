/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 04 Oct 2022.
 */


package nl.das.tcu.rest;

import org.slf4j.Logger;

import io.undertow.server.handlers.accesslog.AccessLogReceiver;

public class Slf4jAccessLogReceiver implements AccessLogReceiver {
    private final Logger logger;

    public Slf4jAccessLogReceiver(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void logMessage(String message) {
        this.logger.info("{}", message);
    }
}