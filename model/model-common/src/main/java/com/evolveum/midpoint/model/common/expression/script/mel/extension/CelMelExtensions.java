/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.model.common.expression.script.mel.value.MidPointValueProducer;
import com.evolveum.midpoint.model.common.expression.script.mel.value.PolyStringCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.QNameCelValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Timestamp;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.NullValue;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.parser.Operator;
import dev.cel.runtime.CelFunctionBinding;
import org.jetbrains.annotations.Nullable;

import java.time.*;
import java.util.Collection;
import java.util.List;

/**
 * Extensions for CEL compiler and runtime implementing behavior of "MEL" language.
 *
 * @author Radovan Semancik
 */
public class CelMelExtensions extends AbstractMidPointCelExtensions {

    private static final Trace LOGGER = TraceManager.getTrace(CelMelExtensions.class);


    private final BasicExpressionFunctions basicExpressionFunctions;
    private final Protector protector;

    public CelMelExtensions(Protector protector, BasicExpressionFunctions basicExpressionFunctions) {
        this.protector = protector;
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
                                    PolyStringCelValue.CEL_TYPE)),
                    CelFunctionBinding.from(
                            "polystring_"+FUNC_ENCRYPT_NAME, PolyStringCelValue.class, this::encrypt)),

            // TODO: bytes.encrypt()? Should we encrypt to ProtectedString or ProtectedData?

            // str.isBlank()
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

            // qname(local)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "qname",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-qname-local",
                                    "TODO",
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
                                    "TODO",
                                    QNameCelValue.CEL_TYPE,
                                    SimpleType.STRING, SimpleType.STRING)),
                    CelFunctionBinding.from("mel-qname-ns", String.class, String.class,
                            this::qname)

            ),

                // secret.resolveBinary(provider, key)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "secret.resolveBinary",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-secret-resolveBinary",
                                    "TODO",
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
                                    "TODO",
                                    SimpleType.STRING,
                                    SimpleType.STRING, SimpleType.STRING)),
                    CelFunctionBinding.from("mel-secret-resolveString", String.class, String.class,
                            this::secretResolveString)

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

            ),

            // timestamp.atStartOfDay
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "atStartOfDay",
                            CelOverloadDecl.newMemberOverload(
                                    "timestamp_atStartOfDay",
                                    "TODO",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.TIMESTAMP)),
                    CelFunctionBinding.from("timestamp_atStartOfDay",
                            Timestamp.class, this::atStartOfDay)
            ),

            // timestamp.atStartOfDay(timezone)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "atStartOfDay",
                            CelOverloadDecl.newMemberOverload(
                                    "timestamp_atStartOfDay_string",
                                    "TODO",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.TIMESTAMP, SimpleType.STRING)),
                    CelFunctionBinding.from("timestamp_atStartOfDay_string",
                            Timestamp.class, String.class, this::atStartOfDay)
            ),

            // timestamp.atEndOfDay
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "atEndOfDay",
                            CelOverloadDecl.newMemberOverload(
                                    "timestamp_atEndOfDay",
                                    "TODO",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.TIMESTAMP)),
                    CelFunctionBinding.from("timestamp_atEndOfDay",
                            Timestamp.class, this::atEndOfDay)
            ),

            // timestamp.atEndOfDay(timezone)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "atEndOfDay",
                            CelOverloadDecl.newMemberOverload(
                                    "timestamp_atEndOfDay_string",
                                    "TODO",
                                    SimpleType.TIMESTAMP,
                                    SimpleType.TIMESTAMP, SimpleType.STRING)),
                    CelFunctionBinding.from("timestamp_atEndOfDay_string",
                            Timestamp.class, String.class, this::atEndOfDay)
            ),

            // timestamp.longAgo()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "timestamp.longAgo",
                            CelOverloadDecl.newGlobalOverload(
                                    "mel-timestamp_longAgo",
                                    "TODO",
                                    SimpleType.TIMESTAMP)),
                    CelFunctionBinding.from("mel-timestamp_longAgo", ImmutableList.of(),
                            CelMelExtensions::longAgo)
            )


                // TODO: toSingle()? single()? scalar()?
        );
    }

    private QNameCelValue qname(String namespace, String localPart) {
        return QNameCelValue.create(namespace, localPart);
    }

    private QNameCelValue qname(String localPart) {
        return QNameCelValue.create(localPart);
    }

    private byte[] secretResolveBinary(String providerName, String key) {
        return basicExpressionFunctions.resolveSecretBinary(providerName, key).array();
    }

    private String secretResolveString(String providerName, String key) {
        return basicExpressionFunctions.resolveSecretString(providerName, key);
    }

    private static Timestamp longAgo(Object[] args) {
        return Timestamp.newBuilder()
                .setSeconds(0)
                .setNanos(0)
                .build();
    }

    private Timestamp atStartOfDay(Timestamp timestamp) {
        return atStartOfDay(timestamp, ZoneId.systemDefault());
    }

    private Timestamp atStartOfDay(Timestamp timestamp, String timezone) {
        return atStartOfDay(timestamp, ZoneId.of(timezone));
    }

    private Timestamp atStartOfDay(Timestamp timestamp, ZoneId zoneId) {
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        ZonedDateTime midnightZdt = LocalDate.ofInstant(instant, zoneId).atStartOfDay(zoneId);
        Instant midnightInstant = midnightZdt.toInstant();
        return Timestamp.newBuilder()
                .setSeconds(midnightInstant.getEpochSecond())
                .setNanos(midnightInstant.getNano())
                .build();
    }

    private Timestamp atEndOfDay(Timestamp timestamp) {
        return atEndOfDay(timestamp, ZoneId.systemDefault());
    }

    private Timestamp atEndOfDay(Timestamp timestamp, String timezone) {
        return atEndOfDay(timestamp, ZoneId.of(timezone));
    }

    private Timestamp atEndOfDay(Timestamp timestamp, ZoneId zoneId) {
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        ZonedDateTime eodZdt = LocalDate.ofInstant(instant, zoneId).atTime(LocalTime.MAX).atZone(zoneId);
        Instant eodInstant = eodZdt.toInstant();
        return Timestamp.newBuilder()
                .setSeconds(eodInstant.getEpochSecond())
                .setNanos(eodInstant.getNano())
                .build();
    }

    private static final class Library implements CelExtensionLibrary<CelMelExtensions> {
        private final CelMelExtensions version0;

        private Library(Protector protector, BasicExpressionFunctions basicExpressionFunctions) {
            version0 = new CelMelExtensions(protector, basicExpressionFunctions);
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

    public static CelExtensionLibrary<CelMelExtensions> library(Protector protector, BasicExpressionFunctions basicExpressionFunctions) {
        return new Library(protector, basicExpressionFunctions);
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
                throw createException("Attempt to get single value from a multi-valued property");
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
        if (o instanceof MidPointValueProducer<?> mpCelVal) {
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

}
