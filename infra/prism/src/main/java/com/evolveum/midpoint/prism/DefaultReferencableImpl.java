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

import com.evolveum.midpoint.prism.xjc.PrismForJAXBUtil;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Used when PrismReferenceValue.getRealValue is called, and no referencable is present in the PRV.
 * It is analogous to ObjectReferenceType; however, the ORT is part of common-3, whereas this one is located in prism layer.
 *
 * @author mederly
 */
public class DefaultReferencableImpl implements Referencable, Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private PrismReferenceValue referenceValue;

	public DefaultReferencableImpl(PrismReferenceValue value) {
		this.referenceValue = value;
	}

	@Override
	public PrismReferenceValue asReferenceValue() {
		return referenceValue;
	}

	@Override
	public void setupReferenceValue(PrismReferenceValue value) {
		referenceValue = value;
	}

	@Override
	public String getOid() {
		return referenceValue.getOid();
	}

	@Override
	public QName getType() {
		return referenceValue.getTargetType();
	}

	@Override
	public PolyStringType getTargetName() {
		return PrismForJAXBUtil.getReferenceTargetName(referenceValue);
	}

	@Override
	public QName getRelation() {
		return referenceValue.getRelation();
	}

	@Override
	public String getDescription() {
		return referenceValue.getDescription();
	}

	@Override
	public EvaluationTimeType getResolutionTime() {
		return referenceValue.getResolutionTime();
	}

	@Override
	public SearchFilterType getFilter() {
		SearchFilterType filter = new SearchFilterType();
		filter.setFilterClauseXNode(PrismForJAXBUtil.getReferenceFilterClauseXNode(referenceValue));
		return filter;
	}

	public DefaultReferencableImpl clone() {
		DefaultReferencableImpl clone;
		try {
			clone = (DefaultReferencableImpl) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		if (referenceValue != null) {
			clone.referenceValue = referenceValue.clone();
		}
		return clone;
	}

	@Override
	public String toString() {
		return "DefaultReferencableImpl(" + referenceValue + ')';
	}
}
