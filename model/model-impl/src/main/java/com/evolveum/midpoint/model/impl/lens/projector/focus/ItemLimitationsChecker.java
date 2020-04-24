/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyLimitationsType;

import org.springframework.stereotype.Component;

import java.util.Map;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Checks item limitations.
 *
 * This is a preliminary implementation; to be merged with something related.
 */
@Component
public class ItemLimitationsChecker {

    /**
     * @pre Focus context is recomputed.
     */
    <O extends ObjectType> void checkItemsLimitations(LensFocusContext<O> focusContext) throws SchemaException {
        Map<UniformItemPath, ObjectTemplateItemDefinitionType> itemDefinitionsMap = focusContext.getItemDefinitionsMap();
        PrismObject<O> objectNew = focusContext.getObjectNew();
        if (objectNew == null) {
            return; // nothing to check on DELETE operation
        }
        for (Map.Entry<UniformItemPath, ObjectTemplateItemDefinitionType> entry : itemDefinitionsMap.entrySet()) {
            for (PropertyLimitationsType limitation : entry.getValue().getLimitations()) {
                if (!limitation.getLayer().contains(LayerType.MODEL)) { // or should we apply SCHEMA-layer limitations as well?
                    continue;
                }
                checkItemLimitations(objectNew, entry.getKey(), limitation);
            }
        }
    }

    private <O extends ObjectType> void checkItemLimitations(PrismObject<O> object, ItemPath path, PropertyLimitationsType limitation)
            throws SchemaException {
        Object item = object.find(path);
        if (isTrue(limitation.isIgnore())) {
            return;
        }
        int count = getValueCount(item);
        Integer min = DefinitionUtil.parseMultiplicity(limitation.getMinOccurs());
        if (min != null && min > 0 && count < min) {
            throw new SchemaException("Expected at least " + min + " values of " + path + ", got " + count);
        }
        Integer max = DefinitionUtil.parseMultiplicity(limitation.getMaxOccurs());
        if (max != null && max >= 0 && count > max) {
            throw new SchemaException("Expected at most " + max + " values of " + path + ", got " + count);
        }
    }

    private int getValueCount(Object item) {
        if (item == null) {
            return 0;
        }
        if (!(item instanceof Item)) {
            throw new IllegalStateException("Expected Item but got " + item.getClass() + " instead");
        }
        return ((Item) item).getValues().size();
    }
}
