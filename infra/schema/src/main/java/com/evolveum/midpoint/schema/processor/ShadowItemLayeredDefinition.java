/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.PrismItemAccessDefinition;
import com.evolveum.midpoint.prism.PrismItemMiscDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceItemDefinitionType;

/** Layer-dependent aspects of an item definition. */
public interface ShadowItemLayeredDefinition {

    /**
     * Returns limitations (cardinality, access rights, processing) for given layer.
     *
     * These are obtained from resource and/or explicitly configured.
     *
     * @see ResourceItemDefinitionType#getLimitations()
     */
    PropertyLimitations getLimitations(LayerType layer);

    /**
     * Gets the level of processing for specified layer.
     *
     * @see PrismItemMiscDefinition#getProcessing()
     */
    ItemProcessing getProcessing(LayerType layer);

    /**
     * Is the attribute ignored (at specified layer)?
     */
    default boolean isIgnored(LayerType layer) {
        return getProcessing(layer) == ItemProcessing.IGNORE;
    }

    /**
     * Gets `maxOccurs` limitation for given layer.
     *
     * @see ItemDefinition#getMaxOccurs()
     */
    int getMaxOccurs(LayerType layer);

    /**
     * Gets `minOccurs` limitation for given layer.
     */
    int getMinOccurs(LayerType layer);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isOptional(LayerType layer) {
        return getMinOccurs(layer) == 0;
    }

    default boolean isMandatory(LayerType layer) {
        return !isOptional(layer);
    }

    default boolean isMultiValue(LayerType layer) {
        int maxOccurs = getMaxOccurs(layer);
        return maxOccurs < 0 || maxOccurs > 1;
    }

    default boolean isSingleValue(LayerType layer) {
        return getMaxOccurs(layer) == 1;
    }

    /**
     * Is adding allowed (at specified layer)?
     *
     * @see PrismItemAccessDefinition#canAdd()
     */
    boolean canAdd(LayerType layer);

    /**
     * Is reading allowed (at specified layer)?
     *
     * @see PrismItemAccessDefinition#canRead()
     */
    boolean canRead(LayerType layer);

    /**
     * Is modification allowed (at specified layer)?
     *
     * @see PrismItemAccessDefinition#canModify()
     */
    boolean canModify(LayerType layer);
}
