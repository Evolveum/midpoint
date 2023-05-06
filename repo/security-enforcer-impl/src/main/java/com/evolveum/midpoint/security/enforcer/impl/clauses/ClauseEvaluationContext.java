/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SubjectedObjectSelectorType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/** TODO */
public interface ClauseEvaluationContext {

    String getPrincipalOid();

    FocusType getPrincipalFocus();

    Object getDesc();

    @NotNull Collection<String> getOtherSelfOids();

    String getAutzDesc();

    @Nullable OwnerResolver getOwnerResolver();

    boolean isSelectorApplicable(
            @NotNull SubjectedObjectSelectorType selector,
            @Nullable PrismObject<? extends ObjectType> object,
            @NotNull Collection<String> otherSelfOids,
            @NotNull String desc)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    Collection<String> getDelegatorsForAssignee();
    Collection<String> getDelegatorsForRelatedObjects();
    Collection<String> getDelegatorsForRequestor();

    String[] getSelfAndOtherOids(Collection<String> otherOids);

    @NotNull RepositoryService getRepositoryService();

    PrismObject<? extends ObjectType> resolveReference(
            ObjectReferenceType ref, PrismObject<? extends ObjectType> object, String referenceName);
}
