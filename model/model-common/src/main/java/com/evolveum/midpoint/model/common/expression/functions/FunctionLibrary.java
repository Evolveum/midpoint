/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.functions;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.Freezable;
import com.evolveum.midpoint.prism.PrismObject;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.FunctionConfigItem;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.util.MiscUtil.argNonNull;

/**
 * A "parsed form" of a {@link FunctionLibraryType}.
 *
 * Used to provide common functionality, e.g. when looking for functions in the library.
 * Improves the performance by using e.g. map for functions declarations (more will probably come in the future).
 */
public class FunctionLibrary {

    private static final Trace LOGGER = TraceManager.getTrace(FunctionLibrary.class);

    @NotNull private final String oid;

    /** The "raw" definition of the library. Immutable. */
    @NotNull private final FunctionLibraryType libraryBean;

    /** An optimization: functions indexed by name. */
    @NotNull private final Multimap<String, FunctionConfigItem> functionsByName = ArrayListMultimap.create();

    private FunctionLibrary(
            @NotNull String oid,
            @NotNull FunctionLibraryType libraryBean) throws ConfigurationException {
        this.oid = oid;
        this.libraryBean = libraryBean;
        for (ExpressionType functionBean : libraryBean.getFunction()) {
            // We cannot use "embedded" factory method here, because ExpressionType is not a Containerable - only a property.
            // This also limits the diagnostic possibilities, as we have no precise item path (with PCV ID) for it.
            var functionConfigItem = FunctionConfigItem.of(
                    functionBean,
                    ConfigurationItemOrigin.inObject(libraryBean, FunctionLibraryType.F_FUNCTION));
            functionsByName.put(functionConfigItem.getName(), functionConfigItem);
        }
    }

    /**
     * Creates the library object from bean. Throws {@link ConfigurationException} for the most obvious problems.
     * (No detailed checks, though.)
     *
     * Library must have an OID.
     */
    public static FunctionLibrary of(@NotNull FunctionLibraryType bean) throws ConfigurationException {
        return new FunctionLibrary(
                argNonNull(bean.getOid(), "Function Library has no OID %s", bean),
                Freezable.doFreeze(bean));
    }

    public @NotNull String getName() {
        PolyStringType name = Objects.requireNonNull(libraryBean.getName(), "no name");
        return Objects.requireNonNull(name.getOrig(), "no orig in library name");
    }

    /**
     * Finds a function by name.
     *
     * In the case of ambiguity (and only in that case), checks also the argument vs parameter names - they must match exactly
     * and uniquely. Parameter types are ignored.
     */
    @NotNull FunctionConfigItem findFunction(
            @NotNull String functionName, @NotNull Collection<String> argumentNames, @NotNull String contextDesc)
            throws ConfigurationException {

        Collection<FunctionConfigItem> withMatchingName = functionsByName.get(functionName);
        if (withMatchingName.isEmpty()) {
            throw new ConfigurationException(
                    "No function with name '%s' found in %s. Function defined are: %s. In %s".formatted(
                            functionName, libraryBean, functionsByName.keySet(), contextDesc));
        }

        if (withMatchingName.size() == 1) {
            return withMatchingName.iterator().next(); // Not checking parameters here.
        }

        List<FunctionConfigItem> withMatchingNameAndParams = withMatchingName.stream()
                .filter(consideredFunction -> consideredFunction.doesMatchArguments(argumentNames))
                .toList();

        if (withMatchingNameAndParams.size() == 1) {
            return withMatchingNameAndParams.get(0);
        }

        if (withMatchingNameAndParams.isEmpty()) {
            throw new ConfigurationException(
                    ("No matching function with name '%s' found in %s. "
                            + "%d functions with this name found but none matches the parameters %s. In %s").formatted(
                            functionName, libraryBean, functionsByName.size(), argumentNames, contextDesc));
        } else {
            throw new ConfigurationException(
                    ("Ambiguous function invocation: %d matching functions with name '%s' and parameters %s found in %s.")
                            .formatted(withMatchingNameAndParams.size(), functionName, argumentNames, contextDesc));
        }
    }

    FunctionLibraryBinding createBinding(ExpressionFactory expressionFactory) {
        return new FunctionLibraryBinding(
                getName(),
                new LibraryFunctionExecutor(this, expressionFactory));
    }

    @SuppressWarnings("WeakerAccess")
    public @NotNull PrismObject<FunctionLibraryType> getLibraryObject() {
        return libraryBean.asPrismObject();
    }

    public @NotNull String getOid() {
        return oid;
    }
}
