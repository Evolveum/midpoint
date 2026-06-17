/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;

/**
 * Extensions for CEL compiler and runtime implementing dealing with secret providers.
 *
 * @author Radovan Semancik
 */
public class CelSecretExtensions extends AbstractMidPointCelExtensions {

    private static final Trace LOGGER = TraceManager.getTrace(CelSecretExtensions.class);


    private final BasicExpressionFunctions basicExpressionFunctions;
    private final Protector protector;

    public CelSecretExtensions(Protector protector, BasicExpressionFunctions basicExpressionFunctions) {
        this.protector = protector;
        this.basicExpressionFunctions = basicExpressionFunctions;
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

            // secret.resolveBinary(provider, key)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "secret.resolveBinary",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-secret-resolveBinary",
                                    "Resolves a secret specified by the key, using a provider specified by its name. "
                                            + "Returns the secret in binary form (bytes).",
                                    SimpleType.BYTES,
                                    SimpleType.STRING, SimpleType.STRING)),
                    CelFunctionBinding.from("mel-secret-resolveBinary", String.class, String.class,
                            this::secretResolveBinary)

            ),

            // secret.resolveString(provider, key)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "secret.resolveString",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-secret-resolveString",
                                    "Resolves a secret specified by the key, using a provider specified by its name. "
                                            + "Returns the secret in string form.",
                                    SimpleType.STRING,
                                    SimpleType.STRING, SimpleType.STRING)),
                    CelFunctionBinding.from("mel-secret-resolveString", String.class, String.class,
                            this::secretResolveString)

            ),

            // secret.resolveProtectedString(provider, key)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "secret.resolveProtectedString",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-secret-resolveProtectedString",
                                    "Resolves a secret specified by the key, using a provider specified by its name. "
                                            + "Returns the secret in protected string form.",
                                    CelTypeMapper.PROTECTED_STRING_CEL_TYPE,
                                    SimpleType.STRING, SimpleType.STRING)),
                    CelFunctionBinding.from("mel-secret-resolveProtectedString", String.class, String.class,
                            this::secretResolveProtectedString)

            )

        );
    }

    private static final class Library implements CelExtensionLibrary<CelSecretExtensions> {
        private final CelSecretExtensions version0;

        private Library(Protector protector, BasicExpressionFunctions basicExpressionFunctions) {
            version0 = new CelSecretExtensions(protector, basicExpressionFunctions);
        }

        @Override
        public String name() {
            return "secret";
        }

        @Override
        public ImmutableSet<CelSecretExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    public static CelExtensionLibrary<CelSecretExtensions> library(Protector protector, BasicExpressionFunctions basicExpressionFunctions) {
        return new Library(protector, basicExpressionFunctions);
    }

    @Override
    public int version() {
        return 0;
    }

    private byte[] secretResolveBinary(String providerName, String key) {
        return basicExpressionFunctions.resolveSecretBinary(providerName, key).array();
    }

    private String secretResolveString(String providerName, String key) {
        return basicExpressionFunctions.resolveSecretString(providerName, key);
    }

    private ProtectedStringType secretResolveProtectedString(String providerName, String key) {
        return basicExpressionFunctions.resolveSecretProtectedString(providerName, key);
    }
}
