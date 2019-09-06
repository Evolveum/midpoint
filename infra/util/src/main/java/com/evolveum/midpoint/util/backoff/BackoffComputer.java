/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.backoff;

/**
 * @author mederly
 */
public interface BackoffComputer {

	class NoMoreRetriesException extends Exception {
		NoMoreRetriesException(String message) {
			super(message);
		}
	}

	/**
	 * @param retryNumber starts at 1
	 */
	long computeDelay(int retryNumber) throws NoMoreRetriesException;

}
