/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.evaluator;

import java.util.List;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.authentication.impl.otp.OtpAuthenticationContext;
import com.evolveum.midpoint.authentication.impl.otp.OtpAuthenticationEvaluator;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestOtpAuthenticationEvaluator extends TestAbstractAuthenticationEvaluator<Integer, OtpAuthenticationContext, OtpAuthenticationEvaluator> {

    private static final String IDENTIFIER = "MyTestIdentifier";
    private static final String ISSUER = "MyTestIssuer";

    private static final String GUYBRUSH_SECRET = "R2YTPJHLUUYEJ2BUUEXDRWTMWMAU73ZF";

    private static final TOtpAuthenticationModuleType TOTP_AUTHENTICATION_MODULE;

    static {
        TOTP_AUTHENTICATION_MODULE = new TOtpAuthenticationModuleType()
                .identifier(IDENTIFIER)
                .issuer(ISSUER)
                .label(UserType.F_FULL_NAME.toBean());
    }

    @Autowired
    private OtpAuthenticationEvaluator authenticationEvaluator;

    @Override
    public Integer get103EmptyPasswordJack() {
        return null;
    }

    @Override
    public OtpAuthenticationEvaluator getAuthenticationEvaluator() {
        return authenticationEvaluator;
    }

    @Override
    public OtpAuthenticationContext getAuthenticationContext(
            String username, Integer value, List<ObjectReferenceType> requiredAssignments) {

        return new OtpAuthenticationContext(
                username, UserType.class, value, requiredAssignments, null, TOTP_AUTHENTICATION_MODULE);
    }

    @Override
    public Integer getGoodPasswordJack() {
        return createCorrectCode(USER_JACK.getObjectable());
    }

    private Integer createCorrectCode(String secret) throws CodeGenerationException {
        CodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        String code = generator.generate(secret, Clock.get().getEpochSecond() / 30);

        return Integer.parseInt(code);
    }

    private Integer createCorrectCode(UserType user) {
        OtpCredentialType otp = user.getCredentials().getOtps().getOtp().get(0);

        try {
            String secret = protector.decryptString(otp.getSecret());

            return createCorrectCode(secret);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't decrypt OTP secret", e);
        }
    }

    @Override
    public Integer getBadPasswordJack() {
        return 957637; // some random code that is not correct
    }

    @Override
    public Integer getGoodPasswordGuybrush() {
        try {
            return createCorrectCode(GUYBRUSH_SECRET);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create correct OTP code for Guybrush", e);
        }
    }

    @Override
    public Integer getBadPasswordGuybrush() {
        return 857964; // some random code that is not correct
    }

    @Override
    public String getEmptyPasswordExceptionMessageKey() {
        return "web.security.provider.invalid.credentials";
    }

    @Override
    public AbstractCredentialType getCredentialUsedForAuthentication(UserType user) {
        return user.getCredentials().getOtps();
    }

    @Override
    public String getModuleIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getSequenceIdentifier() {
        return "totp";
    }

    @Override
    public ItemName getCredentialType() {
        return CredentialsType.F_OTPS;
    }

    @Override
    public void modifyUserCredential(Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {

        OtpCredentialType credential = new OtpCredentialType()
                .secret(new ProtectedStringType()
                        .clearValue(GUYBRUSH_SECRET))
                .verified(true)
                .createTimestamp(XmlTypeConverter.createXMLGregorianCalendar());

        modifyObjectReplaceContainer(
                UserType.class,
                USER_GUYBRUSH_OID,
                ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_OTPS, OtpCredentialsType.F_OTP),
                task,
                result,
                credential);
    }
}
