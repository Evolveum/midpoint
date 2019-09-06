/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.marshaller;

/**
 * @author semancik
 *
 */
public enum XNodeProcessorEvaluationMode {
	/**
	 * Strict mode. Any inconsistency of data with the schema is considered to be an error.
	 */
	STRICT,

	/**
	 * Compatibility mode. The processing will go on as long as the data are roughly compatible
	 * with the schema. E.g. illegal values and unknown elements are silently ignored.
	 */
	COMPAT
}
