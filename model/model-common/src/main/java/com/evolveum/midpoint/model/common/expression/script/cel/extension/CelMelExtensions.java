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
import com.evolveum.midpoint.util.exception.SchemaException;
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

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Extensions for CEL compiler and runtime implementing behavior of "MEL" language.
 *
 * @author Radovan Semancik
 */
public class CelMelExtensions extends AbstractMidPointCelExtensions {

    private static final Trace LOGGER = TraceManager.getTrace(CelMelExtensions.class);


    private final BasicExpressionFunctions basicExpressionFunctions;

    public CelMelExtensions(BasicExpressionFunctions basicExpressionFunctions) {
        this.basicExpressionFunctions = basicExpressionFunctions;
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
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

                // str.isBlank
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNC_IS_BLANK_NAME,
                                CelOverloadDecl.newMemberOverload(
                                        "string_"+FUNC_IS_BLANK_NAME,
                                        "Returns true if string is blank (has zero length or contains only white characters).",
                                        SimpleType.BOOL,
                                        SimpleType.STRING)),
                        CelFunctionBinding.from(
                                "string_"+FUNC_IS_BLANK_NAME, String.class,
                                CelMelExtensions::stringIsBlank)),

                // isBlank(string)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNC_IS_BLANK_NAME,
                                CelOverloadDecl.newGlobalOverload(
                                        FUNC_IS_BLANK_NAME+"_string",
                                        "Returns true if string is blank (has zero length or contains only white characters) or it is null.",
                                        SimpleType.BOOL,
                                        SimpleType.STRING)),
                        CelFunctionBinding.from(
                                FUNC_IS_BLANK_NAME+"_string", String.class,
                                CelMelExtensions::stringIsBlank)),

                // str.isEmpty
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNC_IS_EMPTY_NAME,
                                CelOverloadDecl.newMemberOverload(
                                        "string_"+FUNC_IS_EMPTY_NAME,
                                        "Returns true if string is empty (has zero length).",
                                        SimpleType.BOOL,
                                        SimpleType.STRING)),
                        CelFunctionBinding.from(
                                "string_"+FUNC_IS_EMPTY_NAME, String.class,
                                CelMelExtensions::stringIsEmpty)),

                // isEmpty(string)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNC_IS_EMPTY_NAME,
                                CelOverloadDecl.newGlobalOverload(
                                        FUNC_IS_EMPTY_NAME+"_string",
                                        "Returns true if string is empty (has zero length) or it is null.",
                                        SimpleType.BOOL,
                                        SimpleType.STRING)),
                        CelFunctionBinding.from(
                                FUNC_IS_EMPTY_NAME+"_string", String.class,
                                CelMelExtensions::stringIsEmpty)),

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

            // single
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "single",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-single",
                                    "TODO",
                                    SimpleType.ANY,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mel-single", Object.class,
                            this::single)

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

    private String ascii(Object o) {
        return basicExpressionFunctions.ascii(toJava(o));
    }

    public static boolean stringIsEmpty(String str) {
        if (isCellNull(str)) {
            return true;
        }
        return str.isEmpty();
    }

    public static boolean stringIsBlank(String str) {
        if (isCellNull(str)) {
            return true;
        }
        return str.isBlank();
    }

    // TODO: do we need this?
    public static List<Object> melList(Object input) {
        LOGGER.info("MMMMMMMMMMMM: melList({})", input);
        if (input instanceof List) {
            return (List)input;
        } else {
            return ImmutableList.of(input);
        }
    }

    private Object single(Object o) {
        if (isCellNull(o)) {
            return o;
        }
        if (o instanceof Collection<?> col) {
            if (col.isEmpty()) {
                return NullValue.NULL_VALUE;
            } else if (col.size() == 1) {
                return col.iterator().next();
            } else {
                throw new RuntimeException("Attempt to get single value from a multi-valued property");
            }
        } else {
            return o;
        }
    }

    private String stringify(Object o) {
        return basicExpressionFunctions.stringify(toJava(o));
    }

    private String norm(Object o) {
        if (isCellNull(o)) {
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

}
