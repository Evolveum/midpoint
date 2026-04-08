/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.model.common.expression.script.mel.value.AbstractContainerValueCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.MidPointValueProducer;
import com.evolveum.midpoint.model.common.expression.script.mel.value.PolyStringCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.QNameCelValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOptions;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.internal.CelCodePointArray;
import dev.cel.common.types.*;
import dev.cel.common.values.NullValue;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.common.Operator;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelEvaluationExceptionBuilder;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.RuntimeHelpers;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.*;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Extensions for CEL compiler and runtime implementing behavior of "MEL" language.
 *
 * @author Radovan Semancik
 */
public class CelMelExtensions extends AbstractMidPointCelExtensions {

    private static final Trace LOGGER = TraceManager.getTrace(CelMelExtensions.class);

    private final CelOptions celOptions;
    private final BasicExpressionFunctions basicExpressionFunctions;
    private final Protector protector;

    public CelMelExtensions(CelOptions celOptions, Protector protector, BasicExpressionFunctions basicExpressionFunctions) {
        this.celOptions = celOptions;
        this.protector = protector;
        this.basicExpressionFunctions = basicExpressionFunctions;
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        final TypeParamType paramTypeV = TypeParamType.create("V");
        final OptionalType optionalTypeV = OptionalType.create(paramTypeV);

        return ImmutableSet.of(

            // string == polystring
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            Operator.EQUALS.getFunction(),
                            CelOverloadDecl.newGlobalOverload(
                                    "string-equals-polystring",
                                    "Equality operator string = polystring",
                                    SimpleType.BOOL,
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from("string-equals-polystring", String.class, PolyStringCelValue.class,
                            CelMelExtensions::stringEqualsPolyString)
            ),

            // polystring == string
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            Operator.EQUALS.getFunction(),
                            CelOverloadDecl.newGlobalOverload(
                                    "polystring-equals-string",
                                    "Equality operator polystring = string",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE),
                                    SimpleType.STRING)),
                    CelFunctionBinding.from("polystring-equals-string", PolyStringCelValue.class, String.class,
                            (polystring, string) -> stringEqualsPolyString(string,polystring))
            ),

            // string + polystring
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            Operator.ADD.getFunction(),
                            CelOverloadDecl.newGlobalOverload(
                                    "string-add-polystring",
                                    "String concatenation of string and polystring",
                                    SimpleType.STRING,
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from("string-add-polystring", String.class, PolyStringCelValue.class,
                            CelMelExtensions::stringAddPolyString)
            ),

            // polystring + string
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            Operator.ADD.getFunction(),
                            CelOverloadDecl.newGlobalOverload(
                                    "polystring-add-string",
                                    "String concatenation of polystring and string",
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE),
                                    SimpleType.STRING)),
                    CelFunctionBinding.from("polystring-add-string", PolyStringCelValue.class, String.class,
                            CelMelExtensions::polystringAddString)
            ),

            // polystring + polystring
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            Operator.ADD.getFunction(),
                            CelOverloadDecl.newGlobalOverload(
                                    "polystring-add-polystring",
                                    "String concatenation of polystring and polystring",
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE),
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from("polystring-add-polystring", PolyStringCelValue.class, PolyStringCelValue.class,
                            CelMelExtensions::polystringAddPolystring)
            ),


            // string + int
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            Operator.ADD.getFunction(),
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-string-plus-int",
                                    "Concatenation of int into string.",
                                    SimpleType.STRING,
                                    SimpleType.STRING, SimpleType.INT)),
                    CelFunctionBinding.from("mel-string-plus-int", String.class, Long.class,
                            CelMelExtensions::stringPlusInt)

            ),

            // polystring + int
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            Operator.ADD.getFunction(),
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-polystring-plus-int",
                                    "Concatenation of int into polystring.",
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.INT)),
                    CelFunctionBinding.from("mel-polystring-plus-int", PolyStringCelValue.class, Long.class,
                            this::polystringPlusInt)

            ),

            // ascii
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "ascii",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-ascii",
                                    "Converts the argument to plain ASCII (ASCII7), all non-ASCII characters and all "
                                            + "non-printable characters are removed."
                                            + "ASCII characters with diacritical marks are converted to their"
                                            + " basic variants (removing the marks).",
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mel-ascii", Object.class,
                            this::ascii)

            ),

            // string.charAt()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "charAt",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_char_at_int",
                                    "Returns the character at the given position of polystring orig value."
                                            + " If the position is negative, or"
                                            + " greater than the length of the string, the function will produce an error.",
                                    SimpleType.STRING,
                                    ImmutableList.of(SimpleType.STRING, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "mel_string_char_at_int", String.class, Long.class,
                            CelMelExtensions::charAt)),

            // polystring.charAt()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "charAt",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_char_at_int",
                                    "Returns the character at the given position of polystring orig value."
                                            + " If the position is negative, or"
                                            + " greater than the length of the string, the function will produce an error.",
                                    SimpleType.STRING,
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.INT))),
                    CelFunctionBinding.from(
                            "polystring_char_at_int", PolyStringCelValue.class, Long.class,
                            CelMelExtensions::charAt)),

            // string.contains(substring) is a CEL built-in function

            // polystring.contains(string)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "contains",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_contains",
                                    "Returns true if orig part of polystring contains specified string.",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING)),
                    CelFunctionBinding.from(
                            "polystring_contains", PolyStringCelValue.class, String.class,
                            (polystring, s) -> polystring.getOrig().contains(s))),

            // string.containsIgnoreCase(substring)
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

            // polystring.containsIgnoreCase(substring)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            CelMelExtensions.FUNC_CONTAINS_IGNORE_CASE_NAME,
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_"+CelMelExtensions.FUNC_CONTAINS_IGNORE_CASE_NAME,
                                    "Returns true if string contains specified string without regard to case.",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING)),
                    CelFunctionBinding.from(
                            "polystring_"+CelMelExtensions.FUNC_CONTAINS_IGNORE_CASE_NAME, PolyStringCelValue.class, String.class,
                            (polystring, s) ->basicExpressionFunctions.containsIgnoreCase(polystring.getOrig(), s))
            ),

            // default(x, defaultVal)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "default",
                            CelOverloadDecl.newGlobalOverload(
                                    "default",
                                    "Returns true if is null (includes processing of optionals).",
                                    paramTypeV,
                                    SimpleType.DYN, paramTypeV)),
                    CelFunctionBinding.from(
                            "default", Object.class, Object.class,
                            CelMelExtensions::funcDefault)),

            // protectedString.decrypt()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            FUNC_DECRYPT_NAME,
                            CelOverloadDecl.newMemberOverload(
                                    "protectedstring_"+FUNC_DECRYPT_NAME,
                                    "Decrypts value of protected string, returning cleartext value as string.",
                                    SimpleType.STRING,
                                    CelTypeMapper.PROTECTED_STRING_CEL_TYPE)),
                    CelFunctionBinding.from(
                            "protectedstring_"+FUNC_DECRYPT_NAME, ProtectedStringType.class, this::decrypt)),

            // str.encrypt()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            FUNC_ENCRYPT_NAME,
                            CelOverloadDecl.newMemberOverload(
                                    "string_"+FUNC_ENCRYPT_NAME,
                                    "Encrypts a string value, creating protected string.",
                                    CelTypeMapper.PROTECTED_STRING_CEL_TYPE,
                                    SimpleType.STRING)),
                    CelFunctionBinding.from(
                            "string_"+FUNC_ENCRYPT_NAME, String.class, this::encrypt)),

            // polystring.encrypt()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            FUNC_ENCRYPT_NAME,
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_"+FUNC_ENCRYPT_NAME,
                                    "Encrypts a string value, creating protected string.",
                                    CelTypeMapper.PROTECTED_STRING_CEL_TYPE,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from(
                            "polystring_"+FUNC_ENCRYPT_NAME, PolyStringCelValue.class, this::encrypt)),

            // TODO: bytes.encrypt()? Should we encrypt to ProtectedString or ProtectedData?

            // string.endsWith(substring) is a CEL built-in function

            // polystring.endsWith(substring)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "endsWith",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_endswith",
                                    "Returns true if orig part of polystring ends with specified string.",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING)),
                    CelFunctionBinding.from(
                            "polystring_endswith", PolyStringCelValue.class, String.class,
                            (polystring, s) -> polystring.getOrig().endsWith(s))),


            // str.format([args])
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "format",
                            CelOverloadDecl.newMemberOverload(
                                    "pm-string-format",
                                    "Format strings according to specified template, filling in data from the arguments."
                                            + " Follow Java formatting conventions.",
                                    SimpleType.STRING,
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from(
                            "pm-string-format", String.class, Object.class,
                            CelMelExtensions::stringFormat)),

            // string.indexOf(substring [, offset])
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "indexOf",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_index_of_string",
                                    "Returns the integer index of the first occurrence of the search string."
                                            + " If the search string is not found the function returns -1.",
                                    SimpleType.INT,
                                    ImmutableList.of(NullableType.create(SimpleType.STRING), SimpleType.STRING)),
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_index_of_string_int",
                                    "Returns the integer index of the first occurrence of the search string from the"
                                            + " given offset."
                                            + "If the search string is not found the function returns -1. "
                                            + "If the substring is the empty string, the index where the search starts"
                                            + " is returned (zero or custom).",
                                    SimpleType.INT,
                                    ImmutableList.of(NullableType.create(SimpleType.STRING), SimpleType.STRING, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "mel_string_index_of_string", String.class, String.class, CelMelExtensions::indexOf),
                    CelFunctionBinding.from(
                            "mel_string_index_of_string_int",
                            ImmutableList.of(String.class, String.class, Long.class),
                            CelMelExtensions::indexOfString)),

            // polystring.indexOf(substring [, offset])
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "indexOf",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_index_of_string",
                                    "Returns the integer index of the first occurrence of the search string of orig value of polystring."
                                            + " If the search string is not found the function returns -1.",
                                    SimpleType.INT,
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING)),
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_index_of_string_int",
                                    "Returns the integer index of the first occurrence of the search string of orig value of polystring from the"
                                            + " given offset."
                                            + "If the search string is not found the function returns -1. "
                                            + "If the substring is the empty string, the index where the search starts"
                                            + " is returned (zero or custom).",
                                    SimpleType.INT,
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "polystring_index_of_string", PolyStringCelValue.class, String.class, CelMelExtensions::indexOf),
                    CelFunctionBinding.from(
                            "polystring_index_of_string_int",
                            ImmutableList.of(PolyStringCelValue.class, String.class, Long.class),
                            CelMelExtensions::indexOfPolystring)),


            // string.isBlank()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            FUNC_IS_BLANK_NAME,
                            CelOverloadDecl.newMemberOverload(
                                    "string_"+FUNC_IS_BLANK_NAME,
                                    "Returns true if string is blank (has zero length or contains only white characters).",
                                    SimpleType.BOOL,
                                    NullableType.create(SimpleType.STRING))),
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
                                    NullableType.create(SimpleType.STRING))),
                    CelFunctionBinding.from(
                            FUNC_IS_BLANK_NAME+"_string", String.class,
                            CelMelExtensions::stringIsBlank)),


            // polystring.isBlank
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            CelMelExtensions.FUNC_IS_BLANK_NAME,
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_"+CelMelExtensions.FUNC_IS_BLANK_NAME,
                                    "Returns true if string is blank (has zero length or contains only white characters).",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from(
                            "polystring_"+CelMelExtensions.FUNC_IS_BLANK_NAME, PolyStringCelValue.class,
                            CelMelExtensions::polystringIsBlank)),

            // isBlank(polystring)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            CelMelExtensions.FUNC_IS_BLANK_NAME,
                            CelOverloadDecl.newGlobalOverload(
                                    CelMelExtensions.FUNC_IS_BLANK_NAME+"_polystring",
                                    "Returns true if string is blank (has zero length or contains only white characters) or it is null.",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from(
                            CelMelExtensions.FUNC_IS_BLANK_NAME+"_polystring", PolyStringCelValue.class,
                            CelMelExtensions::polystringIsBlank)),

            // polystring.isEmpty()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            CelMelExtensions.FUNC_IS_EMPTY_NAME,
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_"+CelMelExtensions.FUNC_IS_EMPTY_NAME,
                                    "Returns true if string is empty (has zero length).",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from(
                            "polystring_"+CelMelExtensions.FUNC_IS_EMPTY_NAME, PolyStringCelValue.class,
                            CelMelExtensions::polystringIsEmpty)),

            // string.isEmpty()
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

            // isEmpty(any)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            FUNC_IS_EMPTY_NAME,
                            CelOverloadDecl.newGlobalOverload(
                                    FUNC_IS_EMPTY_NAME+"_string",
                                    "Returns true if argument is empty or null. " +
                                            "In case of string, return true if string is empty (has zero length). " +
                                            "I case of list, returns true if list is empty.",
                                    SimpleType.BOOL,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from(
                            FUNC_IS_EMPTY_NAME+"_string", Object.class,
                            CelMelExtensions::isEmpty)),

            // isNull(any)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "isNull",
                            CelOverloadDecl.newGlobalOverload(
                                    "isNull_any",
                                    "Returns true if argument is null (includes processing of optionals).",
                                    SimpleType.BOOL,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from(
                            "isNull_any", Object.class,
                            CelMelExtensions::isNull)),

            // isPresent(any)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "isPresent",
                            CelOverloadDecl.newGlobalOverload(
                                    "isPresent_any",
                                    "Returns true if argument is present, i.e. not null (includes processing of optionals).",
                                    SimpleType.BOOL,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from(
                            "isPresent_any", Object.class,
                            CelMelExtensions::isPresent)),

            // TODO: JOIN? Does it make sense? -> join(list(any))

            // string.lastIndexOf(substring [, offset])
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "lastIndexOf",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_last_index_of_string",
                                    "Returns the integer index of the last occurrence of the search string. "
                                            + "If the search string is not found the function returns -1.",
                                    SimpleType.INT,
                                    ImmutableList.of(NullableType.create(SimpleType.STRING), SimpleType.STRING)),
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_last_index_of_string_int",
                                    "Returns the integer index of the last occurrence of the search string from the"
                                            + " given offset. "
                                            + "If the search string is not found the function returns -1. "
                                            + "If the substring is the empty string, the index where the search starts is"
                                            + " returned (string length or custom).",
                                    SimpleType.INT,
                                    ImmutableList.of(NullableType.create(SimpleType.STRING), SimpleType.STRING, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "mel_string_last_index_of_string",
                            String.class,
                            String.class,
                            CelMelExtensions::lastIndexOf),
                    CelFunctionBinding.from(
                            "mel_string_last_index_of_string_int",
                            ImmutableList.of(String.class, String.class, Long.class),
                            CelMelExtensions::lastIndexOfString)),

            // polystring.lastIndexOf(substring [, offset])
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "lastIndexOf",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_last_index_of_string",
                                    "Returns the integer index of the last occurrence of the search string in orig part of polystring. "
                                            + "If the"
                                            + " search string is not found the function returns -1.",
                                    SimpleType.INT,
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING)),
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_last_index_of_string_int",
                                    "Returns the integer index of the last occurrence of the search string in orig part of polystring from the"
                                            + " given offset. If the search string is not found the function returns -1. If"
                                            + " the substring is the empty string, the index where the search starts is"
                                            + " returned (string length or custom).",
                                    SimpleType.INT,
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "polystring_last_index_of_string",
                            PolyStringCelValue.class,
                            String.class,
                            CelMelExtensions::lastIndexOf),
                    CelFunctionBinding.from(
                            "polystring_last_index_of_string_int",
                            ImmutableList.of(PolyStringCelValue.class, String.class, Long.class),
                            CelMelExtensions::lastIndexOfPolystring)),

            // string.lc()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "lc",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_lc",
                                    "Returns a new string where all characters of string are lower-cased.",
                                    SimpleType.STRING,
                                    NullableType.create(SimpleType.STRING))),
                    CelFunctionBinding.from("mel_string_lc", String.class,
                            CelMelExtensions::lc)
            ),

            // polystring.lc()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "lc",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_lc",
                                    "Returns a new string where all characters of orig represantation of polystring are lower-cased.",
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from("polystring_lc", PolyStringCelValue.class,
                            CelMelExtensions::lc)
            ),

            // list
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "list",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-list",
                                    "Returns list composed of specified argument.",
                                    ListType.create(SimpleType.DYN),
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mel-list", Object.class,
                            CelMelExtensions::melList)

            ),

            // string.lowerAscii()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "lowerAscii",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_lower_ascii",
                                    "Returns a new string where all ASCII characters of string are lower-cased."
                                            + " This function does not perform Unicode case-mapping for characters outside the ASCII"
                                            + " range.",
                                    SimpleType.STRING,
                                    NullableType.create(SimpleType.STRING))),
                    CelFunctionBinding.from("mel_string_lower_ascii", String.class, Ascii::toLowerCase)
            ),

            // polystring.lowerAscii()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "lowerAscii",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_lower_ascii",
                                    "Returns a new string where all ASCII characters of orig represantation of polystring are lower-cased."
                                            + " This function does not perform Unicode case-mapping for characters outside the ASCII"
                                            + " range.",
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from("polystring_lower_ascii", PolyStringCelValue.class,
                            polystring -> Ascii.toLowerCase(polystring.getOrig()))
            ),


            // polysting.matches(regex)
            // matches(polysting, regex)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "matches",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_matches",
                                    "Returns true if orig part of polystring matches the specified RE2 regular expression",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING),
                            CelOverloadDecl.newGlobalOverload(
                                    "polystring_matches",
                                    "Returns size of the orig part of polystring.",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING)),
                    CelFunctionBinding.from(
                            "polystring_matches", PolyStringCelValue.class, String.class,
                            (polystring, s) -> RuntimeHelpers.matches(polystring.getOrig(), s, celOptions))
            ),


            // norm(any)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "norm",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-norm",
                                    "Returns string in a normalized form.",
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mel-norm", Object.class,
                            this::norm)
            ),

            // qname(local)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "qname",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-qname-local",
                                    "Creates a qname value with null namespace.",
                                    QNameCelValue.CEL_TYPE,
                                    SimpleType.STRING)),
                    CelFunctionBinding.from("mel-qname-local", String.class,
                            this::qname)

            ),

            // qname(ns, local)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "qname",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-qname-ns",
                                    "Creates a qname value.",
                                    QNameCelValue.CEL_TYPE,
                                    SimpleType.STRING, SimpleType.STRING)),
                    CelFunctionBinding.from("mel-qname-ns", String.class, String.class,
                            this::qname)

            ),

            // string.replace(searchString, replacement [, limit])
            // replace(string, searchString, replacement [, limit])
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "replace",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_replace_string_string",
                                    "Returns a new string based on the target, which replaces the occurrences of a"
                                            + " search string with a replacement string in orig represantation of polystring if present.",
                                    SimpleType.STRING,
                                    ImmutableList.of(NullableType.create(SimpleType.STRING), SimpleType.STRING, SimpleType.STRING)),
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_replace_string_string_int",
                                    "Returns a new string based on the target, which replaces the occurrences of a"
                                            + " search string with a replacement string in orig represantation of polystring if present. The function accepts a"
                                            + " limit on the number of substring replacements to be made. When the"
                                            + " replacement limit is 0, the result is the original string. When the limit"
                                            + " is a negative number, the function behaves the same as replace all.",
                                    SimpleType.STRING,
                                    ImmutableList.of(
                                            NullableType.create(SimpleType.STRING), SimpleType.STRING, SimpleType.STRING, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "mel_string_replace_string_string",
                            ImmutableList.of(String.class, String.class, String.class),
                            CelMelExtensions::replaceAllString),
                    CelFunctionBinding.from(
                            "mel_string_replace_string_string_int",
                            ImmutableList.of(String.class, String.class, String.class, Long.class),
                            CelMelExtensions::replaceString)),

            // polystring.replace(searchString, replacement [, limit])
            // replace(polystring, searchString, replacement [, limit])
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "replace",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_replace_string_string",
                                    "Returns a new string based on the target, which replaces the occurrences of a"
                                            + " search string with a replacement string in orig represantation of polystring if present.",
                                    SimpleType.STRING,
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING, SimpleType.STRING)),
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_replace_string_string_int",
                                    "Returns a new string based on the target, which replaces the occurrences of a"
                                            + " search string with a replacement string in orig represantation of polystring if present. The function accepts a"
                                            + " limit on the number of substring replacements to be made. When the"
                                            + " replacement limit is 0, the result is the original string. When the limit"
                                            + " is a negative number, the function behaves the same as replace all.",
                                    SimpleType.STRING,
                                    ImmutableList.of(
                                            NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING, SimpleType.STRING, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "polystring_replace_string_string",
                            ImmutableList.of(PolyStringCelValue.class, String.class, String.class),
                            CelMelExtensions::replaceAllPolystring),
                    CelFunctionBinding.from(
                            "polystring_replace_string_string_int",
                            ImmutableList.of(PolyStringCelValue.class, String.class, String.class, Long.class),
                            CelMelExtensions::replacePolystring)),

            // single(any)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "single",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-single",
                                    "TODO",
                                    SimpleType.DYN,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mel-single", Object.class,
                            this::single)

            ),

            // size(polystring)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "size",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_size",
                                    "Returns size of the orig part of polystring.",
                                    SimpleType.INT,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE)),
                            CelOverloadDecl.newGlobalOverload(
                                    "polystring_size",
                                    "Returns size of the orig part of polystring.",
                                    SimpleType.INT,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from(
                            "polystring_size",
                            PolyStringCelValue.class,
                            polystring -> ((Integer)polystring.getOrig().length()).longValue())),

            // string.split(separator [, limit])
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "split",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_split_string",
                                    "Returns a mutable list of strings split from the orig part of input by the given separator.",
                                    ListType.create(SimpleType.STRING),
                                    ImmutableList.of(NullableType.create(SimpleType.STRING), SimpleType.STRING)),
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_split_string_int",
                                    "Returns a mutable list of strings split from the orig part of input by the given separator with"
                                            + " the specified limit on the number of substrings produced by the split.",
                                    ListType.create(SimpleType.STRING),
                                    ImmutableList.of(NullableType.create(SimpleType.STRING), SimpleType.STRING, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "mel_string_split_string", String.class, String.class,
                            CelMelExtensions::splitString),
                    CelFunctionBinding.from(
                            "mel_string_split_string_int",
                            ImmutableList.of(String.class, String.class, Long.class),
                            CelMelExtensions::splitString)),

            // polystring.split(separator [, limit])
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "split",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_split_string",
                                    "Returns a mutable list of strings split from the orig part of input by the given separator.",
                                    ListType.create(SimpleType.STRING),
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING)),
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_split_string_int",
                                    "Returns a mutable list of strings split from the orig part of input by the given separator with"
                                            + " the specified limit on the number of substrings produced by the split.",
                                    ListType.create(SimpleType.STRING),
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "polystring_split_string", PolyStringCelValue.class, String.class,
                            CelMelExtensions::splitPolystring),
                    CelFunctionBinding.from(
                            "polystring_split_string_int",
                            ImmutableList.of(PolyStringCelValue.class, String.class, Long.class),
                            CelMelExtensions::splitPolystring)),

            // polystring.startsWith(substring)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "startsWith",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_startswith",
                                    "Returns true if orig part of polystring starts with specified string.",
                                    SimpleType.BOOL,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.STRING)),
                    CelFunctionBinding.from(
                            "polystring_startswith", PolyStringCelValue.class, String.class,
                            (polystring, s) -> polystring.getOrig().startsWith(s))),

            // str(any)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "str",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-str",
                                    "Converts its argument to string. " +
                                            "This function is nullable. If the argument is null, null is returned.",
                                    SimpleType.STRING,
                                    NullableType.create(SimpleType.ANY))),
                    CelFunctionBinding.from(
                            "mel-str", Object.class,
                            CelMelExtensions::string)),

            // string(polystring)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "string",
                            CelOverloadDecl.newGlobalOverload(
                                    "string-polystring",
                                    "Converts its argument to string. " +
                                            "This function is not nullable, it cannot be called with null or optional value.",
                                    SimpleType.STRING,
                                    PolyStringCelValue.CEL_TYPE)),
                    CelFunctionBinding.from(
                            "string-polystring", PolyStringCelValue.class,
                            CelMelExtensions::string)),

            // stringify(any)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "stringify",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-stringify",
                                    "Converts its argument to string. " +
                                            "Always return non-null string. If the argument is null, empty string is returned.",
                                    SimpleType.STRING,
                                    NullableType.create(SimpleType.ANY))),
                    CelFunctionBinding.from(
                            "mel-stringify", Object.class,
                            arg -> stringify(arg, ""))),

            // stringify(any, nullValue)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "stringify",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-stringify-default",
                                    "Converts its argument to string. " +
                                            "Always return non-null string. If the argument is null, string provided as second argument is returned.",
                                    SimpleType.STRING,
                                    NullableType.create(SimpleType.ANY), SimpleType.STRING)),
                    CelFunctionBinding.from(
                            "mel-stringify-default", Object.class, String.class,
                            CelMelExtensions::stringify)),


            // string.substring(begin,end)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "substring",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_substring_int",
                                    "returns a string that is a substring of this string. The substring begins with the"
                                            + " character at the specified index and extends to the end of this string.",
                                    SimpleType.STRING,
                                    ImmutableList.of(NullableType.create(SimpleType.STRING), SimpleType.INT)),
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_substring_int_int",
                                    "returns a string that is a substring of this string. The substring begins at the"
                                            + " specified beginIndex and extends to the character at index endIndex - 1."
                                            + " Thus the length of the substring is {@code endIndex-beginIndex}.",
                                    SimpleType.STRING,
                                    ImmutableList.of(NullableType.create(SimpleType.STRING), SimpleType.INT, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "mel_string_substring_int", String.class, Long.class,
                            CelMelExtensions::substring),
                    CelFunctionBinding.from(
                            "mel_string_substring_int_int",
                            ImmutableList.of(String.class, Long.class, Long.class),
                            CelMelExtensions::substringString)),

            // polystring.substring(begin,end)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "substring",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_substring_int",
                                    "returns a string that is a substring of orig part of this polystring. The substring begins with the"
                                            + " character at the specified index and extends to the end of this string.",
                                    SimpleType.STRING,
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.INT)),
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_substring_int_int",
                                    "returns a string that is a substring of orig part of this polystring. The substring begins at the"
                                            + " specified beginIndex and extends to the character at index endIndex - 1."
                                            + " Thus the length of the substring is {@code endIndex-beginIndex}.",
                                    SimpleType.STRING,
                                    ImmutableList.of(NullableType.create(PolyStringCelValue.CEL_TYPE), SimpleType.INT, SimpleType.INT))),
                    CelFunctionBinding.from(
                            "polystring_substring_int", PolyStringCelValue.class, Long.class,
                            CelMelExtensions::substring),
                    CelFunctionBinding.from(
                            "polystring_substring_int_int",
                            ImmutableList.of(PolyStringCelValue.class, Long.class, Long.class),
                            CelMelExtensions::substringPolystring)),


            // timestamp.atStartOfDay
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "atStartOfDay",
                            CelOverloadDecl.newMemberOverload(
                                    "timestamp_atStartOfDay",
                                    "Modifies the timestamp to refer to the start of the day.",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.TIMESTAMP)),
                    CelFunctionBinding.from("timestamp_atStartOfDay",
                            Instant.class, this::atStartOfDay)
            ),

            // timestamp.atStartOfDay(timezone)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "atStartOfDay",
                            CelOverloadDecl.newMemberOverload(
                                    "timestamp_atStartOfDay_string",
                                    "Modifies the timestamp to refer to the start of the day.",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.TIMESTAMP, SimpleType.STRING)),
                    CelFunctionBinding.from("timestamp_atStartOfDay_string",
                            Instant.class, String.class, this::atStartOfDay)
            ),

            // timestamp.atEndOfDay
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "atEndOfDay",
                            CelOverloadDecl.newMemberOverload(
                                    "timestamp_atEndOfDay",
                                    "Modifies the timestamp to refer to the end of the day.",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.TIMESTAMP)),
                    CelFunctionBinding.from("timestamp_atEndOfDay",
                            Instant.class, this::atEndOfDay)
            ),

            // timestamp.atEndOfDay(timezone)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "atEndOfDay",
                            CelOverloadDecl.newMemberOverload(
                                    "timestamp_atEndOfDay_string",
                                    "Modifies the timestamp to refer to the end of the day.",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.TIMESTAMP, SimpleType.STRING)),
                    CelFunctionBinding.from("timestamp_atEndOfDay_string",
                            Instant.class, String.class, this::atEndOfDay)
            ),

            // timestamp.longAgo()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "timestamp.longAgo",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-timestamp_longAgo",
                                    "Returns timestamp that is referring to a time long, long ago, too far in the past.",
                                    SimpleType.TIMESTAMP)),
                    CelFunctionBinding.from("mel-timestamp_longAgo", ImmutableList.of(),
                            CelMelExtensions::longAgo)
            ),

            // string.trim()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "trim",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_trim",
                                    "Returns a new string which removes the leading and trailing whitespace in the"
                                            + " target string. The trim function uses the Unicode definition of whitespace"
                                            + " which does not include the zero-width spaces. ",
                                    SimpleType.STRING,
                                    NullableType.create(SimpleType.STRING))),
                    CelFunctionBinding.from("mel_string_trim", String.class,
                            CelMelExtensions::trim)),

            // polystring.trim()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "trim",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_trim",
                                    "Returns a new string which removes the leading and trailing whitespace in the"
                                            + " orig part of target polystring. The trim function uses the Unicode definition of whitespace"
                                            + " which does not include the zero-width spaces. ",
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from("polystring_trim", PolyStringCelValue.class,
                            CelMelExtensions::trim)),

            // string.uc()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "uc",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_uc",
                                    "Returns a new string where all characters of string are upper-cased.",
                                    SimpleType.STRING,
                                    NullableType.create(SimpleType.STRING))),
                    CelFunctionBinding.from("mel_string_uc", String.class,
                            CelMelExtensions::uc)
            ),

            // polystring.lc()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "uc",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_uc",
                                    "Returns a new string where all characters of orig represantation of polystring are upper-cased.",
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from("polystring_uc", PolyStringCelValue.class,
                            CelMelExtensions::uc)
            ),

            // string.upperAscii()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "upperAscii",
                            CelOverloadDecl.newMemberOverload(
                                    "mel_string_upper_ascii",
                                    "Returns a new string where all ASCII characters of string are upper-cased."
                                            + " This function does not perform Unicode case-mapping for characters outside the ASCII"
                                            + " range.",
                                    SimpleType.STRING,
                                    NullableType.create(SimpleType.STRING))),
                    CelFunctionBinding.from("mel_string_upper_ascii", String.class,
                            Ascii::toUpperCase)),

            // polystring.upperAscii()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "upperAscii",
                            CelOverloadDecl.newMemberOverload(
                                    "polystring_upper_ascii",
                                    "Returns a new string where all ASCII characters of orig represantation of polystring are upper-cased."
                                            + " This function does not perform Unicode case-mapping for characters outside the ASCII"
                                            + " range.",
                                    SimpleType.STRING,
                                    NullableType.create(PolyStringCelValue.CEL_TYPE))),
                    CelFunctionBinding.from("polystring_upper_ascii", PolyStringCelValue.class,
                            polystring -> Ascii.toUpperCase(polystring.getOrig())))

        );
    }

    private static String uc(String s) {
        return StringUtils.upperCase(s);
    }

    private static String uc(PolyStringCelValue ps) {
        return StringUtils.upperCase(ps.getOrig());
    }

    private static String lc(String s) {
        return StringUtils.lowerCase(s);
    }

    private static String lc(PolyStringCelValue ps) {
        return StringUtils.lowerCase(ps.getOrig());
    }

    private static Object funcDefault(Object val, Object defaultVal) {
        if (CelTypeMapper.isCellNull(val)) {
            return defaultVal;
        }
        if (val instanceof Optional<?> opt && opt.isEmpty()) {
            return defaultVal;
        }
        if (val instanceof Collection<?> col && col.isEmpty()) {
            return defaultVal;
        }
        if (!(val instanceof String) && defaultVal instanceof String) {
            // This is a hackish workaround for a common use case.
            // Situation: the default value is string, therefore this method is supposed to return string,
            // however, the val argument is non-string.
            // E.g. if we returned the polystring val as is, CEL runtime is going to fail
            // as it will be looking for string overloads, but with polystring value.
            // Therefore, let's proactively convert the value to string.
            return ExpressionUtil.stringify(toJava(val), "null");
        }
        return val;
    }

    private static String stringPlusInt(String s, Long aLong) {
        return s + aLong.toString();
    }

    private static final class Library implements CelExtensionLibrary<CelMelExtensions> {
        private final CelMelExtensions version0;

        private Library(CelOptions celOptions, Protector protector, BasicExpressionFunctions basicExpressionFunctions) {
            version0 = new CelMelExtensions(celOptions, protector, basicExpressionFunctions);
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

    public static CelExtensionLibrary<CelMelExtensions> library(CelOptions celOptions, Protector protector, BasicExpressionFunctions basicExpressionFunctions) {
        return new Library(celOptions, protector, basicExpressionFunctions);
    }

    @Override
    public int version() {
        return 0;
    }

    private String ascii(Object o) {
        return basicExpressionFunctions.ascii(toJava(o));
    }

    private static Object stringFormat(String format, Object o) {
        Object[] javaArgs;
        if (CelTypeMapper.isCellNull(o)) {
            javaArgs = new Object[]{ null };
        } else if (o instanceof List<?> l) {
            javaArgs = CelTypeMapper.toJavaValues(l.toArray());
        } else {
            javaArgs = new Object[]{ CelTypeMapper.toJavaValue(o) };
        }
        return String.format(format, javaArgs);
    }

    public static boolean isPresent(Object o) {
        return !isNull(o);
    }

    public static boolean isNull(Object o) {
        if (isCellNull(o)) {
            return true;
        }
        return o instanceof Optional<?> opt && opt.isEmpty();
    }

    private QNameCelValue qname(String namespace, String localPart) {
        return QNameCelValue.create(namespace, localPart);
    }

    private QNameCelValue qname(String localPart) {
        return QNameCelValue.create(localPart);
    }

    private static Instant longAgo(Object[] args) {
        return Instant.ofEpochSecond(0);
    }

    private Instant atStartOfDay(Instant instant) {
        return atStartOfDay(instant, ZoneId.systemDefault());
    }

    private Instant atStartOfDay(Instant instant, String timezone) {
        return atStartOfDay(instant, ZoneId.of(timezone));
    }

    private Instant atStartOfDay(Instant instant, ZoneId zoneId) {
        ZonedDateTime midnightZdt = LocalDate.ofInstant(instant, zoneId).atStartOfDay(zoneId);
        return midnightZdt.toInstant();
    }

    private Instant atEndOfDay(Instant instant) {
        return atEndOfDay(instant, ZoneId.systemDefault());
    }

    private Instant atEndOfDay(Instant instant, String timezone) {
        return atEndOfDay(instant, ZoneId.of(timezone));
    }

    private Instant atEndOfDay(Instant instant, ZoneId zoneId) {
        ZonedDateTime eodZdt = LocalDate.ofInstant(instant, zoneId).atTime(LocalTime.MAX).atZone(zoneId);
        return eodZdt.toInstant();
    }

    // TODO: do we need this?
    public static List<Object> melList(Object input) {
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
        if (o instanceof Optional<?> opt) {
            if (opt.isEmpty()) {
                return o;
            }
            o = opt.get();
        }
        if (o instanceof Collection<?> col) {
            if (col.isEmpty()) {
                return NullValue.NULL_VALUE;
            } else if (col.size() == 1) {
                return col.iterator().next();
            } else {
                throw createException("Attempt to get single value from a multi-valued property");
            }
        } else {
            return o;
        }
    }

    @NotNull
    private static String stringify(Object arg, String nullRepresentation) {
        return ExpressionUtil.stringify(CelTypeMapper.toJavaValue(arg), nullRepresentation);
    }

    private static Object string(Object arg) {
        if (CelTypeMapper.isCellNull(arg)) {
            return NullValue.NULL_VALUE;
        }
        return ExpressionUtil.stringify(CelTypeMapper.toJavaValue(arg), "");
    }

    private String norm(Object o) {
        if (isCellNull(o)) {
            return "";
        }
        if (o instanceof MidPointValueProducer<?> mpCelVal) {
            o = mpCelVal.getJavaValue();
        }
        if (o instanceof String s) {
            return basicExpressionFunctions.norm(s);
        }
        if (o instanceof PolyString ps) {
            return basicExpressionFunctions.norm(ps);
        }
        return basicExpressionFunctions.norm(stringify(o, ""));
    }

    @Nullable
    public ProtectedStringType encrypt(@Nullable String str) {
        if (str == null) {
            return null;
        }
        try {
            return protector.encryptString(str);
        } catch (EncryptionException e) {
            throw createException(e);
        }
    }

    @Nullable
    public ProtectedStringType encrypt(@Nullable PolyStringCelValue pstr) {
        if (pstr == null) {
            return null;
        }
        try {
            return protector.encryptString(pstr.getOrig());
        } catch (EncryptionException e) {
            throw createException(e);
        }
    }

    @Nullable
    public String decrypt(@Nullable ProtectedStringType protectedString) {
        if (protectedString == null) {
            return null;
        }
        try {
            return protector.decryptString(protectedString);
        } catch (EncryptionException e) {
            throw createException(e);
        }
    }

    public static boolean stringEqualsPolyString(String s1, PolyStringCelValue s2) {
        if (CelTypeMapper.isCellNull(s1) && CelTypeMapper.isCellNull(s2)) {
            return true;
        }
        if (CelTypeMapper.isCellNull(s1) || CelTypeMapper.isCellNull(s2)) {
            return false;
        }
        return s1.equals(s2.getOrig());
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

    private Object polystringPlusInt(PolyStringCelValue polyStringCelValue, Long aLong) {
        return polyStringCelValue.getOrig() + aLong.toString();
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

    @NotNull
    private static String charAt(@NotNull PolyStringCelValue ps, long i) throws CelEvaluationException {
        return charAt(ps.getOrig(), i);
    }

    @NotNull
    private static String charAt(@NotNull String s, long i) throws CelEvaluationException {
        int index;
        try {
            index = Math.toIntExact(i);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "charAt failure: Index must not exceed the int32 range: %d", i)
                    .setCause(e)
                    .build();
        }

        CelCodePointArray codePointArray = CelCodePointArray.fromString(s);
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

    private static Long indexOfString(Object[] args) throws CelEvaluationException {
        return indexOf((String) args[0], (String) args[1], (Long) args[2]);
    }

    private static Long indexOfPolystring(Object[] args) throws CelEvaluationException {
        return indexOf((PolyStringCelValue) args[0], (String) args[1], (Long) args[2]);
    }

    private static Long indexOf(PolyStringCelValue psubstr, String substr, long offset) throws CelEvaluationException {
        return indexOf(psubstr.getOrig(), substr, offset);
    }

    private static Long indexOf(PolyStringCelValue psubstr, String substr) throws CelEvaluationException {
        return indexOf(psubstr.getOrig(), substr, 0L);
    }

    private static Long indexOf(String s, String substr) throws CelEvaluationException {
        return indexOf(s, substr, 0L);
    }

    private static Long indexOf(String s, String substr, long offsetLong) throws CelEvaluationException {
        if (substr.isEmpty()) {
            return offsetLong;
        }

        int offset;
        try {
            offset = Math.toIntExact(offsetLong);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "indexOf failure: Offset must not exceed the int32 range: %d", offsetLong)
                    .setCause(e)
                    .build();
        }

        CelCodePointArray strCpa = CelCodePointArray.fromString(s);
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

    public static boolean stringIsEmpty(String str) {
        if (isCellNull(str)) {
            return true;
        }
        return str.isEmpty();
    }

    public static boolean isEmpty(Object whatever) {
        if (isCellNull(whatever)) {
            return true;
        }
        if (whatever instanceof Optional<?> opt) {
            if (opt.isEmpty()) {
                return true;
            }
            whatever = opt.get();
        }
        if (whatever instanceof String str) {
            return str.isEmpty();
        }
        if (whatever instanceof PolyStringCelValue ps) {
            return ps.getOrig().isEmpty();
        }
        if (whatever instanceof Collection<?> col) {
            return col.isEmpty();
        }
        if (whatever instanceof AbstractContainerValueCelValue<?> cval) {
            return cval.isEmpty();
        }
        return false;
    }

    public static boolean stringIsBlank(String str) {
        if (isCellNull(str)) {
            return true;
        }
        return str.isBlank();
    }


    private static Long lastIndexOf(PolyStringCelValue pstr, String substr) throws CelEvaluationException {
        return lastIndexOf(pstr.getOrig(), substr);
    }

    private static Long lastIndexOf(String s, String substr) throws CelEvaluationException {
        return lastIndexOf(s, substr, null);
    }

    private static Long lastIndexOfString(Object[] args) throws CelEvaluationException {
        return lastIndexOf(((String) args[0]), (String) args[1], (long) args[2]);
    }

    private static Long lastIndexOfPolystring(Object[] args) throws CelEvaluationException {
        return lastIndexOf(((PolyStringCelValue) args[0]).getOrig(), (String) args[1], (long) args[2]);
    }

    private static Long lastIndexOf(String s, String substr, Long offset)
            throws CelEvaluationException {
        CelCodePointArray strCpa = CelCodePointArray.fromString(s);
        CelCodePointArray substrCpa = CelCodePointArray.fromString(substr);

        if (offset == null) {
            offset = (long) strCpa.length() - 1;
        }

        if (substrCpa.isEmpty()) {
            return offset;
        }

        if (strCpa.length() < substrCpa.length()) {
            return -1L;
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

        if (off < 0 || off >= strCpa.length()) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "lastIndexOf failure: Offset out of range: %d", offset)
                    .build();
        }

        if (off > strCpa.length() - substrCpa.length()) {
            off = strCpa.length() - substrCpa.length();
        }

        for (int i = off; i >= 0; i--) {
            int j;
            for (j = 0; j < substrCpa.length(); j++) {
                if (strCpa.get(i + j) != substrCpa.get(j)) {
                    break;
                }
            }

            if (j == substrCpa.length()) {
                return (long) i;
            }
        }

        return -1L;
    }

    private static String replaceAllString(Object[] objects) {
        return replace((String) objects[0], (String) objects[1], (String) objects[2], -1);
    }

    private static String replaceAllPolystring(Object[] objects) {
        return replace(((PolyStringCelValue) objects[0]).getOrig(), (String) objects[1], (String) objects[2], -1);
    }

    private static String replaceString(Object[] objects) throws CelEvaluationException {
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

        return replace((String) objects[0], (String) objects[1], (String) objects[2], index);
    }

    private static String replacePolystring(Object[] objects) throws CelEvaluationException {
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


    private static List<String> splitPolystring(Object[] args) throws CelEvaluationException {
        return split(((PolyStringCelValue) args[0]).getOrig(), (String) args[1], (Long) args[2]);
    }

    private static List<String> splitPolystring(PolyStringCelValue pstr, String separator) throws CelEvaluationException {
        return split(pstr.getOrig(), separator, null);
    }

    private static List<String> splitString(Object[] args) throws CelEvaluationException {
        return split((String) args[0], (String) args[1], (Long) args[2]);
    }

    private static List<String> splitString(String str, String separator) throws CelEvaluationException {
        return split(str, separator, null);
    }
    /** Returns a **mutable** list of strings split on the separator */
    private static List<String> split(String str, String separator, Long limitLong) throws CelEvaluationException {

        int limit;
        if (limitLong == null) {
            limit = Integer.MAX_VALUE;
        } else {
            try {
                limit = Math.toIntExact(limitLong);
            } catch (ArithmeticException e) {
                throw CelEvaluationExceptionBuilder.newBuilder(
                                "split failure: Limit must not exceed the int32 range: %d", limitLong)
                        .setCause(e)
                        .build();
            }
        }

        if (limit == 0) {
            return new ArrayList<>();
        }

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

    private static String substring(PolyStringCelValue ps, long i) throws CelEvaluationException {
        return substring(ps.getOrig(), i);
    }

    private static String substring(String s, long i) throws CelEvaluationException {
        int beginIndex;
        try {
            beginIndex = Math.toIntExact(i);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "substring failure: Index must not exceed the int32 range: %d", i)
                    .setCause(e)
                    .build();
        }

        CelCodePointArray codePointArray = CelCodePointArray.fromString(s);

        if (beginIndex > codePointArray.length()) {
            return "";
        }

        boolean indexIsInRange = beginIndex >= 0;
        if (!indexIsInRange) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "substring failure: Range [%d, %d) out of bounds",
                            beginIndex, codePointArray.length())
                    .build();
        }

        return codePointArray.slice(beginIndex, codePointArray.length()).toString();
    }


    /**
     * @param args Object array with indices of [0: string], [1: beginIndex], [2: endIndex]
     */
    private static String substringPolystring(Object[] args) throws CelEvaluationException {
        return substring(((PolyStringCelValue) args[0]).getOrig(), (Long) args[1], (Long) args[2]);
    }

    /**
     * @param args Object array with indices of [0: string], [1: beginIndex], [2: endIndex]
     */
    private static String substringString(Object[] args) throws CelEvaluationException {
        return substring((String) args[0], (Long) args[1], (Long) args[2]);
    }

    private static String substring(String s, Long beginIndexLong, Long endIndexLong) throws CelEvaluationException {
        int beginIndex;
        int endIndex;
        try {
            beginIndex = Math.toIntExact(beginIndexLong);
            endIndex = Math.toIntExact(endIndexLong);
        } catch (ArithmeticException e) {
            throw CelEvaluationExceptionBuilder.newBuilder(
                            "substring failure: Indices must not exceed the int32 range: [%d, %d)",
                            beginIndexLong, endIndexLong)
                    .setCause(e)
                    .build();
        }

        CelCodePointArray codePointArray = CelCodePointArray.fromString(s);

        if (beginIndex > codePointArray.length()) {
            beginIndex = codePointArray.length();
        }
        if (endIndex > codePointArray.length()) {
            endIndex = codePointArray.length();
        }

        boolean indicesIsInRange =
                beginIndex <= endIndex
                        && beginIndex >= 0;
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
        return trim(text.getOrig());
    }

    private static String trim(String text) {
        CelCodePointArray textCpa = CelCodePointArray.fromString(text);
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
