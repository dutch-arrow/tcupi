/*
 * Copyright © 2023 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 25 Jan 2023.
 */


package nl.das.tcu.rest.handlers;

import java.util.Deque;
import java.util.Map;

/**
 *
 */
public class HandlerUtils {

	public static String getParm(Map<String, Deque<String>> parms, String parm) {
		Deque<String> req= parms.get(parm);
		String prm = "";
		if (req != null) {
			prm = req.pop();
		}
		return prm;
	}

}
