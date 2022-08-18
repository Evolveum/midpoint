/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import com.evolveum.midpoint.model.api.indexing.Normalization;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IndexedItemNormalizationDefinitionType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public class NormalizationImpl implements Normalization {

    @NotNull private final String name;
    @NotNull private final String normalizedItemLocalName;
    @NotNull private final IndexedItemNormalizationDefinitionType bean;
    @NotNull private final Collection<NormalizationStep<?>> steps;

    private NormalizationImpl(
            @NotNull String name,
            @NotNull String normalizedItemLocalName,
            @NotNull IndexedItemNormalizationDefinitionType bean,
            @NotNull Collection<NormalizationStep<?>> steps) {
        this.name = name;
        this.normalizedItemLocalName = normalizedItemLocalName;
        this.bean = bean;
        this.steps = steps;
    }

    public static Normalization create(
            @NotNull String indexedItemName,
            @NotNull IndexedItemNormalizationDefinitionType normalizationBean) {
        Collection<NormalizationStep<?>> parsedSteps = NormalizationStep.parse(normalizationBean.getSteps());
        String normalizationName = getNormalizationName(normalizationBean, parsedSteps);
        String normalizedItemLocalName =
                getNormalizedItemLocalName(indexedItemName, normalizationName, normalizationBean);
        return new NormalizationImpl(normalizationName, normalizedItemLocalName, normalizationBean, parsedSteps);
    }

    private static @NotNull String getNormalizationName(
            IndexedItemNormalizationDefinitionType normalizationBean,
            Collection<NormalizationStep<?>> parsedSteps) {
        String explicitName = normalizationBean.getName();
        if (explicitName != null) {
            return explicitName;
        } else {
            return getNormalizationSuffix(parsedSteps);
        }
    }

    private static String getNormalizedItemLocalName(
            String indexedItemName,
            String normalizationName,
            IndexedItemNormalizationDefinitionType normalizationBean) {
        String explicitItemName = normalizationBean.getIndexedNormalizedItemName();
        return explicitItemName != null ?
                explicitItemName :
                indexedItemName + "." + normalizationName;
    }

    private static String getNormalizationSuffix(Collection<NormalizationStep<?>> steps) {
        return steps.stream()
                .map(NormalizationStep::asSuffix)
                .collect(Collectors.joining("."));
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public boolean isDefault() {
        return Boolean.TRUE.equals(bean.isDefault());
    }

    @Override
    public ItemName getIndexItemName() {
        return new ItemName(SchemaConstants.NS_IDENTITY, normalizedItemLocalName);
    }

    @Override
    public ItemPath getIndexItemPath() {
        return SchemaConstants.PATH_IDENTITY_SEARCH_ITEMS.append(getIndexItemName());
    }

    @Override
    public @NotNull PrismPropertyDefinition<String> getIndexItemDefinition() {
        // FIXME (not always String)
        MutablePrismPropertyDefinition<String> definition = PrismContext.get().definitionFactory()
                .createPropertyDefinition(getIndexItemName(), DOMUtil.XSD_STRING);
        definition.setMinOccurs(0);
        definition.setMaxOccurs(-1);
        definition.setDynamic(true);
        return definition;
    }

    @Override
    public @NotNull String normalize(@NotNull String input, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        for (NormalizationStep<?> step : steps) {
            input = step.execute(input, task, result);
        }
        return input;
    }

    @Override
    public String toString() {
        return "Normalization{" +
                "name='" + name + '\'' +
                (isDefault() ? ",default" : "") +
                '}';
    }
}
