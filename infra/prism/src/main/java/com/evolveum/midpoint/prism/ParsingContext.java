/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.parser.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.util.logging.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ParsingContext {

	private XNodeProcessorEvaluationMode evaluationMode = XNodeProcessorEvaluationMode.STRICT;
	private boolean allowMissingRefTypes;
	private final List<String> warnings = new ArrayList<>();

	private ParsingContext() {
	}

	private void setAllowMissingRefTypes(boolean allowMissingRefTypes) {
		this.allowMissingRefTypes = allowMissingRefTypes;
	}

	private void setEvaluationMode(XNodeProcessorEvaluationMode evaluationMode) {
		this.evaluationMode = evaluationMode;
	}

	public boolean isAllowMissingRefTypes() {
		return allowMissingRefTypes;
	}

	public XNodeProcessorEvaluationMode getEvaluationMode() {
		return evaluationMode;
	}

	public static ParsingContext forMode(XNodeProcessorEvaluationMode mode) {
		ParsingContext pc = new ParsingContext();
		pc.setEvaluationMode(mode);
		return pc;
	}

	public static ParsingContext allowMissingRefTypes() {
		ParsingContext pc = new ParsingContext();
		pc.setAllowMissingRefTypes(true);
		return pc;
	}

	public static ParsingContext createDefault() {
		return new ParsingContext();
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

	public void warn(String message) {
		warnings.add(message);
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}
}
