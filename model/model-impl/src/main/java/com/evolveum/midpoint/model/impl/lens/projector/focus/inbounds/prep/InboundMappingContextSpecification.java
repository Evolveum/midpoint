/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSynchronizationExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingSpecificationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeIdentificationType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Background information for value provenance metadata ({@link MappingSpecificationType}) for inbound mappings.
 *
 * In other words, it describes the context in which inbound mappings are evaluated: object type, association type, shadow tag.
 *
 * @param typeIdentification For what object type we evaluate the mappings. Note that we are interested in the "top-level"
 * type here: even if we are technically evaluating a mapping for `association/xyz` object type, we are interested in
 * the type of (e.g.) `account/default` in which the association resides.
 *
 * Should be non-null in reasonable cases; only for really broken (unclassified, or without-resource)
 * {@link LensProjectionContext}s it may be null. See the typology of projection contexts in {@link ProjectionContextKey}.
 *
 * @param associationTypeName If the mapping is defined and evaluated in the context of an association,
 * this is the name of the association. Whether it should be present also for inner-level mappings defined e.g. as part
 * of {@link AssociationSynchronizationExpressionEvaluatorType}, is currently an open question.
 *
 * @param shadowTag If the mapping is evaluated in the context of a multi-account resource, this is the shadow tag.
 */
public record InboundMappingContextSpecification(
        @Nullable ResourceObjectTypeIdentification typeIdentification,
        @Nullable QName associationTypeName,
        @Nullable String shadowTag)
        implements Serializable {

    @Nullable ResourceObjectTypeIdentificationType typeIdentificationBean() {
        return ResourceObjectTypeIdentification.asBean(typeIdentification);
    }
}
