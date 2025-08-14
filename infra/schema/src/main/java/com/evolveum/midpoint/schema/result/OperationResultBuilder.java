/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultImportanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *  Used to postpone initialization of OperationResult until parameters and context items are set - in order to log
 *  operation entry correctly.
 *
 *  Operation names are not "builder-style" but let's ignore this for the moment. This is to simplify the implementation;
 *  can be fixed later.
 */
public interface OperationResultBuilder {
    OperationResult build();

    OperationResult addQualifier(String value);

    OperationResultBuilder addParam(String name, String value);

    OperationResultBuilder addParam(String name, PrismObject<? extends ObjectType> value);

    OperationResultBuilder addParam(String name, ObjectType value);

    OperationResultBuilder addParam(String name, boolean value);

    OperationResultBuilder addParam(String name, long value);

    OperationResultBuilder addParam(String name, int value);

    OperationResultBuilder addParam(String name, Class<?> value);

    OperationResultBuilder addParam(String name, QName value);

    OperationResultBuilder addParam(String name, PolyString value);

    OperationResultBuilder addParam(String name, ResourceObjectTypeIdentification value);

    OperationResultBuilder addParam(String name, ObjectQuery value);

    OperationResultBuilder addParam(String name, ObjectDelta<?> value);

    OperationResultBuilder addParam(String name, String... values);

    OperationResultBuilder addArbitraryObjectAsParam(String paramName, Object paramValue);

    OperationResultBuilder addArbitraryObjectCollectionAsParam(String name, Collection<?> value);

    OperationResultBuilder addContext(String name, String value);

    OperationResultBuilder addContext(String name, PrismObject<? extends ObjectType> value);

    OperationResultBuilder addContext(String name, ObjectType value);

    OperationResultBuilder addContext(String name, boolean value);

    OperationResultBuilder addContext(String name, long value);

    OperationResultBuilder addContext(String name, int value);

    OperationResultBuilder addContext(String name, Class<?> value);

    OperationResultBuilder addContext(String name, QName value);

    OperationResultBuilder addContext(String name, PolyString value);

    OperationResultBuilder addContext(String name, ObjectQuery value);

    OperationResultBuilder addContext(String name, ObjectDelta<?> value);

    OperationResultBuilder addContext(String name, String... values);

    OperationResultBuilder addArbitraryObjectAsContext(String name, Object value);

    @SuppressWarnings("unused")
    OperationResultBuilder addArbitraryObjectCollectionAsContext(String paramName, Collection<?> paramValue);

    OperationResultBuilder setMinor();

    OperationResult setImportance(OperationResultImportanceType value);

    OperationResultBuilder tracingProfile(CompiledTracingProfile profile);

    OperationResultBuilder operationKind(OperationKindType kind);

    OperationResultBuilder preserve();

    OperationResultBuilder notRecordingValues();
}
