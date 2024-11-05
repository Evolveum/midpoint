/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OptionObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utilities related to GetOperationOptions and their externalized ("bean") representation.
 */
public class GetOperationOptionsUtil {

    @Contract("null -> null; !null -> !null")
    public static SelectorQualifiedGetOptionsType optionsToOptionsBeanNullable(
            Collection<SelectorOptions<GetOperationOptions>> options) {
        return options != null ?
                optionsToOptionsBean(options) : null;
    }

    @SuppressWarnings("WeakerAccess")
    public static @NotNull SelectorQualifiedGetOptionsType optionsToOptionsBean(
            @NotNull Collection<SelectorOptions<GetOperationOptions>> options) {
        SelectorQualifiedGetOptionsType optionsType = new SelectorQualifiedGetOptionsType();
        List<SelectorQualifiedGetOptionType> retval = new ArrayList<>();
        for (SelectorOptions<GetOperationOptions> option : options) {
            retval.add(selectorOptionToSelectorQualifiedGetOptionType(option));
        }
        optionsType.getOption().addAll(retval);
        return optionsType;
    }

    private static SelectorQualifiedGetOptionType selectorOptionToSelectorQualifiedGetOptionType(
            SelectorOptions<GetOperationOptions> selectorOption) {
        OptionObjectSelectorType selectorType = selectorToSelectorType(selectorOption.getSelector());
        GetOperationOptionsType getOptionsType = getOptionsToGetOptionsType(selectorOption.getOptions());
        SelectorQualifiedGetOptionType selectorOptionType = new SelectorQualifiedGetOptionType();
        selectorOptionType.setOptions(getOptionsType);
        selectorOptionType.setSelector(selectorType);
        return selectorOptionType;
    }

    private static OptionObjectSelectorType selectorToSelectorType(ObjectSelector selector) {
        if (selector == null) {
            return null;
        }
        OptionObjectSelectorType selectorType = new OptionObjectSelectorType();
        selectorType.setPath(new ItemPathType(selector.getPath()));
        return selectorType;
    }

    private static GetOperationOptionsType getOptionsToGetOptionsType(GetOperationOptions options) {
        GetOperationOptionsType optionsType = new GetOperationOptionsType();
        optionsType.setRetrieve(RetrieveOption.toRetrieveOptionType(options.getRetrieve()));
        optionsType.setResolve(options.getResolve());
        optionsType.setResolveNames(options.getResolveNames());
        optionsType.setNoFetch(options.getNoFetch());
        optionsType.setRaw(options.getRaw());
        optionsType.setTolerateRawData(options.getTolerateRawData());
        optionsType.setNoDiscovery(options.getDoNotDiscovery());
        // TODO relational value search query (but it might become obsolete)
        optionsType.setAllowNotFound(options.getAllowNotFound());
        optionsType.setPointInTimeType(PointInTimeType.toPointInTimeTypeType(options.getPointInTimeType()));
        optionsType.setDefinitionProcessing(
                DefinitionProcessingOption.toDefinitionProcessingOptionType(
                        options.getDefinitionProcessing()));
        optionsType.setStaleness(options.getStaleness());
        optionsType.setDistinct(options.getDistinct());
        optionsType.setShadowClassificationMode(options.getShadowClassificationMode());
        return optionsType;
    }

    public static List<SelectorOptions<GetOperationOptions>> optionsBeanToOptions(
            SelectorQualifiedGetOptionsType objectOptionsType) {
        if (objectOptionsType == null) {
            return null;
        }
        List<SelectorOptions<GetOperationOptions>> retval = new ArrayList<>();
        for (SelectorQualifiedGetOptionType optionType : objectOptionsType.getOption()) {
            retval.add(selectorQualifiedGetOptionTypeToSelectorOption(optionType));
        }
        return retval;
    }

    private static SelectorOptions<GetOperationOptions> selectorQualifiedGetOptionTypeToSelectorOption(
            SelectorQualifiedGetOptionType objectOptionsType) {
        ObjectSelector selector = selectorTypeToSelector(objectOptionsType.getSelector());
        GetOperationOptions options = getOptionsTypeToGetOptions(objectOptionsType.getOptions());
        return new SelectorOptions<>(selector, options);
    }

    private static GetOperationOptions getOptionsTypeToGetOptions(GetOperationOptionsType optionsType) {
        GetOperationOptions options = new GetOperationOptions();
        options.setRetrieve(RetrieveOption.fromRetrieveOptionType(optionsType.getRetrieve()));
        options.setResolve(optionsType.isResolve());
        options.setResolveNames(optionsType.isResolveNames());
        options.setNoFetch(optionsType.isNoFetch());
        options.setRaw(optionsType.isRaw());
        options.setTolerateRawData(optionsType.isTolerateRawData());
        options.setDoNotDiscovery(optionsType.isNoDiscovery());
        // TODO relational value search query (but it might become obsolete)
        options.setAllowNotFound(optionsType.isAllowNotFound());
        options.setPointInTimeType(PointInTimeType.toPointInTimeType(optionsType.getPointInTimeType()));
        options.setDefinitionProcessing(DefinitionProcessingOption.toDefinitionProcessingOption(optionsType.getDefinitionProcessing()));
        options.setStaleness(optionsType.getStaleness());
        options.setDistinct(optionsType.isDistinct());
        options.setShadowClassificationMode(optionsType.getShadowClassificationMode());
        options.setIterationPageSize(optionsType.getIterationPageSize());
        return options;
    }

    private static ObjectSelector selectorTypeToSelector(OptionObjectSelectorType selectorType) {
        if (selectorType == null) {
            return null;
        }
        return new ObjectSelector(PrismContext.get().toUniformPath(selectorType.getPath()));
    }
}
