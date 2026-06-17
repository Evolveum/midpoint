/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.functions;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Any function library that can be used in scripts. Includes:
 *
 * - built-in libraries like `basic`, `log`, `midpoint`, `report`,
 * - {@link LibraryFunctionExecutor} objects derived from {@link FunctionLibrary} that are a parsed form of {@link FunctionLibraryType}.
 *
 * Named "binding" because it binds the variable name with the library implementation.
 *
 * TODO terminology of function libraries: built-in vs standard? custom function libraries?
 *
 * @author semancik
 */
@SuppressWarnings("ClassCanBeRecord")
public class FunctionLibraryBinding {

    /** Name of the variable that will be used to access the library in scripts. */
    private final @NotNull String variableName;

    /** Implementation of the library - a Java object. */
    private final @NotNull Object implementation;

    /** Parsed form of the library. Applied only to custom libraries (FunctionLibraryType). */
    private final @Nullable FunctionLibrary parsedLibrary;

    public FunctionLibraryBinding(
            @NotNull String variableName,
            @NotNull Object implementation,
            @Nullable FunctionLibrary parsedLibrary) {
        this.variableName = variableName;
        this.implementation = implementation;
        this.parsedLibrary = parsedLibrary;
    }

    public FunctionLibraryBinding(
            @NotNull String variableName,
            @NotNull Object implementation) {
        this(variableName, implementation, null);
    }

    public @NotNull String getVariableName() {
        return variableName;
    }

    public @NotNull Object getImplementation() {
        return implementation;
    }

    public @Nullable FunctionLibrary getParsedLibrary() {
        return parsedLibrary;
    }

    @Override
    public String toString() {
        return "FunctionLibraryBinding{" +
                "variableName='" + variableName + '\'' +
                ", implementation=" + implementation +
                ", parsedLibrary=" + parsedLibrary +
                '}';
    }
}
