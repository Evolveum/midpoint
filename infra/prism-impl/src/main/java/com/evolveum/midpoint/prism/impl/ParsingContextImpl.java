/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ParsingContextImpl implements ParsingContext {

	private XNodeProcessorEvaluationMode evaluationMode = XNodeProcessorEvaluationMode.STRICT;
	private boolean allowMissingRefTypes;
	private final List<String> warnings = new ArrayList<>();

	ParsingContextImpl() {
	}

	static ParsingContext allowMissingRefTypes() {
		ParsingContextImpl pc = new ParsingContextImpl();
		pc.setAllowMissingRefTypes(true);
		return pc;
	}


	public static ParsingContext createForCompatibilityMode() {
		return forMode(XNodeProcessorEvaluationMode.COMPAT);
	}

	static ParsingContext forMode(XNodeProcessorEvaluationMode mode) {
		ParsingContextImpl pc = new ParsingContextImpl();
		pc.setEvaluationMode(mode);
		return pc;
	}

	public static ParsingContext createDefault() {
		return new ParsingContextImpl();
	}

	@SuppressWarnings("SameParameterValue")
	void setAllowMissingRefTypes(boolean allowMissingRefTypes) {
		this.allowMissingRefTypes = allowMissingRefTypes;
	}

	void setEvaluationMode(XNodeProcessorEvaluationMode evaluationMode) {
		this.evaluationMode = evaluationMode;
	}

	public boolean isAllowMissingRefTypes() {
		return allowMissingRefTypes;
	}

	public XNodeProcessorEvaluationMode getEvaluationMode() {
		return evaluationMode;
	}

	public boolean isCompat() {
		return evaluationMode == XNodeProcessorEvaluationMode.COMPAT;
	}

	public boolean isStrict() {
		return evaluationMode == XNodeProcessorEvaluationMode.STRICT;
	}

	public void warn(Trace logger, String message) {
		logger.warn("{}", message);
		warn(message);
	}

	public void warnOrThrow(Trace logger, String message) throws SchemaException {
		warnOrThrow(logger, message, null);
	}

	public void warnOrThrow(Trace logger, String message, Throwable t) throws SchemaException {
		if (isCompat()) {
			logger.warn("{}", message, t);
			warn(message);
		} else {
			throw new SchemaException(message, t);
		}
	}

	public void warn(String message) {
		warnings.add(message);
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

	public ParsingContext clone() {
		ParsingContextImpl clone;
		try {
			clone = (ParsingContextImpl) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		clone.evaluationMode = evaluationMode;
		clone.allowMissingRefTypes = allowMissingRefTypes;
		clone.warnings.addAll(warnings);
		return clone;
	}

	public ParsingContext strict() {
		this.setEvaluationMode(XNodeProcessorEvaluationMode.STRICT);
		return this;
	}

	public ParsingContext compat() {
		this.setEvaluationMode(XNodeProcessorEvaluationMode.COMPAT);
		return this;
	}
}
