/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.TimestampFormatUtil;
import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.model.common.expression.script.mel.value.PolyStringCelValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Timestamp;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelFunctionOverload;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Extensions for CEL compiler and runtime implementing formatting and parsing functions.
 *
 * @author Radovan Semancik
 */
public class CelFormatExtensions extends AbstractMidPointCelExtensions {

    private static final Trace LOGGER = TraceManager.getTrace(CelFormatExtensions.class);

    private static final String FUNCTION_NAME_PREFIX = "format";
    private static final String FUNCTION_NAME_PREFIX_DOT = FUNCTION_NAME_PREFIX+".";

    private final BasicExpressionFunctions basicExpressionFunctions;

    public CelFormatExtensions(BasicExpressionFunctions basicExpressionFunctions) {
        this.basicExpressionFunctions = basicExpressionFunctions;
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        Set<Function> functions = new HashSet<>(
            ImmutableSet.of(

                // format.concatName
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "concatName",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_concatName",
                                        "Concatenates a user-friendly name from the list of provided components.",
                                        SimpleType.STRING,
                                        ListType.create(SimpleType.ANY))),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_concatName", List.class,
                                this::concatName)

                ),


                // DateTime functions:

                // format.formatDateTime
                // Java-like function, compatible with original midPoint 3.x BasicExpressionFunctions
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "formatDateTime",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_formatDateTime",
                                        "Formats provided timestamp to string, using a format template specified in Java SimpleDateFormat notation.",
                                        SimpleType.STRING,
                                        SimpleType.TIMESTAMP, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_formatDateTime", Timestamp.class, String.class,
                                this::formatDateTime)

                ),

                // timestamp.formatDateTime
                // Java-like function, compatible with original midPoint 3.x BasicExpressionFunctions
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "formatDateTime",
                                CelOverloadDecl.newMemberOverload(
                                        "timestamp_" + FUNCTION_NAME_PREFIX + "_formatDateTime",
                                        "Formats provided timestamp to string, using a format template specified in Java SimpleDateFormat notation.",
                                        SimpleType.STRING,
                                        SimpleType.TIMESTAMP, SimpleType.STRING)),
                        CelFunctionBinding.from("timestamp_" + FUNCTION_NAME_PREFIX + "_formatDateTime",
                                Timestamp.class, String.class, this::formatDateTime)

                ),

                // format.parseDateTime
                // Java-like function, compatible with original midPoint 3.x BasicExpressionFunctions
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "parseDateTime",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_parseDateTime",
                                        "Parses provided string to timestamp, using a format template specified in Java SimpleDateFormat notation.",
                                        SimpleType.TIMESTAMP,
                                        SimpleType.STRING, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_parseDateTime", String.class, String.class,
                                this::parseDateTime)

                ),

                // str.parseDateTime
                // Java-like function, compatible with original midPoint 3.x BasicExpressionFunctions
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "parseDateTime",
                                CelOverloadDecl.newMemberOverload(
                                        "string_" + FUNCTION_NAME_PREFIX + "_parseDateTime",
                                        "Parses provided string to timestamp, using a format template specified in Java SimpleDateFormat notation.",
                                        SimpleType.TIMESTAMP,
                                        SimpleType.STRING, SimpleType.STRING)),
                        CelFunctionBinding.from("string_" + FUNCTION_NAME_PREFIX + "_parseDateTime",
                                String.class, String.class, this::parseDateTime)

                ),

                // format.strftime
                // POSIX-like function, compatible with other CEL implementations and UNIX world
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "strftime",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_strftime",
                                        "Formats provided timestamp to string, using a format template specified in POSIX notation.",
                                        SimpleType.STRING,
                                        SimpleType.TIMESTAMP, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_strftime", Timestamp.class, String.class,
                                this::strftime)

                ),

                // timestamp.strftime
                // POSIX-like function, compatible with other CEL implementations and UNIX world
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "strftime",
                                CelOverloadDecl.newMemberOverload(
                                        "timestamp_" + FUNCTION_NAME_PREFIX + "_strftime",
                                        "Formats provided timestamp to string, using a format template specified in POSIX notation.",
                                        SimpleType.STRING,
                                        SimpleType.TIMESTAMP, SimpleType.STRING)),
                        CelFunctionBinding.from("timestamp_" + FUNCTION_NAME_PREFIX + "_strftime",
                                Timestamp.class, String.class, this::strftime)

                ),

                // format.strptime
                // POSIX-like function, compatible with other CEL implementations and UNIX world
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "strptime",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_strptime",
                                        "Parses provided string to timestamp, using a format template specified in POSIX notation.",
                                        SimpleType.TIMESTAMP,
                                        SimpleType.STRING, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_strptime", String.class, String.class,
                                this::strptime)

                ),

                // str.parseDateTime
                // POSIX-like function, compatible with other CEL implementations and UNIX world
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "strptime",
                                CelOverloadDecl.newMemberOverload(
                                        "string_" + FUNCTION_NAME_PREFIX + "_strptime",
                                        "Parses provided string to timestamp, using a format template specified in POSIX notation.",
                                        SimpleType.TIMESTAMP,
                                        SimpleType.STRING, SimpleType.STRING)),
                        CelFunctionBinding.from("string_" + FUNCTION_NAME_PREFIX + "_strptime",
                                String.class, String.class, this::strptime)

                )

        ));

        functions.addAll(functionsParseName("parseGivenName", this::parseGivenName));
        functions.addAll(functionsParseName("parseFamilyName", this::parseFamilyName));
        functions.addAll(functionsParseName("parseAdditionalName", this::parseAdditionalName));
        functions.addAll(functionsParseName("parseNickName", this::parseNickName));
        functions.addAll(functionsParseName("parseHonorificPrefix", this::parseHonorificPrefix));
        functions.addAll(functionsParseName("parseHonorificSuffix", this::parseHonorificSuffix));

        return ImmutableSet.copyOf(functions);
    }

    private Collection<? extends Function> functionsParseName(String functionName, CelFunctionOverload.Unary<Object> method) {
        return ImmutableSet.of(
                // format.parse*Name
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + functionName,
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_" + functionName,
                                        "Parses a component of person's full name.",
                                        SimpleType.STRING,
                                        SimpleType.ANY)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_" + functionName, Object.class,
                                method)

                ),

                // str.parse*Name
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                functionName,
                                CelOverloadDecl.newMemberOverload(
                                        "string_" + FUNCTION_NAME_PREFIX + "_" + functionName,
                                        "Parses a component of person's full name.",
                                        SimpleType.STRING,
                                        SimpleType.ANY)),
                        CelFunctionBinding.from("string_" + FUNCTION_NAME_PREFIX + "_" + functionName, String.class,
                                s -> method.apply((Object)s))

                ),

                // polystring.parse*Name
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                functionName,
                                CelOverloadDecl.newMemberOverload(
                                        "polystring_" + FUNCTION_NAME_PREFIX + "_" + functionName,
                                        "Parses a component of person's full name.",
                                        PolyStringCelValue.CEL_TYPE,
                                        SimpleType.ANY)),
                        CelFunctionBinding.from("polystring_" + FUNCTION_NAME_PREFIX + "_" + functionName, PolyStringCelValue.class,
                                s -> method.apply((Object)s))

                )

            );
    }


    private static final class Library implements CelExtensionLibrary<CelFormatExtensions> {
        private final CelFormatExtensions version0;

        private Library(BasicExpressionFunctions basicExpressionFunctions) {
            version0 = new CelFormatExtensions(basicExpressionFunctions);
        }

        @Override
        public String name() {
            return FUNCTION_NAME_PREFIX;
        }

        @Override
        public ImmutableSet<CelFormatExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    public static CelExtensionLibrary<CelFormatExtensions> library(BasicExpressionFunctions basicExpressionFunctions) {
        return new Library(basicExpressionFunctions);
    }

    @Override
    public int version() {
        return 0;
    }

    @NotNull
    private String formatDateTime(@NotNull Timestamp timestamp, @NotNull String format) {
        return basicExpressionFunctions.formatDateTime(format, CelTypeMapper.toMillis(timestamp));
    }

    private Timestamp parseDateTime(@NotNull String stringDate, @NotNull String format) {
        try {
            return CelTypeMapper.toTimestamp(basicExpressionFunctions.parseDateTime(format, stringDate));
        } catch (ParseException e) {
            // TODO: better exception handling
            throw new RuntimeException(e);
        }
    }

    private String strftime(@NotNull Timestamp timestamp, @NotNull String posixFormat) {
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return TimestampFormatUtil.strftime(instant, posixFormat);
    }

    private Timestamp strptime(@NotNull String stringDate, @NotNull String posixFormat) {
        ZonedDateTime zdt = TimestampFormatUtil.strptime(stringDate, posixFormat);
        Instant instant = zdt.toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public String concatName(List<Object> args) {
        return basicExpressionFunctions.concatName(CelTypeMapper.toJavaValues(args.toArray()));
    }

    public String parseGivenName(Object fullName) {
        return basicExpressionFunctions.parseGivenName(CelTypeMapper.toJavaValue(fullName));
    }

    public String parseFamilyName(Object fullName) {
        return basicExpressionFunctions.parseFamilyName(CelTypeMapper.toJavaValue(fullName));
    }

    public String parseAdditionalName(Object fullName) {
        return basicExpressionFunctions.parseAdditionalName(CelTypeMapper.toJavaValue(fullName));
    }

    public String parseNickName(Object fullName) {
        return basicExpressionFunctions.parseNickName(CelTypeMapper.toJavaValue(fullName));
    }

    public String parseHonorificPrefix(Object fullName) {
        return basicExpressionFunctions.parseHonorificPrefix(CelTypeMapper.toJavaValue(fullName));
    }

    public String parseHonorificSuffix(Object fullName) {
        return basicExpressionFunctions.parseHonorificSuffix(CelTypeMapper.toJavaValue(fullName));
    }

}
