/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqale.delta.item.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;

/**
 * Processor for embedded containers.
 * Due to stateful nature of processing call either {@link #process(ItemDelta)}
 * or {@link #delete()} and call it only once - *do not reuse this processor instance*.
 */
public class EmbeddedContainerDeltaProcessor<T extends Containerable>
        extends ItemDeltaValueProcessor<T> {

    private final SqaleNestedMapping<?, ?, ?> mapping;
    private final Map<QName, ItemSqlMapper> mappers; // iterated in a stateful manner

    public EmbeddedContainerDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> context, SqaleNestedMapping<?, ?, ?> nestedMapping) {
        super(context);
        this.mapping = nestedMapping;
        mappers = mapping.getItemMappings();
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        if (!modification.isDelete()) {
            setValue(getAnyValue(modification));
        }

        // we want to delete values for all mapper unused until now
        delete();
    }

    /**
     * Sets the values for items in the PCV that are mapped to database columns.
     * Removes entries for used items from the {@link #mappers}.
     */
    public void setValue(T value) {
        PrismContainerValue<T> pcv = Containerable.asPrismContainerValue(value);
        if (pcv == null) {
            return;
        }

        for (Item<?, ?> item : pcv.getItems()) {
            ItemSqlMapper mapper = mappers.remove(item.getElementName());
            if (mapper == null) {
                continue; // ok, not mapped to database
            }

            if (!(mapper instanceof SqaleItemSqlMapper)) {
                throw new IllegalArgumentException("No delta processor available for " + mapper
                        + " in mapping " + mapping + "! (Only query mapping is available.)");
            }
            SqaleItemSqlMapper sqaleMapper = (SqaleItemSqlMapper) mapper;
            ItemDeltaValueProcessor<?> processor = sqaleMapper.createItemDeltaProcessor(context);
            if (processor == null) {
                // TODO this should not happen when all mapper types are covered (e.g. ref tables)
                System.out.println("PROCESSOR NULL for: " + sqaleMapper);
                return;
            }
            processor.setRealValue(item.getAnyValue().getRealValue());
        }
    }

    @Override
    public void delete() {
        for (ItemSqlMapper mapper : mappers.values()) {
            deleteUsing(mapper);
        }
    }

    private void deleteUsing(ItemSqlMapper mapper) {
        if (!(mapper instanceof SqaleItemSqlMapper)) {
            throw new IllegalArgumentException("No delta processor available for " + mapper
                    + " in mapping " + mapping + "! (Only query mapping is available.)");
        }
        SqaleItemSqlMapper sqaleMapper = (SqaleItemSqlMapper) mapper;
        ItemDeltaValueProcessor<?> processor = sqaleMapper.createItemDeltaProcessor(context);
        if (processor == null) {
            // TODO this should not happen when all mapper types are covered (e.g. ref tables)
            System.out.println("PROCESSOR NULL for: " + sqaleMapper);
            return;
        }
        processor.delete();
    }
}
