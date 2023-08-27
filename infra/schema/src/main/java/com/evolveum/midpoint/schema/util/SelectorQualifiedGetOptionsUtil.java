/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class SelectorQualifiedGetOptionsUtil {

    public static void merge(@NotNull SelectorQualifiedGetOptionsType base, @Nullable IterationMethodType method) {
        mergeRoot(base, GetOperationOptionsType::getIterationMethod, GetOperationOptionsType::setIterationMethod, method);
    }

    static <X> void mergeRoot(SelectorQualifiedGetOptionsType base, Function<GetOperationOptionsType, X> getter,
            BiConsumer<GetOperationOptionsType, X> setter, X value) {
        for (SelectorQualifiedGetOptionType qualifiedOptions : base.getOption()) {
            if (matches(qualifiedOptions, getter)) {
                setter.accept(qualifiedOptions.getOptions(), value);
                return;
            }
        }
        if (value != null) {
            GetOperationOptionsType newOptions = new GetOperationOptionsType();
            setter.accept(newOptions, value);
            base.getOption().add(
                    new SelectorQualifiedGetOptionType()
                            .options(newOptions));
        }
    }

    private static <X> boolean matches(SelectorQualifiedGetOptionType qualifiedOptions,
            Function<GetOperationOptionsType, X> getter) {
        return isEmpty(qualifiedOptions.getSelector()) &&
                qualifiedOptions.getOptions() != null &&
                getter.apply(qualifiedOptions.getOptions()) != null;
    }

    private static boolean isEmpty(OptionObjectSelectorType selector) {
        return selector == null || isEmpty(selector.getPath());
    }

    private static boolean isEmpty(ItemPathType itemPathType) {
        return itemPathType == null || itemPathType.getItemPath().isEmpty();
    }

    public static boolean hasRawOption(SelectorQualifiedGetOptionsType options) {
        List<SelectorOptions<GetOperationOptions>> selectorOptions = GetOperationOptionsUtil.optionsBeanToOptions(options);
        return GetOperationOptions.isRaw(selectorOptions);
    }
}
