/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.util.GetOperationOptionsUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

/**
 * Work definition that can provide object set specification.
 *
 * It has to be aware of an activity type name because of the default implementation of {@link AffectedObjectSetProvider#getAffectedObjectSetInformation(AbstractActivityWorkStateType)}.
 */
public interface ResourceObjectSetSpecificationProvider
        extends AffectedObjectSetProvider, FailedObjectsSelectorProvider {

    @NotNull ResourceObjectSetType getResourceObjectSetSpecification();

    /** Provided here to avoid code duplication in individual work definition implementations. */
    @Override
    default @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(@Nullable AbstractActivityWorkStateType state) {
        ResourceObjectSetType set = getResourceObjectSetSpecification();

        // Currently, all object classes are in ri: namespace. (Otherwise we'd have a big problem,
        // as we would need to fetch the resource, etc. And that would require task, result, error handling, etc.)
        QName rawClassName = set.getObjectclass();
        QName qualifiedClassName = rawClassName != null ? QNameUtil.qualifyIfNeeded(rawClassName, NS_RI) : null;

        return AffectedObjectsInformation.ObjectSet.resource(
                new BasicResourceObjectSetType()
                        .resourceRef(CloneUtil.cloneCloneable(set.getResourceRef())) // Consider keeping only the OID
                        .kind(set.getKind())
                        .intent(set.getIntent())
                        .objectclass(qualifiedClassName));
    }

    @Override
    default FailedObjectsSelectorType getFailedObjectsSelector() {
        return getResourceObjectSetSpecification().getFailedObjectsSelector();
    }

    // TODO consider removing this method
    default boolean isNoFetchMode() {
        return GetOperationOptions.isNoFetch(
                GetOperationOptionsUtil.optionsBeanToOptions(
                        getResourceObjectSetSpecification().getSearchOptions()));
    }
}
