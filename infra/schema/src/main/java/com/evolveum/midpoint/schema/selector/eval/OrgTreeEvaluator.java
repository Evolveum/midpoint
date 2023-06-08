/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Evaluates organization tree questions. Usually implemented by the repository.
 */
public interface OrgTreeEvaluator {

    /**
     * Returns `true` if the `object` is above organization identified with `descendantOrgOid`.
     * Despite type parameter, only `PrismObject<OrgType>` can return `true`.
     *
     * Examples (from the perspective of the first parameter):
     *
     * * Any other type than `Org` used for `object` returns `false`.
     * * Organization being a parent of another organization with `descendantOrgOid` returns `true`.
     * This means that Organization with `descendantOrgOid` has `parentOrgRef` to `object`.
     * * Organization higher in the organization hierarchy than Org with `descendantOrgOid`
     * returns `true`, for any number of levels between them as long as it's possible to traverse
     * from Org identified by `descendantOrgOid` to `object` using any number of `parentOrgRefs`.
     * * Organization with `descendantOrgOid` returns `false`, as it is not considered
     * to be its own ancestor.
     *
     * @param object potential ancestor organization
     * @param descendantOrgOid identifier of potential descendant organization
     */
    <O extends ObjectType> boolean isAncestor(PrismObject<O> object, String descendantOrgOid)
            throws SchemaException;

    /**
     * Returns `true` if the `object` is under the organization identified with `ancestorOrgOid`.
     * The `object` can either be an Org or any other object in which case all the targets
     * of its `parentOrgRefs` are tested.
     *
     * Examples (from the perspective of the first parameter):
     *
     * * User belonging to Org with `ancestorOrgOid` returns true.
     * * Organization under Org with `ancestorOrgOid` returns true (in any depth).
     * * User belonging to Org under another Org with `ancestorOrgOid` returns true (any depth).
     * * Organization with `ancestorOrgOid` returns `false`, as it is not considered
     * to be its own descendant.
     *
     * @param object object of any type tested to belong under Org with `ancestorOrgOid`
     * @param ancestorOrgOid identifier of ancestor organization
     */
    <O extends ObjectType> boolean isDescendant(PrismObject<O> object, String ancestorOrgOid)
            throws SchemaException;

}
