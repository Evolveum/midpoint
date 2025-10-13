/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_C;

/**
 * Work definition that can provide object set specification.
 *
 * It has to be aware of an activity type name because of the default implementation of {@link AffectedObjectSetProvider#getAffectedObjectSetInformation(AbstractActivityWorkStateType)}.
 */
public interface ObjectSetSpecificationProvider
        extends AffectedObjectSetProvider, FailedObjectsSelectorProvider {

    @NotNull ObjectSetType getObjectSetSpecification();

    /** Provided here to avoid code duplication in individual work definition implementations. */
    @Override
    default @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(@Nullable AbstractActivityWorkStateType state) {
        ObjectSetType set = getObjectSetSpecification();

        // Currently, all objects are in "common" namespace (otherwise we should call schema registry,
        // but there's no fully suitable method now)
        var rawType = set.getType();
        QName qualifiedType = rawType != null ? QNameUtil.qualifyIfNeeded(rawType, NS_C) : null;

        return AffectedObjectsInformation.ObjectSet.repository(
                new BasicObjectSetType()
                        .type(qualifiedType)
                        .archetypeRef( // Consider keeping only the OID
                                CloneUtil.cloneCloneable(set.getArchetypeRef())));
    }

    @Override
    default FailedObjectsSelectorType getFailedObjectsSelector() {
        return getObjectSetSpecification().getFailedObjectsSelector();
    }
}
