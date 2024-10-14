/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest.authentication.oidc;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.testing.rest.authentication.TestAbstractAuthentication;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertNotNull;

public abstract class TestAbstractOidcRestModule extends TestAbstractAuthentication {

    private static final Trace LOGGER = TraceManager.getTrace(TestAbstractOidcRestModule.class);

    public static final String USER_ADMINISTRATOR_USERNAME = "administrator";

    public static final String NAME_OF_USERNAME_CLAIM_VALUE = "preferred_username";

    public static final File SECURITY_POLICY_ISSUER_URI =
            new File(BASE_AUTH_REPO_DIR, "security-policy-issuer-uri.xml");
    public static final File SECURITY_POLICY_JWS_URI =
            new File(BASE_AUTH_REPO_DIR, "security-policy-jws-uri.xml");
    public static final File SECURITY_POLICY_JWS_URI_WRONG_ALG =
            new File(BASE_AUTH_REPO_DIR, "security-policy-jws-uri-wrong-alg.xml");
    public static final File SECURITY_POLICY_PUBLIC_KEY =
            new File(BASE_AUTH_REPO_DIR, "security-policy-public-key.xml");
    public static final File SECURITY_POLICY_PUBLIC_KEY_WRONG_ALG =
            new File(BASE_AUTH_REPO_DIR, "security-policy-public-key-wrong-alg.xml");
    public static final File SECURITY_POLICY_PUBLIC_KEY_KEYSTORE =
            new File(BASE_AUTH_REPO_DIR, "security-policy-public-key-keystore.xml");
    public static final File SECURITY_POLICY_WRONG_ATTRIBUTE_NAME =
            new File(BASE_AUTH_REPO_DIR, "security-policy-wrong-attribute-name.xml");
    public static final File SECURITY_POLICY_USER_INFO_URI =
            new File(BASE_AUTH_REPO_DIR, "security-policy-user-info-uri.xml");

    private static final String NAME_OF_USERNAME_CLAIM_KEY = "nameOfUsernameClaim";
    protected static final String MIDPOINT_USER_PASSWORD_KEY = "midpointUserPassword";
    protected static final String CLIENT_ID_KEY = "clientId";
    protected static final String CLIENT_SECRET_KEY = "clientSecret";
    private static final String ISSUER_URI_KEY = "issuerUri";
    private static final String JWK_SET_URI_KEY = "jwkSetUri";
    private static final String TRUSTING_ASYMMETRIC_CERT_KEY = "trustingAsymmetricCertificate";
    private static final String KEY_STORE_PATH_KEY = "keyStorePath";
    private static final String USER_INFO_URI_KEY = "userInfoUri";

    private Properties properties;

    @Override
    public void initSystem(Task initTask, OperationResult result) throws Exception {
        super.initSystem(initTask, result);

        this.properties = new Properties();
        InputStream is = new FileInputStream(getPropertyFile());
        properties.load(is);
    }

    private File getPropertyFile(){
        return new File("src/test/resources/authentication/configuration/oidc.properties");
    }

    protected String getProperty(String key) {
        return StringEscapeUtils.escapeXml10(properties.getProperty(getServerPrefix() + "." + key));
    }

    protected String getUserPasssword() {
        return properties.getProperty(getServerPrefix() + "." + MIDPOINT_USER_PASSWORD_KEY);
    }

    protected abstract String getServerPrefix();

    protected String createTag(String key){
        return "||" + key + "||";
    }

    protected String getSecurityPolicy(File securityPolicyFile) throws IOException {
        String securityPolicy = FileUtils.readFileToString(
                securityPolicyFile,
                StandardCharsets.UTF_8);
        securityPolicy = securityPolicy.replace(createTag(NAME_OF_USERNAME_CLAIM_KEY), getNameOfUsernameClaim());
        return securityPolicy;
    }

