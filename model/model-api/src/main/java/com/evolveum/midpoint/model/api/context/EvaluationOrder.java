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

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.util.DebugDumpable;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author semancik
 * @author mederly
 */
public interface EvaluationOrder extends DebugDumpable {

	int getSummaryOrder();

	EvaluationOrder advance();

	EvaluationOrder advance(QName relation);

	EvaluationOrder decrease(int amount);

	EvaluationOrder decrease(int amount, QName relation);

	int getMatchingRelationOrder(QName relation);

	String shortDump();

	Collection<QName> getExtraRelations();
}
