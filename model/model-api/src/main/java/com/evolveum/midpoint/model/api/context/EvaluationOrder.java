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

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;

import org.apache.commons.collections4.MultiSet;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author semancik
 * @author mederly
 */
public interface EvaluationOrder extends DebugDumpable, ShortDumpable, Cloneable {

	int getSummaryOrder();

	EvaluationOrder advance(QName relation);

	EvaluationOrder decrease(MultiSet<QName> relations);

	int getMatchingRelationOrder(QName relation);

	Collection<QName> getExtraRelations();

	EvaluationOrder clone();

	EvaluationOrder resetOrder(QName relation, int newOrder);

	// returns the delta that would transform current object state to the newState
	// both current and new states must be defined
	Map<QName, Integer> diff(EvaluationOrder newState);

	EvaluationOrder applyDifference(Map<QName,Integer> difference);

	boolean isDefined();

	Set<QName> getRelations();

	boolean isValid();

	boolean isOrderOne();
}
