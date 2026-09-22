/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;

import com.evolveum.midpoint.model.common.expression.script.mel.extension.AbstractMidPointCelExtensions;

/**
 * Extensions for testing the configurable declarations of user-provided CEL/MEL extension libraries.
 */
public class CelTestingExtensions extends AbstractMidPointCelExtensions {

    public CelTestingExtensions() {
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

            // testing.hello(param)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "testing.hello",
                            CelOverloadDecl.newGlobalOverload(
                                    "testing-hello",
                                    "Simple 'hello world' function for testing.",
                                    SimpleType.STRING,
                                    SimpleType.STRING)),
                    CelFunctionBinding.from(
                            "testing-hello", String.class, string -> "Hello, " + string + "!")));
    }

    public static final class Library implements CelExtensionLibrary<CelTestingExtensions> {
        private final CelTestingExtensions version0;

        public Library() {
            version0 = new CelTestingExtensions();
        }

        @Override
        public String name() {
            return "testing";
        }

        @Override
        public ImmutableSet<CelTestingExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    public static CelExtensionLibrary<CelTestingExtensions> library() {
        return new Library();
    }

    @Override
    public int version() {
        return 0;
    }
}
