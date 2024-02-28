/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.resolution.DataSearchResult;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author lazyman
 */
public class JpaAnyContainerDefinition extends JpaDataNodeDefinition {

    JpaAnyContainerDefinition(@NotNull Class<? extends RObject> jpaClass) {
        super(jpaClass, null);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Any";
    }

    @Override
    public DataSearchResult<?> nextLinkDefinition(ItemPath path, ItemDefinition<?> itemDefinition)
            throws QueryException {
        if (!path.isSingleName()) {
            throw new QueryException("Couldn't resolve paths other than those in the form of single name in extension/attributes container: " + path);
        }
        if (itemDefinition == null) {
            throw new QueryException("Couldn't resolve dynamically defined item path '" + path + "' without proper definition");
        }

        CollectionSpecification collSpec = itemDefinition.isSingleValue() ? null : new CollectionSpecification();
        String jpaName; // longs, strings, ...
        JpaAnyItemLinkDefinition<?> linkDefinition;
        if (itemDefinition instanceof PrismPropertyDefinition) {
            try {
                if (!RAnyConverter.isIndexed(itemDefinition, itemDefinition.getItemName(), true)) {
                    throw new QueryException("Can't query non-indexed value for '" + itemDefinition.getItemName()
                            + "', definition " + itemDefinition);
                }

                RAnyConverter.ValueType valueType = RAnyConverter.getValueType(itemDefinition.getTypeName());
                jpaName = switch (valueType) {
                    case BOOLEAN -> "booleans";
                    case DATE -> "dates";
                    case LONG -> "longs";
                    case POLY_STRING -> "polys";
                    default -> "strings";
                };
                // We hope we won't need the specific class information (e.g. ROExtPolyString) here.
                var jpaNodeDefinition = new JpaAnyPropertyDefinition(RAnyValue.class, valueType.getValueType());
                linkDefinition = new JpaAnyItemLinkDefinition<>(itemDefinition, jpaName, collSpec, getOwnerType(), jpaNodeDefinition);
            } catch (SchemaException e) {
                throw new QueryException(e.getMessage(), e);
            }
        } else if (itemDefinition instanceof PrismReferenceDefinition) {
            jpaName = "references";
            var jpaNodeDefinition = new JpaAnyReferenceDefinition(Object.class, RObject.class);
            linkDefinition = new JpaAnyItemLinkDefinition<>(itemDefinition, jpaName, collSpec, getOwnerType(), jpaNodeDefinition);
        } else {
            throw new QueryException("Unsupported 'any' item: " + itemDefinition);
        }
        return new DataSearchResult<>(linkDefinition, ItemPath.EMPTY_PATH);
    }

    // assignment extension has no ownerType, but virtual (object extension / shadow attributes) do have
    protected RObjectExtensionType getOwnerType() {
        return null;
    }

    @Override
    public void accept(Visitor<JpaDataNodeDefinition> visitor) {
        visitor.visit(this);
    }

    @Override
    public String debugDump(int indent) {
        return super.getShortInfo();
    }
}
