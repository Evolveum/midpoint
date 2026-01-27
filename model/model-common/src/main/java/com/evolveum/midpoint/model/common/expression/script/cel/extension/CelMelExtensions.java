/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.cel.value.MidPointCelValue;
import com.evolveum.midpoint.model.common.expression.script.cel.value.PolyStringCelValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.cel.checker.CelCheckerBuilder;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.NullValue;
import dev.cel.compiler.CelCompilerLibrary;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelRuntimeBuilder;
import dev.cel.runtime.CelRuntimeLibrary;

import java.util.List;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Extensions for CEL compiler and runtime implementing behavior of "MEL" language.
 *
 * @author Radovan Semancik
 */
public class CelMelExtensions
        implements CelCompilerLibrary, CelRuntimeLibrary, CelExtensionLibrary.FeatureSet {

    public static final String FUNC_CONTAINS_IGNORE_CASE_NAME = "containsIgnoreCase";

    private static final Trace LOGGER = TraceManager.getTrace(CelMelExtensions.class);

    public class Function {

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

    private final BasicExpressionFunctions basicExpressionFunctions;
    private final ImmutableSet<CelMelExtensions.Function> functions;

    public CelMelExtensions(BasicExpressionFunctions basicExpressionFunctions) {
        this.basicExpressionFunctions = basicExpressionFunctions;
        functions = initializeFunctions();
    }

    private ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

            // ascii
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "ascii",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-ascii",
                                    "TODO",
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mel-ascii", Object.class,
                            this::ascii)

            ),

            // containsIgnoreCase
            new Function(
                CelFunctionDecl.newFunctionDeclaration(
                        FUNC_CONTAINS_IGNORE_CASE_NAME,
                        CelOverloadDecl.newMemberOverload(
                                "string_"+FUNC_CONTAINS_IGNORE_CASE_NAME,
                                "Returns true if string contains specified string without regard to case.",
                                SimpleType.BOOL,
                                SimpleType.STRING, SimpleType.STRING)),
                CelFunctionBinding.from(
                        "string_"+FUNC_CONTAINS_IGNORE_CASE_NAME, String.class, String.class,
                        basicExpressionFunctions::containsIgnoreCase)),

            // list
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "list",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-list",
                                    "TODO",
                                    ListType.create(SimpleType.DYN),
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mel-list", Object.class,
                            CelMelExtensions::melList)

            ),

            // norm
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "norm",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-norm",
                                    "TODO",
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mel-norm", Object.class,
                            this::norm)

            ),

            // stringify
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "stringify",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-stringify",
                                    "TODO",
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mel-stringify", Object.class,
                            this::stringify)

            )


                // TODO: toSingle()? single()? scalar()?
        );
    }

    private static final class Library implements CelExtensionLibrary<CelMelExtensions> {
        private final CelMelExtensions version0;

        private Library(BasicExpressionFunctions basicExpressionFunctions) {
            version0 = new CelMelExtensions(basicExpressionFunctions);
        }

        @Override
        public String name() {
            return "mel";
        }

        @Override
        public ImmutableSet<CelMelExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    public static CelExtensionLibrary<CelMelExtensions> library(BasicExpressionFunctions basicExpressionFunctions) {
        return new Library(basicExpressionFunctions);
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

    private String ascii(Object o) {
        return basicExpressionFunctions.ascii(toJava(o));
    }

    public static List<Object> melList(Object input) {
        LOGGER.info("MMMMMMMMMMMM: melList({})", input);
        if (input instanceof List) {
            return (List)input;
        } else {
            return ImmutableList.of(input);
        }
    }

    private String stringify(Object o) {
        return basicExpressionFunctions.stringify(toJava(o));
    }

    private String norm(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof NullValue) {
            return "";
        }
        if (o instanceof MidPointCelValue<?> mpCelVal) {
            o = mpCelVal.getJavaValue();
        }
        if (o instanceof String s) {
            return basicExpressionFunctions.norm(s);
        }
        if (o instanceof PolyString ps) {
            return basicExpressionFunctions.norm(ps);
        }
        return basicExpressionFunctions.norm(stringify(o));
    }

    private Object toJava(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof NullValue) {
            return null;
        }
        if (o instanceof MidPointCelValue<?> mpCelVal) {
            return mpCelVal.getJavaValue();
        }
        return o;
    }

}