    @Test
    public void test001OidcAuthByIssuerUri() throws Exception {
        String securityContent = getSecurityPolicy(SECURITY_POLICY_ISSUER_URI);
        securityContent = securityContent.replace(createTag(ISSUER_URI_KEY), getProperty(ISSUER_URI_KEY));
        replaceSecurityPolicy(securityContent);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertSuccess(response);
    }

    @Test
    public void test002OidcAuthByJWSUri() throws Exception {
        String securityContent = getSecurityPolicy(SECURITY_POLICY_JWS_URI);
        securityContent = securityContent.replace(createTag(JWK_SET_URI_KEY), getProperty(JWK_SET_URI_KEY));
        replaceSecurityPolicy(securityContent);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertSuccess(response);
    }

    @Test
    public void test003OidcAuthByJWSUriWithWrongAlg() throws Exception {
        String securityContent = getSecurityPolicy(SECURITY_POLICY_JWS_URI_WRONG_ALG);
        securityContent = securityContent.replace(createTag(JWK_SET_URI_KEY), getProperty(JWK_SET_URI_KEY));
        replaceSecurityPolicy(securityContent);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertUnsuccess(response);
    }

    @Test
    public void test004OidcAuthByPublicKey() throws Exception {
        Map<String, String> publicKeys = getPublicKeys();
        boolean allKeysFails = true;
        WebClient client = prepareClient();

        for (String publicKeyKid : publicKeys.keySet()) {
            String securityContent = getSecurityPolicy(SECURITY_POLICY_PUBLIC_KEY);
            securityContent = securityContent.replace(createTag(TRUSTING_ASYMMETRIC_CERT_KEY), publicKeys.get(publicKeyKid));
            replaceSecurityPolicy(securityContent);

            TimeUnit.SECONDS.sleep(2);
            when();
            Response response = client.get();

            boolean success = true;
            then();
            try {
                assertSuccess(response);
            } catch (AssertionError e) {
                success = false;
                LOGGER.error("Couldn't use key with kid " + publicKeyKid + " for test.", e);
                getDummyAuditService().clear();
            }
            if (success) {
                allKeysFails = false;
                break;
            }
        }
        if (allKeysFails) {
            throw new AssertionError("Test for each public keys fails.");
        }
    }

    @Test
    public void test005OidcAuthByPublicKeyWithWrongAlg() throws Exception {
        Map<String, String> publicKeys = getPublicKeys();
        boolean oneIsSuccess = false;
        WebClient client = prepareClient();

        for (String publicKeyKid : publicKeys.keySet()) {
            String securityContent = getSecurityPolicy(SECURITY_POLICY_PUBLIC_KEY_WRONG_ALG);
            securityContent = securityContent.replace(createTag(TRUSTING_ASYMMETRIC_CERT_KEY), publicKeys.get(publicKeyKid));
            replaceSecurityPolicy(securityContent);

            when();
            Response response = client.get();

            then();
            try {
                assertUnsuccess(response);
            } catch (AssertionError e) {
                LOGGER.error("Unexpected success assert for using key with kid " + publicKeyKid + " for test.", e);
                oneIsSuccess = true;
                break;
            }
        }
        if (oneIsSuccess) {
            throw new AssertionError("Test for each public keys fails.");
        }
    }

