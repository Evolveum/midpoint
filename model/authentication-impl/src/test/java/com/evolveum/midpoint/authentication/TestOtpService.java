/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.time.SystemTimeProvider;
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

        CodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        CodeVerifier verifier = new DefaultCodeVerifier(generator, new SystemTimeProvider());

        // wrong
        int code = 123;
        boolean result = service.verifyCode(secret, code);
        Assertions.assertThat(result).isEqualTo(verifier.isValidCode(secret, Integer.toString(code)));

        // correct
        code = Integer.parseInt(generator.generate(secret, new SystemTimeProvider().getTime() / 30L));
        result = service.verifyCode(secret, code);
        Assertions.assertThat(result).isEqualTo(verifier.isValidCode(secret, Integer.toString(code)));
    }

    private String escapeUrl(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

    protected OtpService createOtpService() {
        // manually initialize clock to avoid dependency on Spring context
        Clock clock = new Clock();
        clock.init();

        OtpServiceFactory factory = new OtpServiceFactoryImpl(clock);

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
