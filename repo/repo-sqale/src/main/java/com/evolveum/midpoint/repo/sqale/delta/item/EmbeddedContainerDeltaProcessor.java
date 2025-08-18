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
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleNestedMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Delta processor for whole embedded single-value containers.
 * While the single value is PCV and is complex, the {@link ItemDeltaSingleValueProcessor#process}
 * high-level implementation still works, but the details are more complex and implemented here.
 *
 * @param <T> type of the processed container (target)
 */
public class EmbeddedContainerDeltaProcessor<T extends Containerable>
        extends ItemDeltaSingleValueProcessor<T> {

    private final SqaleNestedMapping<T, ?, ?> mapping;

    /**
     * Constructs the delta processor, here we care about type match for parameters, later we don't.
     *
     * @param <OS> schema type of the owner of the embedded container
     * @param <OQ> query type of the owner entity
     * @param <OR> type of the owner row
     */
    public <OS, OQ extends FlexibleRelationalPathBase<OR>, OR> EmbeddedContainerDeltaProcessor(
            SqaleUpdateContext<OS, OQ, OR> context, SqaleNestedMapping<T, OQ, OR> nestedMapping) {
        super(context);
        this.mapping = nestedMapping;
    }

    /**
     * Sets the values for items in the PCV that are mapped to database columns and nulls the rest.
     */
    public void setValue(T value) throws SchemaException {
        PrismContainerValue<T> pcv = Containerable.asPrismContainerValue(value);
        if (pcv == null) {
            delete(); // we need to clear existing values
            return;
        }

        Map<QName, ? extends ItemSqlMapper<?, ?>> mappers = mapping.getItemMappings();
        for (Item<?, ?> item : pcv.getItems()) {
            ItemSqlMapper<?, ?> mapper = mappers.remove(item.getElementName());
            if (mapper == null) {
                continue; // ok, not mapped to database
            }

            ItemDeltaValueProcessor<?> processor = createItemDeltaProcessor(mapper);
            // while the embedded container is single-value, its items may be multi-value
            processor.setRealValues(item.getRealValues());
        }
        var metadataMapper = mappers.get(InfraItemName.METADATA);
        if (metadataMapper != null) {
            // We do not want remove legacy metadata and value metadata, since we are updating
            // them
            mappers.remove(InfraItemName.METADATA);
            mappers.remove(ObjectType.F_METADATA);
            var metadata = pcv.getValueMetadataAsContainer();
            if (!metadata.isEmpty()) {
                var metadataProcessor = createItemDeltaProcessor(metadataMapper);
                metadataProcessor.setRealValues(metadata.getRealValues());


            }

        }
        // clear the rest of the values
        mappers.values().forEach(this::deleteUsing);
    }

    /**
     * Sets all columns for the embedded container to `null` using all the known mappers.
     */
    @Override
    public void delete() {
        for (ItemSqlMapper<?, ?> mapper : mapping.getItemMappings().values()) {
            deleteUsing(mapper);
        }
    }

    private void deleteUsing(ItemSqlMapper<?, ?> mapper) {
        // It seems simple to just set all the columns, but only mappers know the columns
        // and only delta processors know how to properly clear it.
        ItemDeltaValueProcessor<?> processor = createItemDeltaProcessor(mapper);
        processor.delete();
    }

    private ItemDeltaValueProcessor<?> createItemDeltaProcessor(ItemSqlMapper<?, ?> mapper) {
        if (!(mapper instanceof SqaleItemSqlMapper)) {
            throw new IllegalArgumentException("No delta processor available for " + mapper
                    + " in mapping " + mapping + "! (Only query mapping is available.)");
        }

        SqaleItemSqlMapper<?, ?, ?> sqaleMapper = (SqaleItemSqlMapper<?, ?, ?>) mapper;
        return sqaleMapper.createItemDeltaProcessor(context);
    }
}
