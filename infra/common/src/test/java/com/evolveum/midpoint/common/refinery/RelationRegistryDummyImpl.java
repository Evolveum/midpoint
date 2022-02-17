/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_DEFAULT;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

/**
 * Temporary workaround: in TestRefinedSchema we don't have spring context.
 */
class RelationRegistryDummyImpl implements RelationRegistry {

    @Override
    public List<RelationDefinitionType> getRelationDefinitions() {
        return emptyList();
    }

    @Override
    public RelationDefinitionType getRelationDefinition(QName relation) {
        return null;
    }

    @Override
    public boolean isOfKind(QName relation, RelationKindType kind) {
        return kind == RelationKindType.MEMBER && (relation == null || QNameUtil.match(relation, ORG_DEFAULT));
    }

    @Override
    public boolean isProcessedOnLogin(QName relation) {
        return false;
    }

    @Override
    public boolean isProcessedOnRecompute(QName relation) {
        return false;
    }

    @Override
    public boolean isStoredIntoParentOrgRef(QName relation) {
        return false;
    }

    @Override
    public boolean isAutomaticallyMatched(QName relation) {
        return false;
    }

    @Override
    public QName getDefaultRelation() {
        return ORG_DEFAULT;
    }

    @NotNull
    @Override
    public Collection<QName> getAllRelationsFor(RelationKindType kind) {
        return kind == RelationKindType.MEMBER ? singletonList(ORG_DEFAULT) : emptyList();
    }

    @Override
    public QName getDefaultRelationFor(RelationKindType kind) {
        return kind == RelationKindType.MEMBER ? ORG_DEFAULT : null;
    }

    @NotNull
    @Override
    public QName normalizeRelation(QName relation) {
        return relation == null ? ORG_DEFAULT : relation;
    }

    @Override
    public void applyRelationsConfiguration(SystemConfigurationType systemConfiguration) {
    }

    @Override
    public boolean isDefault(QName relation) {
        return relation == null || QNameUtil.match(relation, ORG_DEFAULT);
    }

    @NotNull
    @Override
    public Collection<QName> getAliases(QName relation) {
        return singleton(relation);
    }
}
