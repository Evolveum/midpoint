/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.TimeProvider;
import org.apache.commons.codec.binary.Base32;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.authentication.api.OtpService;
import com.evolveum.midpoint.authentication.api.OtpServiceFactory;
import com.evolveum.midpoint.authentication.impl.otp.OtpAlgorithm;
import com.evolveum.midpoint.authentication.impl.otp.OtpServiceFactoryImpl;
import com.evolveum.midpoint.authentication.impl.otp.OtpType;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TOtpAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TestOtpService {

    public static final String ISSUER = "my+custom issuer";
    public static final String IDENTIFIER = "my identifier";

    private static final Clock CLOCK;

    static {
        // manually initialize clock to avoid dependency on Spring context

        CLOCK = new Clock();
        CLOCK.init();
    }

    @Test
    public void test100GenerateSecret() {
        OtpService service = createOtpService();
        String secret = service.generateSecret();

        OtpAuthenticationModuleType module = createAuthenticationModule();
        String algorithm = module.getAlgorithm() != null ? module.getAlgorithm() : OtpAlgorithm.SHA1.value;

        OtpAlgorithm otpAlgorithm = OtpAlgorithm.fromValue(algorithm);

        Assertions.assertThat(secret)
                .isNotNull();

        byte[] bytes = new Base32().decode(secret);
        Assertions.assertThat(bytes)
                .hasSize(otpAlgorithm.secretLength);
    }

    @Test
    public void test110GenerateAuthUrl() {
        OtpService service = createOtpService();
        final String secret = service.generateSecret();

        final String label = "my account+label";
        String url = service.generateAuthUrl(label, secret);

        Assertions.assertThat(url)
                .isNotNull()
                .contains(escapeUrl(ISSUER) + ":" + escapeUrl(label));

        URI uri = URI.create(url);

        Assertions.assertThat(uri)
                .hasScheme("otpauth")
                .hasHost(OtpType.TOTP.authUrlName)
                .hasPath(String.format("/%s:%s", ISSUER, label))
                .hasParameter("secret", secret)
                .hasParameter("period", "30")
                .hasParameter("digits", "6")
                .hasParameter("algorithm", "SHA1")
                .hasParameter("issuer", ISSUER);
    }

    @Test
    public void test120VerifyCode() throws Exception {
        OtpService service = createOtpService();
        final String secret = service.generateSecret();

        TimeProvider timeProvider = () -> CLOCK.getEpochSecond();

        CodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);

        // wrong
        int code = 123;
        boolean result = service.verifyCode(secret, code);
        Assertions.assertThat(result);

        // correct
        code = Integer.parseInt(generator.generate(secret, timeProvider.getTime() / 30L));
        result = service.verifyCode(secret, code);
        Assertions.assertThat(result);
    }

    private String escapeUrl(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

    protected OtpService createOtpService() {
        OtpServiceFactory factory = new OtpServiceFactoryImpl(CLOCK);

        OtpAuthenticationModuleType module = createAuthenticationModule();
        return factory.create(module);
    }

    protected OtpAuthenticationModuleType createAuthenticationModule() {
        return new TOtpAuthenticationModuleType()
                .identifier(IDENTIFIER)
                .issuer(ISSUER)
                .label(UserType.F_FULL_NAME.toBean());
    }
}
