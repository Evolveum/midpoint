/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.impl.xjc.PrismForJAXBUtil;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ReferentialIntegrityType;

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
    public DefaultReferencableImpl setupReferenceValue(PrismReferenceValue value) {
        referenceValue = value;
        return this;
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
    public ReferentialIntegrityType getReferentialIntegrity() {
        return referenceValue.getReferentialIntegrity();
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
