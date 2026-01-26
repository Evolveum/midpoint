/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.extension;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.util.Set;

import com.evolveum.midpoint.model.common.expression.script.cel.value.ObjectCelValue;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.collect.ImmutableSet;
import dev.cel.checker.CelCheckerBuilder;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.NullValue;
import dev.cel.compiler.CelCompilerLibrary;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.*;

/**
 * Extensions for CEL compiler and runtime implementing behavior of Prism
 * objects, properties and other items and value.
 *
 * @author Radovan Semancik
 */
public class CelPrismItemsExtensions
        implements CelCompilerLibrary, CelRuntimeLibrary, CelExtensionLibrary.FeatureSet{

    public static final Trace LOGGER = TraceManager.getTrace(CelPrismItemsExtensions.class);

    public enum Function {

//        PRISM_INDEX_STRING(
//                CelFunctionDecl.newFunctionDeclaration(
//                        Operator.INDEX.getFunction(),
////                        "index_map",
//                        CelOverloadDecl.newGlobalOverload(
//                                "prism-index-string",
//                                "TODO",
//                                SimpleType.ANY,
//                                ObjectCelValue.CEL_TYPE,
//                                SimpleType.STRING)),
//                CelFunctionBinding.from("prism-index-string", ObjectCelValue.class, String.class,
//                        CelPrismItemsExtensions::prismIndexString)),

                PRISM_OBJECT_FIND(
                        CelFunctionDecl.newFunctionDeclaration(
                                "find",
                                CelOverloadDecl.newMemberOverload(
                                        "prism-object-find-string",
                                        "TODO",
                                        SimpleType.ANY,
                                        ObjectCelValue.CEL_TYPE,
                                        SimpleType.STRING)),
                        CelFunctionBinding.from("prism-object-find-string", ObjectCelValue.class, String.class,
                                CelPrismItemsExtensions::prismIndexString)
        );

        private final CelFunctionDecl functionDecl;
        private final ImmutableSet<CelFunctionBinding> commonFunctionBindings;

        String getFunction() {
            return functionDecl.name();
        }

        Function(CelFunctionDecl functionDecl, CelFunctionBinding... commonFunctionBindings) {
            this.functionDecl = functionDecl;
            this.commonFunctionBindings = ImmutableSet.copyOf(commonFunctionBindings);
        }
    };

    private final ImmutableSet<CelPrismItemsExtensions.Function> functions;

    public CelPrismItemsExtensions() {
        this(ImmutableSet.copyOf(CelPrismItemsExtensions.Function.values()));
    }

    CelPrismItemsExtensions(Set<CelPrismItemsExtensions.Function> functions) {
        this.functions = ImmutableSet.copyOf(functions);
    }

    private static final class Library implements CelExtensionLibrary<CelPrismItemsExtensions> {
        private final CelPrismItemsExtensions version0;

        private Library() {
            version0 = new CelPrismItemsExtensions();
        }

        @Override
        public String name() {
            return "prism";
        }

        @Override
        public ImmutableSet<CelPrismItemsExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    private static final Library LIBRARY = new Library();

    public static CelExtensionLibrary<CelPrismItemsExtensions> library() {
        return LIBRARY;
    }

    @Override
    public int version() {
        return 0;
    }

    @Override
    public ImmutableSet<CelFunctionDecl> functions() {
        return functions.stream().map(f -> f.functionDecl).collect(toImmutableSet());
    }

    @Override
    public void setCheckerOptions(CelCheckerBuilder checkerBuilder) {
        functions.forEach(function -> checkerBuilder.addFunctionDeclarations(function.functionDecl));
    }

    @Override
    public void setRuntimeOptions(CelRuntimeBuilder runtimeBuilder) {
        functions.forEach(function -> {
            runtimeBuilder.addFunctionBindings(function.commonFunctionBindings);
        });
    }

    public static Object prismIndexString(ObjectCelValue<?> objectCelValue, String stringPath) {
//        LOGGER.info("EEEEEEEX prismIndexString({},{})", objectCelValue, stringPath);
        Object o = objectCelValue.getObject().find(PrismContext.get().itemPathParser().asItemPath(stringPath));
//        LOGGER.info("EEEEEEEY prismIndexString({},{}): {}", objectCelValue, stringPath, o);
        if (o == null) {
            return NullValue.NULL_VALUE;
        }
        return o;
    }

}
