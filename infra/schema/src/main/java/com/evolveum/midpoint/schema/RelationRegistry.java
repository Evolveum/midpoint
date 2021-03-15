/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * A component that holds current definition of object relations.
 *
 * @author mederly
 */
public interface RelationRegistry {

    /**
     * Returns all relation definitions: explicitly specified as well as built-in ones.
     * Invalid or duplicate definitions are filtered out and not mentioned here.
     *
     * Each relation is listed only once even if it can be referenced using various QNames (e.g. null, default, org:default).
     */
    List<RelationDefinitionType> getRelationDefinitions();

    /**
     * Returns a relation definition for a specified relation name.
     * The relation name need not be normalized, i.e. all names for a relation might be used here
     * (e.g. null, default, org:default); resulting in the same definition.
     *
     * Returns null if the definition cannot be found.
     */
    @Nullable RelationDefinitionType getRelationDefinition(QName relation);

    /**
     * Returns true if the relation is of specified kind. The relation name need not be normalized.
     */
    boolean isOfKind(QName relation, RelationKindType kind);

    default boolean isMember(QName relation) {
        return isOfKind(relation, RelationKindType.MEMBER);
    }

    default boolean isManager(QName relation) {
        return isOfKind(relation, RelationKindType.MANAGER);
    }

    default boolean isMeta(QName relation) {
        return isOfKind(relation, RelationKindType.META);
    }

    default boolean isDelegation(QName relation) {
        return isOfKind(relation, RelationKindType.DELEGATION);
    }

    @SuppressWarnings("unused")
    default boolean isApprover(QName relation) {
        return isOfKind(relation, RelationKindType.APPROVER);
    }

    @SuppressWarnings("unused")
    default boolean isOwner(QName relation) {
        return isOfKind(relation, RelationKindType.OWNER);
    }

    /**
     * Whether this kind of relations is processed on login. By default, only relations of MEMBER and DELEGATION kinds are.
     */
    boolean isProcessedOnLogin(QName relation);

    /**
     * Whether this kind of relations is processed on recompute. By default, only relations of MEMBER, MANAGER and DELEGATION kinds are.
     */
    boolean isProcessedOnRecompute(QName relation);

    /**
     * Whether this kind of relations is stored in parentOrgRef. By default, only relations of MEMBER kind are.
     */
    boolean isStoredIntoParentOrgRef(QName relation);

    /**
     * Whether this kind of relations is automatically matched by order constraints. By default, only relations of MEMBER,
     * META and DELEGATION kinds are.
     */
    boolean isAutomaticallyMatched(QName relation);

    /**
     * Returns the default relation i.e. the one that is equivalent to the null relation name.
     * Please do NOT use this information for queries nor determining the behavior of the relation! Use relation kinds instead.
     */
    QName getDefaultRelation();

    /**
     * Checks whether the relation is equivalent to the default one.
     * Please do NOT use this information for determining the behavior of the relation! Use relation kinds instead.
     */
    boolean isDefault(QName relation);

    /**
     * Returns all relations of a given kind. Note that the result might be an empty set; although it is a bad practice to
     * configure midPoint in that way. Unused relations are better hidden using categories.
     */
    @NotNull
    Collection<QName> getAllRelationsFor(RelationKindType kind);

    /**
     * Returns the default relation for a given kind. The result might be a null value; although it is a bad practice to
     * configure midPoint in that way. Unused relations are better hidden using categories.
     */
    @Nullable QName getDefaultRelationFor(RelationKindType kind);

    /**
     * Returns a normalized relation name, i.e. the one that is used in the "ref" item on the definition.
     * It should be qualified (so please DO NOT use unqualified relation names in the definitions!)
     * Returns default relation for null input, never returns null.
     *
     * If the relation is unknown, the relation name is returned unchanged.
     */
    @NotNull
    QName normalizeRelation(QName relation);

    /**
     * This method should be called whenever midPoint determines that the relations definition in system configuration might
     * have been changed.
     */
    void applyRelationsConfiguration(SystemConfigurationType systemConfiguration);

    /**
     * Returns aliases of a relation. Currently these are:
     * - unqualified version of the relation QName
     * - null if the relation is the default one
     *
     * --
     * In the future we might return some other values (e.g. explicitly configured aliases) as well.
     * But we would need to adapt prismContext.relationsEquivalent method and other comparison methods as well.
     * So it is perhaps not worth the effort.
     */
    @NotNull
    Collection<QName> getAliases(QName relation);
}
