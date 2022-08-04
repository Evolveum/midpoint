/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.CorrelationProperty;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.impl.correlator.FullCorrelationContext;
import com.evolveum.midpoint.model.impl.correlator.items.CorrelationItem;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.util.MatchingUtil;
import com.evolveum.midpoint.schema.util.cases.CorrelationCaseUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Creates {@link CorrelationProperty} list to be used e.g. in the GUI.
 *
 * TEMPORARY
 */
@Experimental
class CorrelationPropertiesCreator {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationPropertiesCreator.class);

    @NotNull private final CorrelatorContext<?> correlatorContext;
    @NotNull private final FullCorrelationContext fullCorrelationContext;
    @NotNull private final ObjectType preFocus;

    CorrelationPropertiesCreator(
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull FullCorrelationContext fullCorrelationContext,
            @NotNull CaseType aCase) {
        this.correlatorContext = correlatorContext;
        this.fullCorrelationContext = fullCorrelationContext;
        this.preFocus = CorrelationCaseUtil.getPreFocusRequired(aCase);
    }

    public Collection<CorrelationProperty> createProperties()
            throws ConfigurationException, SchemaException {
        List<CorrelationItem> correlationItems = getExplicitCorrelationItems();
        if (!correlationItems.isEmpty()) {
            return createFromCorrelationItems(correlationItems);
        } else {
            return createFromPreFocus();
        }
    }

    private List<CorrelationItem> getExplicitCorrelationItems() throws ConfigurationException {
        List<CorrelationItem> correlationItems = new ArrayList<>();
        for (Map.Entry<String, CorrelationItemDefinitionType> entry : correlatorContext.getItemDefinitionsMap().entrySet()) {
            CorrelationItem correlationItem = CorrelationItem.create(
                    entry.getValue(),
                    correlatorContext,
                    preFocus);
            LOGGER.trace("Created correlation item: {}", correlationItem);
            correlationItems.add(correlationItem);
        }
        return correlationItems;
    }

    private List<CorrelationProperty> createFromCorrelationItems(List<CorrelationItem> correlationItems)
            throws SchemaException {
        List<CorrelationProperty> definitions = new ArrayList<>();
        for (CorrelationItem correlationItem : correlationItems) {
            definitions.add(
                    correlationItem.getSourceCorrelationPropertyDefinition());
        }
        return definitions;
    }

    private Collection<CorrelationProperty> createFromPreFocus() {
        List<PrismProperty<?>> properties = MatchingUtil.getSingleValuedProperties(preFocus);
        PathKeyedMap<CorrelationProperty> correlationPropertiesMap = new PathKeyedMap<>();
        for (PrismProperty<?> property : properties) {
            ItemPath path = property.getPath().namedSegmentsOnly();
            CorrelationProperty existing = correlationPropertiesMap.get(path);
            if (existing == null) {
                correlationPropertiesMap.put(path,
                        CorrelationProperty.createSimple(
                                property.getRealValues(),
                                path,
                                property.getDefinition()));
            } else {
                correlationPropertiesMap.put(path,
                        existing.merge(
                                property.getRealValues(),
                                property.getDefinition()));
            }
        }
        return correlationPropertiesMap.values();
    }
}
