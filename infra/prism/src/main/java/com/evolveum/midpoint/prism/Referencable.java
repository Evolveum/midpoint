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

package com.evolveum.midpoint.prism;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

/**
 * @author Katka Valalikova
 *
 * TODO think about the exact purpose and use of this interface
 */
public interface Referencable {

	PrismReferenceValue asReferenceValue();

	void setupReferenceValue(PrismReferenceValue value);

	String getOid();

	QName getType();

	PolyStringType getTargetName();

	QName getRelation();

	String getDescription();

	EvaluationTimeType getResolutionTime();

	SearchFilterType getFilter();
}
