/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.TimestampFormatUtil;
import com.evolveum.midpoint.model.common.expression.script.cel.CelTypeMapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Timestamp;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZonedDateTime;

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
        return ImmutableSet.of(

            // format.formatDateTime
            // Java-like function, compatible with original midPoint 3.x BasicExpressionFunctions
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            FUNCTION_NAME_PREFIX_DOT + "formatDateTime",
                            CelOverloadDecl.newGlobalOverload(
                                    FUNCTION_NAME_PREFIX + "_formatDateTime",
                                    "TODO",
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
                                    "timetamp_" + FUNCTION_NAME_PREFIX + "_formatDateTime",
                                    "TODO",
                                    SimpleType.STRING,
                                    SimpleType.TIMESTAMP, SimpleType.STRING)),
                    CelFunctionBinding.from("timetamp_" + FUNCTION_NAME_PREFIX + "_formatDateTime",
                            Timestamp.class,  String.class, this::formatDateTime)

            ),

            // format.parseDateTime
            // Java-like function, compatible with original midPoint 3.x BasicExpressionFunctions
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            FUNCTION_NAME_PREFIX_DOT + "parseDateTime",
                            CelOverloadDecl.newGlobalOverload(
                                    FUNCTION_NAME_PREFIX + "_parseDateTime",
                                    "TODO",
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
                                    "TODO",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.STRING, SimpleType.STRING)),
                    CelFunctionBinding.from("string_" + FUNCTION_NAME_PREFIX + "_parseDateTime",
                            String.class,  String.class, this::parseDateTime)

            ),

            // format.strftime
            // POSIX-like function, compatible with other CEL implementations and UNIX world
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            FUNCTION_NAME_PREFIX_DOT + "strftime",
                            CelOverloadDecl.newGlobalOverload(
                                    FUNCTION_NAME_PREFIX + "_strftime",
                                    "TODO",
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
                                    "timetamp_" + FUNCTION_NAME_PREFIX + "_strftime",
                                    "TODO",
                                    SimpleType.STRING,
                                    SimpleType.TIMESTAMP, SimpleType.STRING)),
                    CelFunctionBinding.from("timetamp_" + FUNCTION_NAME_PREFIX + "_strftime",
                            Timestamp.class,  String.class, this::strftime)

            ),

            // format.strptime
            // POSIX-like function, compatible with other CEL implementations and UNIX world
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            FUNCTION_NAME_PREFIX_DOT + "strptime",
                            CelOverloadDecl.newGlobalOverload(
                                    FUNCTION_NAME_PREFIX + "_strptime",
                                    "TODO",
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
                                    "TODO",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.STRING, SimpleType.STRING)),
                    CelFunctionBinding.from("string_" + FUNCTION_NAME_PREFIX + "_strptime",
                            String.class,  String.class, this::strptime)

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
}