    @Test
    public void test006OidcAuthByPublicKeyAsKeystore() throws Exception {
        Map<String, String> publicKeys = getPublicKeys();
        boolean allKeysFails = true;

        String pathToKeystore = MIDPOINT_HOME + "/test-keystore";
        String securityContent = getSecurityPolicy(SECURITY_POLICY_PUBLIC_KEY_KEYSTORE);
        securityContent = securityContent.replace(createTag(KEY_STORE_PATH_KEY), pathToKeystore);
        replaceSecurityPolicy(securityContent);

        WebClient client = prepareClient();

        for (String publicKeyKid : publicKeys.keySet()) {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null);
            String publicKey = publicKeys.get(publicKeyKid);

            byte[] certBytes;
            if (Base64.isBase64(publicKey)) {
                boolean isBase64Url = publicKey.contains("-") || publicKey.contains("_");
                certBytes = Base64Utility.decode(publicKey, isBase64Url);
            } else {
                certBytes = publicKey.getBytes();
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate certificate = cf.generateCertificate(new ByteArrayInputStream(certBytes));
            keystore.setCertificateEntry("cert", certificate);

            File file = new File(pathToKeystore);
            file.deleteOnExit();
            if (file.exists()) {
                file.delete();
            }

            FileOutputStream out = new FileOutputStream(file);
            keystore.store(out, "secret".toCharArray());
            out.close();

            when();
            Response response = client.get();

            boolean success = true;
            then();
            try {
                assertSuccess(response);
            } catch (AssertionError e) {
                success = false;
                LOGGER.error("Couldn't use key with kid " + publicKeyKid + " for test.", e);
                getDummyAuditService().clear();
            }
            if (success) {
                allKeysFails = false;
                break;
            }
        }
        if (allKeysFails) {
            throw new AssertionError("Test for each public keys fails.");
        }
    }

    @Test
    public void test007WrongAttributeName() throws Exception {
        String securityContent = getSecurityPolicy(SECURITY_POLICY_WRONG_ATTRIBUTE_NAME);
        securityContent = securityContent.replace(createTag(ISSUER_URI_KEY), getProperty(ISSUER_URI_KEY));
        replaceSecurityPolicy(securityContent);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertUnsuccess(response, 0);
    }

    @Test
    public void test008OidcOpaqueToken() throws Exception {
        String securityContent = getSecurityPolicy(SECURITY_POLICY_USER_INFO_URI);
        securityContent = securityContent.replace(createTag(USER_INFO_URI_KEY), getProperty(USER_INFO_URI_KEY));
        replaceSecurityPolicy(securityContent);

        WebClient client = prepareClientForOpaqueToken();

        when();
        Response response = client.get();

        then();
        assertSuccess(response);
    }

    protected WebClient prepareClientForOpaqueToken() {
        return prepareClient();
    }

    private Map<String, String> getPublicKeys() {
        Map<String, String> publicKeys = new HashMap<>();
        try {
            List<LinkedTreeMap<String, Object>> keys = ((List<LinkedTreeMap<String, Object>>) JSONObjectUtils.parse(
                    WebClient.create(getJwksUri()).get().readEntity(String.class)).get("keys"));
            keys.stream()
                    .filter(json ->
                            json.containsKey("use")
                                    && "sig".equals(json.get("use")))
                    .forEach(key ->
                publicKeys.put(
                        (String) key.get("kid"),
                        ((List<String>) key.get("x5c")).get(0)));
            return publicKeys;
        } catch (Exception e) {
            LOGGER.error("Couldn't get public key from IDP", e);
            return publicKeys;
        }
    }

    private String getJwksUri(){
        return getProperty(JWK_SET_URI_KEY);
    }

    protected abstract WebClient prepareClient();

    protected void createAuthorizationHeader(WebClient client, String username, String password) {
        if (username != null) {
            String authorizationHeader = username + " " + password;
            client.header("Authorization", authorizationHeader);
        }
    }

    protected void assertSuccess(Response response) {
        assertStatus(response, 200);
        UserType userType = response.readEntity(UserType.class);
        assertNotNull("Returned entity in body must not be null.", userType);
        logger.info("Returned entity: {}", userType.asPrismObject().debugDump());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    protected void assertUnsuccess(Response response) {
        assertUnsuccess(response, 1);
    }

    protected void assertUnsuccess(Response response, int expectedAuditRecords) {
        assertStatus(response, 401);

        if (expectedAuditRecords != 0) {
            displayDumpable("Audit", getDummyAuditService());
            getDummyAuditService().assertRecords(expectedAuditRecords);
            getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
        }
    }

    protected String getNameOfUsernameClaim(){
        return NAME_OF_USERNAME_CLAIM_VALUE;
    }
}
