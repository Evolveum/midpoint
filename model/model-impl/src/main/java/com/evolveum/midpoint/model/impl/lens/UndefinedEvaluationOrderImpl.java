/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.util.DebugUtil;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;

/**
 * @author semancik
 *
 */
public class UndefinedEvaluationOrderImpl implements EvaluationOrder {

	UndefinedEvaluationOrderImpl() {
	}

	@Override
	public int getSummaryOrder() {
		return -1; // TODO
	}
	
	@Override
	public EvaluationOrder advance() {
		return this;
	}
	
	@Override
	public EvaluationOrder advance(QName relation) {
		return this;
	}

	private EvaluationOrder advance(int amount, QName relation) {
		return this;
	}

	@Override
	public EvaluationOrder decrease(int amount) {
		return this;
	}

	@Override
	public EvaluationOrder decrease(int amount, QName relation) {
		return this;
	}

	@Override
	public int getMatchingRelationOrder(QName relation) {
		return -1;		// TODO
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "EvaluationOrder UNDEFINED", indent);
		return sb.toString();
	}


	@Override
	public String toString() {
		return "EvaluationOrder(" + shortDump() + ")";
	}
	
	@Override
	public String shortDump() {
		return "UNDEFINED";
	}

	@Override
	public Collection<QName> getExtraRelations() {
		return Collections.emptyList();
	}
}
