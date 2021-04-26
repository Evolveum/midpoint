/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleNestedMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Delta processor for embedded single-value containers.
 *
 * @param <T> type of the processed container (target)
 * @param <OS> schema type of the owner of the embedded container
 * @param <OQ> query type of the owner entity
 * @param <OR> type of the owner row
 */
public class EmbeddedContainerDeltaProcessor<T extends Containerable,
        OS, OQ extends FlexibleRelationalPathBase<OR>, OR>
        extends ItemDeltaSingleValueProcessor<T> {

    // TODO: perhaps this could be <T, OQ, OR> but we need mechanism to create new instance for
    //  embedded context on the same query type for that. The also <OS> can go away.
    private final SqaleUpdateContext<OS, OQ, OR> context;
    private final SqaleNestedMapping<T, OQ, OR> mapping;

    public EmbeddedContainerDeltaProcessor(
            SqaleUpdateContext<OS, OQ, OR> context, SqaleNestedMapping<T, OQ, OR> nestedMapping) {
        super(context);
        this.context = context;
        this.mapping = nestedMapping;
    }

    /**
     * Sets the values for items in the PCV that are mapped to database columns and nulls the rest.
     */
    public void setValue(T value) {
        PrismContainerValue<T> pcv = Containerable.asPrismContainerValue(value);
        if (pcv == null) {
            delete(); // we need to clear existing values
            return;
        }

        Map<QName, ItemSqlMapper<T, OQ, OR>> mappers = mapping.getItemMappings();
        for (Item<?, ?> item : pcv.getItems()) {
            ItemSqlMapper<T, OQ, OR> mapper = mappers.remove(item.getElementName());
            if (mapper == null) {
                continue; // ok, not mapped to database
            }

            ItemDeltaValueProcessor<?> processor = createItemDeltaProcessor(mapper);
            // while the embedded container is single-value, its items may be multi-value
            processor.setRealValues(item.getRealValues());
        }

        // clear the rest of the values
        mappers.values().forEach(this::deleteUsing);
    }

    @Override
    public void delete() {
        for (ItemSqlMapper<T, OQ, OR> mapper : mapping.getItemMappings().values()) {
            deleteUsing(mapper);
        }
    }

    private void deleteUsing(ItemSqlMapper<T, OQ, OR> mapper) {
        ItemDeltaValueProcessor<?> processor = createItemDeltaProcessor(mapper);
        processor.delete();
    }

    private ItemDeltaValueProcessor<?> createItemDeltaProcessor(ItemSqlMapper<?, ?, ?> mapper) {
        if (!(mapper instanceof SqaleItemSqlMapper)) {
            throw new IllegalArgumentException("No delta processor available for " + mapper
                    + " in mapping " + mapping + "! (Only query mapping is available.)");
        }
        // TODO fix parametrized types? Is OS right here? it is required by the context, but
        // this hints at some type misuse or missing one? the context serves both the owning and the embedded schema type
        SqaleItemSqlMapper<OS, OQ, OR> sqaleMapper = (SqaleItemSqlMapper<OS, OQ, OR>) mapper;
        return sqaleMapper.createItemDeltaProcessor(context);
    }
}
