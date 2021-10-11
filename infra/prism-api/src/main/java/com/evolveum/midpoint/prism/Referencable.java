/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ReferentialIntegrityType;

import javax.xml.namespace.QName;

/**
 * @author Katka Valalikova
 *
 * TODO think about the exact purpose and use of this interface
 */
public interface Referencable {

    PrismReferenceValue asReferenceValue();

    Referencable setupReferenceValue(PrismReferenceValue value);

    String getOid();

    QName getType();

    PolyStringType getTargetName();

    QName getRelation();

    String getDescription();

    EvaluationTimeType getResolutionTime();

    ReferentialIntegrityType getReferentialIntegrity();

    SearchFilterType getFilter();
}
