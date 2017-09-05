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
import org.apache.commons.collections4.MultiSet;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

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
	public EvaluationOrder advance(QName relation) {
		return this;
	}

	@Override
	public EvaluationOrder decrease(MultiSet<QName> relations) {
		return this;
	}

	@Override
	public EvaluationOrder clone() {
		return this;
	}

	@Override
	public EvaluationOrder resetOrder(QName relation, int newOrder) {
		return this;
	}

	@Override
	public Map<QName, Integer> diff(EvaluationOrder newState) {
		throw new IllegalStateException("Cannot compute diff on undefined evaluation order");
	}

	@Override
	public EvaluationOrder applyDifference(Map<QName, Integer> difference) {
		return this;
	}

	@Override
	public boolean isDefined() {
		return false;
	}

	@Override
	public boolean isValid() {
		return true;					// undefined order is always valid
	}

	@Override
	public Set<QName> getRelations() {
		return Collections.emptySet();
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
	public void shortDump(StringBuilder sb) {
		sb.append("UNDEFINED");
	}

	@Override
	public Collection<QName> getExtraRelations() {
		return Collections.emptyList();
	}

	@Override
	public boolean isOrderOne() {
		return false; // TODO
	}
}
