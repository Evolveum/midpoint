/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.extension;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.cel.CelTypeMapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

/**
 * Extensions for CEL compiler and runtime implementing formatting and parsing functions.
 *
 * @author Radovan Semancik
 */
public class CelLdapExtensions extends AbstractMidPointCelExtensions {

    private static final Trace LOGGER = TraceManager.getTrace(CelLdapExtensions.class);

    private static final String FUNCTION_NAME_PREFIX = "ldap";
    private static final String FUNCTION_NAME_PREFIX_DOT = FUNCTION_NAME_PREFIX+".";

    private final BasicExpressionFunctions basicExpressionFunctions;

    public CelLdapExtensions(BasicExpressionFunctions basicExpressionFunctions) {
        this.basicExpressionFunctions = basicExpressionFunctions;
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

                // ldap.composeDn
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "composeDn",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_composeDn",
                                        "Creates a valid LDAP distinguished name from the wide range of components. E.g.: "
                                                + "composeDnWithSuffix(['cn','foo','ou','baz','o','bar']). "
                                                + "Note: the DN is not normalized. The case of the attribute names and white spaces are "
                                                + "preserved.",
                                        SimpleType.STRING,
                                        ListType.create(SimpleType.ANY))),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_composeDn", List.class,
                                this::composeDn)

                ),

                // ldap.composeDnWithSuffix
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "composeDnWithSuffix",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_composeDnWithSuffix",
                                        "Creates a valid LDAP distinguished name from the wide range of components assuming "
                                                + "that the last component is a suffix. E.g.: "
                                                + "composeDnWithSuffix(['cn','foo','ou=baz,o=bar']). "
                                                + "Note: the DN is not normalized. The case of the attribute names and white spaces are "
                                                + "preserved.",
                                        SimpleType.STRING,
                                        ListType.create(SimpleType.ANY))),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_composeDnWithSuffix", List.class,
                                this::composeDnWithSuffix)

                ),

                // ldap.hashPassword
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "hashPassword",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_hashPassword_string",
                                        "Hashes cleartext password in an (unofficial) LDAP password format. "
                                                + "Supported algorithms: SSHA, SHA and MD5.",
                                        SimpleType.STRING,
                                        SimpleType.STRING, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_hashPassword_string", String.class, String.class,
                                this::hashPassword)

                ),

                // ldap.hashPassword
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "hashPassword",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_hashPassword_bytes",
                                        "Hashes cleartext password in an (unofficial) LDAP password format. "
                                                + "Supported algorithms: SSHA, SHA and MD5.",
                                        SimpleType.STRING,
                                        SimpleType.BYTES, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_hashPassword_bytes", byte[].class, String.class,
                                this::hashPassword)

                ),

                // ldap.hashPassword
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "hashPassword",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_hashPassword_protectedstring",
                                        "Hashes cleartext password in an (unofficial) LDAP password format. "
                                                + "Supported algorithms: SSHA, SHA and MD5.",
                                        SimpleType.STRING,
                                        CelTypeMapper.PROTECTED_STRING_CEL_TYPE, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_hashPassword_protectedstring", ProtectedStringType.class, String.class,
                                this::hashPassword)

                ),

                // ldap.determineSingleAttributeValue
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "determineSingleAttributeValue",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX + "_determineSingleAttributeValue",
                                        "Selects single value from many LDAP values, based on object DN. "
                                                + "E.g. value 'bar' is selected from list of values ['foo','bar','baz'] "
                                                + "because that value is present in DN 'uid=bar,o=example'.",
                                        SimpleType.STRING,
                                        SimpleType.STRING,
                                        SimpleType.STRING,
                                        ListType.create(SimpleType.ANY))),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX + "_determineSingleAttributeValue",
                                ImmutableList.of(String.class, String.class, List.class),
                                args -> determineSingleAttributeValue((String)args[0], (String)args[1],
                                        (List)args[2]))

                )

        );
    }

    private static final class Library implements CelExtensionLibrary<CelLdapExtensions> {
        private final CelLdapExtensions version0;

        private Library(BasicExpressionFunctions basicExpressionFunctions) {
            version0 = new CelLdapExtensions(basicExpressionFunctions);
        }

        @Override
        public String name() {
            return FUNCTION_NAME_PREFIX;
        }

        @Override
        public ImmutableSet<CelLdapExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    public static CelExtensionLibrary<CelLdapExtensions> library(BasicExpressionFunctions basicExpressionFunctions) {
        return new Library(basicExpressionFunctions);
    }

    @Override
    public int version() {
        return 0;
    }

    private String composeDn(List<Object> args) {
        try {
            return BasicExpressionFunctions.composeDn(CelTypeMapper.toJavaValues(args.toArray()));
        } catch (InvalidNameException e) {
            throw createException(e);
        }
    }

    private String composeDnWithSuffix(List<Object> args) {
        try {
            return BasicExpressionFunctions.composeDnWithSuffix(CelTypeMapper.toJavaValues(args.toArray()));
        } catch (InvalidNameException e) {
            throw createException(e);
        }
    }

    private String hashPassword(String clearString, String alg) {
        try {
            return basicExpressionFunctions.hashLdapPassword(clearString, alg);
        } catch (NoSuchAlgorithmException e) {
            throw createException(e);
        }
    }

    private String hashPassword(byte[] clearBytes, String alg) {
        try {
            return basicExpressionFunctions.hashLdapPassword(clearBytes, alg);
        } catch (NoSuchAlgorithmException e) {
            throw createException(e);
        }
    }

    private String hashPassword(ProtectedStringType prs, String alg) {
        try {
            return basicExpressionFunctions.hashLdapPassword(prs, alg);
        } catch (NoSuchAlgorithmException | EncryptionException e) {
            throw createException(e);
        }
    }

    private String determineSingleAttributeValue(String dn, String attributeName, List<Object> values) {
        try {
            return basicExpressionFunctions.determineLdapSingleAttributeValue(dn, attributeName, values);
        } catch (NamingException e) {
            throw createException(e);
        }
    }
}
