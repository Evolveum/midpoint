/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.resolution.DataSearchResult;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author lazyman
 */
public class JpaAnyContainerDefinition<T extends JpaAnyContainerDefinition<T>>
        extends JpaDataNodeDefinition<T> {

    public JpaAnyContainerDefinition(Class<? extends RObject> jpaClass) {
        super(jpaClass, null);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Any";
    }

    @Override
    public DataSearchResult nextLinkDefinition(ItemPath path, ItemDefinition itemDefinition, PrismContext prismContext) throws QueryException {
        if (!path.isSingleName()) {
            throw new QueryException("Couldn't resolve paths other than those in the form of single name in extension/attributes container: " + path);
        }
        if (itemDefinition == null) {
            throw new QueryException("Couldn't resolve dynamically defined item path '" + path + "' without proper definition");
        }

        CollectionSpecification collSpec = itemDefinition.isSingleValue() ? null : new CollectionSpecification();
        String jpaName;        // longs, strings, ...
        JpaDataNodeDefinition jpaNodeDefinition;
        if (itemDefinition instanceof PrismPropertyDefinition) {
            try {
                jpaName = RAnyConverter.getAnySetType(itemDefinition, prismContext);
            } catch (SchemaException e) {
                throw new QueryException(e.getMessage(), e);
            }
            jpaNodeDefinition = new JpaAnyPropertyDefinition(Object.class, null);      // TODO
        } else if (itemDefinition instanceof PrismReferenceDefinition) {
            jpaName = "references";
            jpaNodeDefinition = new JpaAnyReferenceDefinition(Object.class, RObject.class);
        } else {
            throw new QueryException("Unsupported 'any' item: " + itemDefinition);
        }
        JpaLinkDefinition<?> linkDefinition = new JpaAnyItemLinkDefinition(itemDefinition, jpaName, collSpec, getOwnerType(), jpaNodeDefinition);
        return new DataSearchResult<>(linkDefinition, ItemPath.EMPTY_PATH);

    }

    // assignment extension has no ownerType, but virtual (object extension / shadow attributes) do have
    protected RObjectExtensionType getOwnerType() {
        return null;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String debugDump(int indent) {
        return super.getShortInfo();
    }
}
