/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.model.common.expression.script.mel.value.PolyStringCelValue;

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOptions;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.internal.CelCodePointArray;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.NullValue;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.parser.Operator;
import dev.cel.runtime.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Polystring extensions for CEL compiler and runtime.
 *
 * Goal of this extension is for PolyString to behave almost exactly like string.
 *
 * Note: Implementation of many string functions in CelStringExtensions are private.
 * There is no easy or clean way to reach them.
 * Therefore, the code is copied into this class, adapter to polystrings.
 * It is not ideal, but it is the best option at the moment.
 *
 * @author Radovan Semancik
 * Base on CelStringExtensions
 */
public class CelPolyStringExtensions extends AbstractMidPointCelExtensions {

    private final CelOptions celOptions;
    private final BasicExpressionFunctions basicExpressionFunctions;

    public CelPolyStringExtensions(CelOptions celOptions, BasicExpressionFunctions basicExpressionFunctions) {
        super();
        this.celOptions = celOptions;
        this.basicExpressionFunctions = basicExpressionFunctions;
        initialize();
    }

    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

                // _==_(string,polystring)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                Operator.EQUALS.getFunction(),
                                CelOverloadDecl.newGlobalOverload(
                                        "string-equals-polystring",
                                        "Equality operator string = polystring",
                                        SimpleType.BOOL,
                                        SimpleType.STRING,
                                        PolyStringCelValue.CEL_TYPE)),
                        // The parameter has to be an Object instead of PolyStringCelValue.
                        // When we define this overload, it starts to get all kinds of "null equality" checks,
                        // not just those related to polystring.
                        CelFunctionBinding.from("string-equals-polystring", Object.class, Object.class,
                                CelPolyStringExtensions::stringEqualsPolyString)
                ),

                // _==_(polystring,string)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                Operator.EQUALS.getFunction(),
                                CelOverloadDecl.newGlobalOverload(
                                        "polystring-equals-string",
                                        "Equality operator polystring = string",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE,
                                        SimpleType.STRING)),
                        // The parameter has to be an Object instead of PolyStringCelValue.
                        // When we define this overload, it starts to get all kinds of "null equality" checks,
                        // not just those related to polystring.
                        CelFunctionBinding.from("polystring-equals-string", Object.class, Object.class,
                                (polystring, string) -> stringEqualsPolyString(string,polystring))
                ),

                // _+_
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                Operator.ADD.getFunction(),
                                CelOverloadDecl.newGlobalOverload(
                                        "string-add-polystring",
                                        "String concatenation of string and polystring",
                                        SimpleType.STRING,
                                        SimpleType.STRING,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from("string-add-polystring", String.class, PolyStringCelValue.class,
                                CelPolyStringExtensions::stringAddPolyString)
                ),

                // _+_
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                Operator.ADD.getFunction(),
                                CelOverloadDecl.newGlobalOverload(
                                        "polystring-add-string",
                                        "String concatenation of polystring and string",
                                        SimpleType.STRING,
                                        PolyStringCelValue.CEL_TYPE,
                                        SimpleType.STRING)),
                        CelFunctionBinding.from("polystring-add-string", PolyStringCelValue.class, String.class,
                                CelPolyStringExtensions::polystringAddString)
                ),

                // _+_
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                Operator.ADD.getFunction(),
                                CelOverloadDecl.newGlobalOverload(
                                        "polystring-add-polystring",
                                        "String concatenation of polystring and polystring",
                                        SimpleType.STRING,
                                        PolyStringCelValue.CEL_TYPE,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from("polystring-add-polystring", PolyStringCelValue.class, PolyStringCelValue.class,
                                CelPolyStringExtensions::polystringAddPolystring)
                ),

                // charAt
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "charAt",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_char_at_int",
                                        "Returns the character at the given position of polystring orig value."
                                                + " If the position is negative, or"
                                                + " greater than the length of the string, the function will produce an error.",
                                        SimpleType.STRING,
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.INT))),
                        CelFunctionBinding.from(
                                "polystring_char_at_int", PolyStringCelValue.class, Long.class,
                                CelPolyStringExtensions::charAt)),

                // contains
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "contains",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_contains",
                                        "Returns true if orig part of polystring contains specified string.",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE, SimpleType.STRING)),
                        CelFunctionBinding.from(
                                "polystring_contains", PolyStringCelValue.class, String.class,
                                (polystring, s) -> polystring.getOrig().contains(s))),

                // containsIgnoreCase
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                CelMelExtensions.FUNC_CONTAINS_IGNORE_CASE_NAME,
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_"+CelMelExtensions.FUNC_CONTAINS_IGNORE_CASE_NAME,
                                        "Returns true if string contains specified string without regard to case.",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE, SimpleType.STRING)),
                        CelFunctionBinding.from(
                                "polystring_"+CelMelExtensions.FUNC_CONTAINS_IGNORE_CASE_NAME, PolyStringCelValue.class, String.class,
                                (polystring, s) ->basicExpressionFunctions.containsIgnoreCase(polystring.getOrig(), s))
                ),

                // endsWith
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "endsWith",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_endswith",
                                        "Returns true if orig part of polystring ends with specified string.",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE, SimpleType.STRING)),
                        CelFunctionBinding.from(
                                "polystring_endswith", PolyStringCelValue.class, String.class,
                                (polystring, s) -> polystring.getOrig().endsWith(s))),

                // indexOf
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "indexOf",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_index_of_string",
                                        "Returns the integer index of the first occurrence of the search string of orig value of polystring."
                                                + " If the"
                                                + " search string is not found the function returns -1.",
                                        SimpleType.INT,
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.STRING)),
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_index_of_string_int",
                                        "Returns the integer index of the first occurrence of the search string of orig value of polystring from the"
                                                + " given offset. If the search string is not found the function returns"
                                                + " -1. If the substring is the empty string, the index where the search starts"
                                                + " is returned (zero or custom).",
                                        SimpleType.INT,
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.INT))),
                                CelFunctionBinding.from(
                                        "polystring_index_of_string", PolyStringCelValue.class, String.class, CelPolyStringExtensions::indexOf),
                                CelFunctionBinding.from(
                                        "polystring_index_of_string_int",
                                        ImmutableList.of(PolyStringCelValue.class, String.class, Long.class),
                                        CelPolyStringExtensions::indexOf)),

                // str.isBlank
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                CelMelExtensions.FUNC_IS_BLANK_NAME,
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_"+CelMelExtensions.FUNC_IS_BLANK_NAME,
                                        "Returns true if string is blank (has zero length or contains only white characters).",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(
                                "polystring_"+CelMelExtensions.FUNC_IS_BLANK_NAME, PolyStringCelValue.class,
                                CelPolyStringExtensions::polystringIsBlank)),

                // isBlank(string)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                CelMelExtensions.FUNC_IS_BLANK_NAME,
                                CelOverloadDecl.newGlobalOverload(
                                        CelMelExtensions.FUNC_IS_BLANK_NAME+"_polystring",
                                        "Returns true if string is blank (has zero length or contains only white characters) or it is null.",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(
                                CelMelExtensions.FUNC_IS_BLANK_NAME+"_polystring", PolyStringCelValue.class,
                                CelPolyStringExtensions::polystringIsBlank)),

                // str.isEmpty
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                CelMelExtensions.FUNC_IS_EMPTY_NAME,
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_"+CelMelExtensions.FUNC_IS_EMPTY_NAME,
                                        "Returns true if string is empty (has zero length).",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(
                                "polystring_"+CelMelExtensions.FUNC_IS_EMPTY_NAME, PolyStringCelValue.class,
                                CelPolyStringExtensions::polystringIsEmpty)),

                // isEmpty(string)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                CelMelExtensions.FUNC_IS_EMPTY_NAME,
                                CelOverloadDecl.newGlobalOverload(
                                        CelMelExtensions.FUNC_IS_EMPTY_NAME+"_polystring",
                                        "Returns true if string is empty (has zero length) or it is null.",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(
                                CelMelExtensions.FUNC_IS_EMPTY_NAME+"_polystring", PolyStringCelValue.class,
                                CelPolyStringExtensions::polystringIsEmpty)),


                // TODO: JOIN? Does it make sense?

                // lastIndexOf
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "lastIndexOf",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_last_index_of_string",
                                        "Returns the integer index of the last occurrence of the search string in orig part of polystring. "
                                                + "If the"
                                                + " search string is not found the function returns -1.",
                                        SimpleType.INT,
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.STRING)),
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_last_index_of_string_int",
                                        "Returns the integer index of the last occurrence of the search string in orig part of polystring from the"
                                                + " given offset. If the search string is not found the function returns -1. If"
                                                + " the substring is the empty string, the index where the search starts is"
                                                + " returned (string length or custom).",
                                        SimpleType.INT,
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.INT))),
                                CelFunctionBinding.from(
                                        "polystring_last_index_of_string",
                                        PolyStringCelValue.class,
                                        String.class,
                                        CelPolyStringExtensions::lastIndexOf),
                                CelFunctionBinding.from(
                                        "polystring_last_index_of_string_int",
                                        ImmutableList.of(PolyStringCelValue.class, String.class, Long.class),
                                        CelPolyStringExtensions::lastIndexOf)),

                // lowerAscii
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "lowerAscii",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_lower_ascii",
                                        "Returns a new string where all ASCII characters of orig represantation of polystring are lower-cased."
                                                + " This function does not perform Unicode case-mapping for characters outside the ASCII"
                                                + " range.",
                                        SimpleType.STRING,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from("polystring_lower_ascii", PolyStringCelValue.class,
                                polystring -> Ascii.toLowerCase(polystring.getOrig()))
                ),

                // matches
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "matches",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_matches",
                                        "Returns true if orig part of polystring matches the specified RE2 regular expression",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE, SimpleType.STRING),
                                CelOverloadDecl.newGlobalOverload(
                                        "polystring_matches",
                                        "Returns size of the orig part of polystring.",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE, SimpleType.STRING)),
                        CelFunctionBinding.from(
                                "polystring_matches", PolyStringCelValue.class, String.class,
                                (polystring, s) -> RuntimeHelpers.matches(polystring.getOrig(), s, celOptions))
                ),




                // replace
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "replace",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_replace_string_string",
                                        "Returns a new string based on the target, which replaces the occurrences of a"
                                                + " search string with a replacement string in orig represantation of polystring if present.",
                                        SimpleType.STRING,
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.STRING)),
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_replace_string_string_int",
                                        "Returns a new string based on the target, which replaces the occurrences of a"
                                                + " search string with a replacement string in orig represantation of polystring if present. The function accepts a"
                                                + " limit on the number of substring replacements to be made. When the"
                                                + " replacement limit is 0, the result is the original string. When the limit"
                                                + " is a negative number, the function behaves the same as replace all.",
                                        SimpleType.STRING,
                                        ImmutableList.of(
                                                PolyStringCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.STRING, SimpleType.INT))),
                                CelFunctionBinding.from(
                                        "polystring_replace_string_string",
                                        ImmutableList.of(PolyStringCelValue.class, String.class, String.class),
                                        CelPolyStringExtensions::replaceAll),
                                CelFunctionBinding.from(
                                        "polystring_replace_string_string_int",
                                        ImmutableList.of(PolyStringCelValue.class, String.class, String.class, Long.class),
                                        CelPolyStringExtensions::replace)),

                // size
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "size",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_size",
                                        "Returns size of the orig part of polystring.",
                                        SimpleType.INT,
                                        PolyStringCelValue.CEL_TYPE),
                                CelOverloadDecl.newGlobalOverload(
                                        "polystring_size",
                                        "Returns size of the orig part of polystring.",
                                        SimpleType.INT,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(
                                "polystring_size",
                                PolyStringCelValue.class,
                                polystring -> ((Integer)polystring.getOrig().length()).longValue())),

                // split
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "split",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_split_string",
                                        "Returns a mutable list of strings split from the orig part of input by the given separator.",
                                        ListType.create(SimpleType.STRING),
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.STRING)),
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_split_string_int",
                                        "Returns a mutable list of strings split from the orig part of input by the given separator with"
                                                + " the specified limit on the number of substrings produced by the split.",
                                        ListType.create(SimpleType.STRING),
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.INT))),
                                CelFunctionBinding.from(
                                        "polystring_split_string", PolyStringCelValue.class, String.class,
                                        CelPolyStringExtensions::split),
                                CelFunctionBinding.from(
                                        "polystring_split_string_int",
                                        ImmutableList.of(PolyStringCelValue.class, String.class, Long.class),
                                        CelPolyStringExtensions::split)),

                // startsWith
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "startsWith",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_startswith",
                                        "Returns true if orig part of polystring starts with specified string.",
                                        SimpleType.BOOL,
                                        PolyStringCelValue.CEL_TYPE, SimpleType.STRING)),
                        CelFunctionBinding.from(
                                "polystring_startswith", PolyStringCelValue.class, String.class,
                                (polystring, s) -> polystring.getOrig().startsWith(s))),

                // substring
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "substring",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_substring_int",
                                        "returns a string that is a substring of orig part of this polystring. The substring begins with the"
                                                + " character at the specified index and extends to the end of this string.",
                                        SimpleType.STRING,
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.INT)),
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_substring_int_int",
                                        "returns a string that is a substring of orig part of this polystring. The substring begins at the"
                                                + " specified beginIndex and extends to the character at index endIndex - 1."
                                                + " Thus the length of the substring is {@code endIndex-beginIndex}.",
                                        SimpleType.STRING,
                                        ImmutableList.of(PolyStringCelValue.CEL_TYPE, SimpleType.INT, SimpleType.INT))),
                                CelFunctionBinding.from(
                                        "polystring_substring_int", PolyStringCelValue.class, Long.class,
                                        CelPolyStringExtensions::substring),
                                CelFunctionBinding.from(
                                        "polystring_substring_int_int",
                                        ImmutableList.of(PolyStringCelValue.class, Long.class, Long.class),
                                        CelPolyStringExtensions::substring)),

                // trim
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "trim",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_trim",
                                        "Returns a new string which removes the leading and trailing whitespace in the"
                                                + " orig part of target polystring. The trim function uses the Unicode definition of whitespace"
                                                + " which does not include the zero-width spaces. ",
                                        SimpleType.STRING,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from("polystring_trim", PolyStringCelValue.class,
                                CelPolyStringExtensions::trim)),

                // upperAscii
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "upperAscii",
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_upper_ascii",
                                        "Returns a new string where all ASCII characters of orig represantation of polystring are upper-cased."
                                                + " This function does not perform Unicode case-mapping for characters outside the ASCII"
                                                + " range.",
                                        SimpleType.STRING,
                                        PolyStringCelValue.CEL_TYPE)),
                        CelFunctionBinding.from("polystring_upper_ascii", PolyStringCelValue.class,
                                polystring -> Ascii.toUpperCase(polystring.getOrig()))

                )
        );
    }

    private static final class Library implements CelExtensionLibrary<CelPolyStringExtensions> {
        private final CelPolyStringExtensions version0;

        private Library(CelOptions celOptions, BasicExpressionFunctions basicExpressionFunctions) {
            version0 = new CelPolyStringExtensions(celOptions, basicExpressionFunctions);
        }

        @Override
        public String name() {
            return "polystring";
        }

        @Override
        public ImmutableSet<CelPolyStringExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    public static CelExtensionLibrary<CelPolyStringExtensions> library(CelOptions celOptions, BasicExpressionFunctions basicExpressionFunctions) {
        return new Library(celOptions, basicExpressionFunctions);
    }

    @Override
    public int version() {
        return 0;
    }

    // The parameter has to be an Object instead of PolyStringCelValue.
    // When we define this overload, it starts to get all kinds of "null equality" checks,
    // not just those related to polystring.
    public static boolean stringEqualsPolyString(Object s1, Object s2) {
        if (CelTypeMapper.isCellNull(s1) && CelTypeMapper.isCellNull(s2)) {
            return true;
        }
        if (CelTypeMapper.isCellNull(s1) || CelTypeMapper.isCellNull(s2)) {
            return false;
        }
        if (s1 instanceof PolyStringCelValue x) {
            s1 = x.getOrig();
        }
        if (s2 instanceof PolyStringCelValue x) {
            s2 = x.getOrig();
        }
        return s1.equals(s2);
    }

    public static String stringAddPolyString(String s, PolyStringCelValue polystringValue) {
        if (s == null && polystringValue == null) {
            return null;
        }
        if (s == null) {
            return polystringValue.getOrig();
        }
        if (polystringValue == null) {
            return s;
        }
        return s + polystringValue.getOrig();
    }

    public static String polystringAddString(PolyStringCelValue polystringValue, String s) {
        if (s == null && polystringValue == null) {
            return null;
        }
        if (s == null) {
            return polystringValue.getOrig();
        }
        if (polystringValue == null) {
            return s;
        }
        return polystringValue.getOrig() + s;
    }

    public static String polystringAddPolystring(PolyStringCelValue polystringValue1, PolyStringCelValue polystringValue2) {
        if (polystringValue2 == null && polystringValue1 == null) {
            return null;
        }
        if (polystringValue2 == null) {
            return polystringValue1.getOrig();
        }
        if (polystringValue1 == null) {
            return polystringValue2.getOrig();
        }
        return polystringValue1.getOrig() + polystringValue2.getOrig();
    }

    private static boolean polystringIsBlank(PolyStringCelValue celPolystring) {
        if (isCellNull(celPolystring)) {
            return true;
        }
        return celPolystring.getOrig().isBlank();
    }

    private static boolean polystringIsEmpty(PolyStringCelValue celPolystring) {
        if (isCellNull(celPolystring)) {
            return true;
        }
        return celPolystring.getOrig().isEmpty();
    }


    // Taken from CelStringExtensions, modified for Polystring

    private static String charAt(PolyStringCelValue ps, long i) throws CelEvaluationException {
        int index;
        try {
            index = Math.toIntExact(i);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "charAt failure: Index must not exceed the int32 range: %d", i)
                    .setCause(e)
                    .build();
        }

        CelCodePointArray codePointArray = CelCodePointArray.fromString(ps.getOrig());
        if (index == codePointArray.length()) {
            return "";
        }
        if (index < 0 || index > codePointArray.length()) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "charAt failure: Index out of range: %d", index)
                    .build();
        }

        return codePointArray.slice(index, index + 1).toString();
    }

    private static Long indexOf(PolyStringCelValue pstr, String substr) throws CelEvaluationException {
        Object[] params = {pstr, substr, 0L};
        return indexOf(params);
    }

    /**
     * @param args Object array with indices of: [0: string], [1: substring], [2: offset]
     */
    private static Long indexOf(Object[] args) throws CelEvaluationException {
        PolyStringCelValue str = (PolyStringCelValue) args[0];
        String substr = (String) args[1];
        long offsetInLong = (Long) args[2];
        int offset;
        try {
            offset = Math.toIntExact(offsetInLong);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "indexOf failure: Offset must not exceed the int32 range: %d", offsetInLong)
                    .setCause(e)
                    .build();
        }

        return indexOf(str, substr, offset);
    }

    private static Long indexOf(PolyStringCelValue pstr, String substr, int offset) throws CelEvaluationException {
        if (substr.isEmpty()) {
            return (long) offset;
        }

        CelCodePointArray strCpa = CelCodePointArray.fromString(pstr.getOrig());
        CelCodePointArray substrCpa = CelCodePointArray.fromString(substr);

        if (offset < 0 || offset >= strCpa.length()) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "indexOf failure: Offset out of range: %d", offset)
                    .build();
        }

        return safeIndexOf(strCpa, substrCpa, offset);
    }


    /** Retrieves the index of the substring in a given string without throwing. */
    private static Long safeIndexOf(CelCodePointArray str, CelCodePointArray substr, int offset) {
        for (int i = offset; i < str.length() - (substr.length() - 1); i++) {
            int j;
            for (j = 0; j < substr.length(); j++) {
                if (str.get(i + j) != substr.get(j)) {
                    break;
                }
            }

            if (j == substr.length()) {
                return (long) i;
            }
        }

        // Offset is out of bound.
        return -1L;
    }


    private static Long lastIndexOf(PolyStringCelValue pstr, String substr) throws CelEvaluationException {
        CelCodePointArray strCpa = CelCodePointArray.fromString(pstr.getOrig());
        CelCodePointArray substrCpa = CelCodePointArray.fromString(substr);
        if (substrCpa.isEmpty()) {
            return (long) strCpa.length();
        }

        if (strCpa.length() < substrCpa.length()) {
            return -1L;
        }

        return lastIndexOf(strCpa, substrCpa, (long) strCpa.length() - 1);
    }

    private static Long lastIndexOf(Object[] args) throws CelEvaluationException {
        CelCodePointArray strCpa = CelCodePointArray.fromString(((PolyStringCelValue) args[0]).getOrig());
        CelCodePointArray substrCpa = CelCodePointArray.fromString((String) args[1]);
        long offset = (long) args[2];

        return lastIndexOf(strCpa, substrCpa, offset);
    }

    private static Long lastIndexOf(CelCodePointArray str, CelCodePointArray substr, long offset)
            throws CelEvaluationException {
        if (substr.isEmpty()) {
            return offset;
        }

        int off;
        try {
            off = Math.toIntExact(offset);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "lastIndexOf failure: Offset must not exceed the int32 range: %d", offset)
                    .setCause(e)
                    .build();
        }

        if (off < 0 || off >= str.length()) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "lastIndexOf failure: Offset out of range: %d", offset)
                    .build();
        }

        if (off > str.length() - substr.length()) {
            off = str.length() - substr.length();
        }

        for (int i = off; i >= 0; i--) {
            int j;
            for (j = 0; j < substr.length(); j++) {
                if (str.get(i + j) != substr.get(j)) {
                    break;
                }
            }

            if (j == substr.length()) {
                return (long) i;
            }
        }

        return -1L;
    }

    private static String replaceAll(Object[] objects) {
        return replace(((PolyStringCelValue) objects[0]).getOrig(), (String) objects[1], (String) objects[2], -1);
    }

    private static String replace(Object[] objects) throws CelEvaluationException {
        Long indexInLong = (Long) objects[3];
        int index;
        try {
            index = Math.toIntExact(indexInLong);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "replace failure: Index must not exceed the int32 range: %d", indexInLong)
                    .setCause(e)
                    .build();
        }

        return replace(((PolyStringCelValue) objects[0]).getOrig(), (String) objects[1], (String) objects[2], index);
    }

    private static String replace(String text, String searchString, String replacement, int limit) {
        if (searchString.equals(replacement) || limit == 0) {
            return text;
        }

        if (text.isEmpty()) {
            return searchString.isEmpty() ? replacement : "";
        }

        CelCodePointArray textCpa = CelCodePointArray.fromString(text);
        CelCodePointArray searchCpa = CelCodePointArray.fromString(searchString);
        CelCodePointArray replaceCpa = CelCodePointArray.fromString(replacement);

        int start = 0;
        int end = Math.toIntExact(safeIndexOf(textCpa, searchCpa, 0));
        if (end < 0) {
            return text;
        }

        // The minimum length of 1 handles the case of searchString being empty, where every character
        // would be matched. This ensures the window is always moved forward to continue the search.
        int minSearchLength = max(searchCpa.length(), 1);
        StringBuilder sb =
                new StringBuilder(textCpa.length() - searchCpa.length() + replaceCpa.length());

        do {
            CelCodePointArray sliced = textCpa.slice(start, end);
            sb.append(sliced).append(replaceCpa);
            start = end + searchCpa.length();
            limit--;
        } while (limit != 0
                && (end = Math.toIntExact(safeIndexOf(textCpa, searchCpa, end + minSearchLength))) > 0);

        return sb.append(textCpa.slice(start, textCpa.length())).toString();
    }

    private static List<String> split(PolyStringCelValue pstr, String separator) {
        return split(pstr, separator, Integer.MAX_VALUE);
    }

    /**
     * @param args Object array with indices of: [0: string], [1: separator], [2: limit]
     */
    private static List<String> split(Object[] args) throws CelEvaluationException {
        long limitInLong = (Long) args[2];
        int limit;
        try {
            limit = Math.toIntExact(limitInLong);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "split failure: Limit must not exceed the int32 range: %d", limitInLong)
                    .setCause(e)
                    .build();
        }

        return split((PolyStringCelValue) args[0], (String) args[1], limit);
    }

    /** Returns a **mutable** list of strings split on the separator */
    private static List<String> split(PolyStringCelValue pstr, String separator, int limit) {
        if (limit == 0) {
            return new ArrayList<>();
        }

        String str = pstr.getOrig();

        if (limit == 1) {
            List<String> singleElementList = new ArrayList<>();
            singleElementList.add(str);
            return singleElementList;
        }

        if (limit < 0) {
            limit = str.length();
        }

        if (separator.isEmpty()) {
            return explode(str, limit);
        }

        Iterable<String> splitString = Splitter.on(separator).limit(limit).split(str);
        return Lists.newArrayList(splitString);
    }

    private static List<String> explode(String str, int limit) {
        List<String> exploded = new ArrayList<>();
        CelCodePointArray codePointArray = CelCodePointArray.fromString(str);
        if (limit > 0) {
            limit -= 1;
        }
        int charCount = min(codePointArray.length(), limit);
        for (int i = 0; i < charCount; i++) {
            exploded.add(codePointArray.slice(i, i + 1).toString());
        }
        if (codePointArray.length() > limit) {
            exploded.add(codePointArray.slice(limit, codePointArray.length()).toString());
        }
        return exploded;
    }

    private static Object substring(PolyStringCelValue ps, long i) throws CelEvaluationException {
        int beginIndex;
        try {
            beginIndex = Math.toIntExact(i);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "substring failure: Index must not exceed the int32 range: %d", i)
                    .setCause(e)
                    .build();
        }

        CelCodePointArray codePointArray = CelCodePointArray.fromString(ps.getOrig());

        boolean indexIsInRange = beginIndex <= codePointArray.length() && beginIndex >= 0;
        if (!indexIsInRange) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "substring failure: Range [%d, %d) out of bounds",
                            beginIndex, codePointArray.length())
                    .build();
        }

        if (beginIndex == codePointArray.length()) {
            return "";
        }

        return codePointArray.slice(beginIndex, codePointArray.length()).toString();
    }

    /**
     * @param args Object array with indices of [0: string], [1: beginIndex], [2: endIndex]
     */
    private static String substring(Object[] args) throws CelEvaluationException {
        Long beginIndexInLong = (Long) args[1];
        Long endIndexInLong = (Long) args[2];
        int beginIndex;
        int endIndex;
        try {
            beginIndex = Math.toIntExact(beginIndexInLong);
            endIndex = Math.toIntExact(endIndexInLong);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "substring failure: Indices must not exceed the int32 range: [%d, %d)",
                            beginIndexInLong, endIndexInLong)
                    .setCause(e)
                    .build();
        }

        String s = ((PolyStringCelValue) args[0]).getOrig();
        CelCodePointArray codePointArray = CelCodePointArray.fromString(s);

        boolean indicesIsInRange =
                beginIndex <= endIndex
                        && beginIndex >= 0
                        && beginIndex <= codePointArray.length()
                        && endIndex <= codePointArray.length();
        if (!indicesIsInRange) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "substring failure: Range [%d, %d) out of bounds", beginIndex, endIndex)
                    .build();
        }

        if (beginIndex == endIndex) {
            return "";
        }

        return codePointArray.slice(beginIndex, endIndex).toString();
    }

    private static String trim(PolyStringCelValue text) {
        CelCodePointArray textCpa = CelCodePointArray.fromString(text.getOrig());
        int left = indexOfNonWhitespace(textCpa);
        if (left == textCpa.length()) {
            return "";
        }
        int right = lastIndexOfNonWhitespace(textCpa);
        return textCpa.slice(left, right + 1).toString();
    }


    /**
     * Finds the first index of the non-whitespace character found in the string. See {@link
     * #isWhitespace} for definition of a whitespace char.
     *
     * @return index of first non-whitespace character found (ex: " test " -> 0). Length of the string
     *     is returned instead if a non-whitespace character is not found.
     */
    private static int indexOfNonWhitespace(CelCodePointArray textCpa) {
        for (int i = 0; i < textCpa.length(); i++) {
            if (!isWhitespace(textCpa.get(i))) {
                return i;
            }
        }
        return textCpa.length();
    }

    /**
     * Finds the last index of the non-whitespace character found in the string. See {@link
     * #isWhitespace} for definition of a whitespace char.
     *
     * @return index of last non-whitespace character found. (ex: " test " -> 5). 0 is returned
     *     instead if a non-whitespace char is not found. -1 is returned for an empty string ("").
     */
    private static int lastIndexOfNonWhitespace(CelCodePointArray textCpa) {
        if (textCpa.isEmpty()) {
            return -1;
        }

        for (int i = textCpa.length() - 1; i >= 0; i--) {
            if (!isWhitespace(textCpa.get(i))) {
                return i;
            }
        }

        return 0;
    }

    /**
     * Checks if a provided codepoint is a whitespace according to Unicode's standard
     * (White_Space=yes).
     *
     * <p>This exists because Java's native Character.isWhitespace does not follow the Unicode's
     * standard of whitespace definition.
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Whitespace_character">link<a> for the full list.
     */
    private static boolean isWhitespace(int codePoint) {
        return (codePoint >= 0x0009 && codePoint <= 0x000D)
                || codePoint == 0x0020
                || codePoint == 0x0085
                || codePoint == 0x00A0
                || codePoint == 0x1680
                || (codePoint >= 0x2000 && codePoint <= 0x200A)
                || codePoint == 0x2028
                || codePoint == 0x2029
                || codePoint == 0x202F
                || codePoint == 0x205F
                || codePoint == 0x3000;
    }

}
