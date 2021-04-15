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
 */
public class EmbeddedContainerDeltaProcessor<T extends Containerable>
        extends ItemDeltaValueProcessor<T> {

    private final SqaleNestedMapping<?, ?, ?> mapping;

    public EmbeddedContainerDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> context, SqaleNestedMapping<?, ?, ?> nestedMapping) {
        super(context);
        this.mapping = nestedMapping;
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        if (!modification.isDelete()) {
            setValue(getAnyValue(modification));
        } else {
            delete();
        }
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

        Map<QName, ItemSqlMapper> mappers = mapping.getItemMappings();
        for (Item<?, ?> item : pcv.getItems()) {
            ItemSqlMapper mapper = mappers.remove(item.getElementName());
            if (mapper == null) {
                continue; // ok, not mapped to database
            }

            ItemDeltaValueProcessor<?> processor = createItemDeltaProcessor(mapper);
            // TODO remove: this should not happen when all mapper types are covered (e.g. ref tables)
            if (processor == null) {
                System.out.println("PROCESSOR NULL for: " + mapper);
                continue;
            }
            processor.setRealValue(item.getAnyValue().getRealValue());
        }

        // clear the rest of the values
        mappers.values().forEach(this::deleteUsing);
    }

    @Override
    public void delete() {
        for (ItemSqlMapper mapper : mapping.getItemMappings().values()) {
            deleteUsing(mapper);
        }
    }

    private void deleteUsing(ItemSqlMapper mapper) {
        ItemDeltaValueProcessor<?> processor = createItemDeltaProcessor(mapper);
        // TODO remove: this should not happen when all mapper types are covered (e.g. ref tables)
        if (processor == null) {
            System.out.println("PROCESSOR NULL for: " + mapper);
            return;
        }
        processor.delete();
    }

    private ItemDeltaValueProcessor<?> createItemDeltaProcessor(ItemSqlMapper mapper) {
        if (!(mapper instanceof SqaleItemSqlMapper)) {
            throw new IllegalArgumentException("No delta processor available for " + mapper
                    + " in mapping " + mapping + "! (Only query mapping is available.)");
        }
        SqaleItemSqlMapper sqaleMapper = (SqaleItemSqlMapper) mapper;
        return sqaleMapper.createItemDeltaProcessor(context);
    }
}
