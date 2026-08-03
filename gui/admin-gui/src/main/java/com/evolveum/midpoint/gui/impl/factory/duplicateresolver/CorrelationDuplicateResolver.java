/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.duplicateresolver;

import com.evolveum.midpoint.common.cleanup.ObjectCleaner;
import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.util.QNameUtil;

import java.util.Collections;

/**
 * Resolver responsible for duplicating {@link ItemsSubCorrelatorType} containers.
 * <p>
 * Creates a deep copy of the correlator configuration, removes container IDs,
 * and adjusts user-visible fields (name, display name, description)
 * to indicate that the object is a duplicated copy.
 */
@Component
public class CorrelationDuplicateResolver extends ContainerDuplicateResolver<ItemsSubCorrelatorType> {

    private static final String COPY_OF_KEY = "DuplicationProcessHelper.copyOf";

    @Override
    public boolean match(@NotNull ItemDefinition<?> def) {
        return QNameUtil.match(def.getTypeName(), ItemsSubCorrelatorType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public ItemsSubCorrelatorType duplicateObject(@NotNull ItemsSubCorrelatorType originalBean, PageBase pageBase) {
        @SuppressWarnings("unchecked")
        PrismContainerValue<ItemsSubCorrelatorType> originalObject =
                (PrismContainerValue<ItemsSubCorrelatorType>) originalBean.asPrismContainerValue();

        PrismContainerValue<ItemsSubCorrelatorType> duplicate = duplicateItemSubCorrelatorValue(originalObject);
        @NotNull ItemsSubCorrelatorType duplicatedBean = duplicate.asContainerable();

        duplicatedBean
                .name(copyOf(originalBean.getName()))
                .displayName(copyOf(originalBean.getDisplayName()))
                .description(copyOf(originalBean.getDescription()));

        return duplicatedBean;
    }

    private String copyOf(String value) {
        return value != null
                ? LocalizationUtil.translate(COPY_OF_KEY, new Object[] { value })
                : null;
    }

    /**
     * Creates a duplicate of the provided ItemsSubCorrelatorType value without attaching it
     * to a parent container and removes IDs.
     */
    public static PrismContainerValue<ItemsSubCorrelatorType> duplicateItemSubCorrelatorValue(
            PrismContainerValue<ItemsSubCorrelatorType> container) {
        PrismContainerValue<ItemsSubCorrelatorType> duplicate = PrismValueCollectionsUtil.cloneCollectionComplex(
                        CloneStrategy.REUSE,
                        Collections.singletonList(container))
                .iterator().next();
        ObjectCleaner cleanupProcessor = new ObjectCleaner();
        cleanupProcessor.setRemoveContainerIds(true);
        cleanupProcessor.process(duplicate);
        return duplicate;
    }
}
