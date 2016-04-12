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
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public class ParsingContext {

	private XNodeProcessorEvaluationMode evaluationMode;
	private final PrismContext prismContext;
	private boolean allowMissingRefTypes = false;

	public ParsingContext(PrismContext prismContext) {
		Validate.notNull(prismContext, "prismContext");
		this.prismContext = prismContext;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public boolean isAllowMissingRefTypes() {
		return allowMissingRefTypes;
	}

	public void setAllowMissingRefTypes(boolean allowMissingRefTypes) {
		this.allowMissingRefTypes = allowMissingRefTypes;
	}

	public XNodeProcessorEvaluationMode getEvaluationMode() {
		return evaluationMode;
	}

	public void setEvaluationMode(XNodeProcessorEvaluationMode evaluationMode) {
		this.evaluationMode = evaluationMode;
	}

	public boolean isStrict() {
		return evaluationMode == XNodeProcessorEvaluationMode.STRICT;
	}

	public static ParsingContext forMode(PrismContext prismContext, XNodeProcessorEvaluationMode mode) {
		ParsingContext pc = new ParsingContext(prismContext);
		pc.setEvaluationMode(mode);
		return pc;
	}

	public static ParsingContext allowMissingRefTypes(PrismContext prismContext) {
		ParsingContext pc = new ParsingContext(prismContext);
		pc.setAllowMissingRefTypes(true);
		return pc;
	}

	public static ParsingContext createDefault(PrismContext prismContext) {
		return new ParsingContext(prismContext);
	}

	public boolean isCompat() {
		return evaluationMode == XNodeProcessorEvaluationMode.COMPAT;
	}

	public SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}
}
