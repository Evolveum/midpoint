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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.AccessTokenResponse;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Properties;

import static org.testng.AssertJUnit.assertNotNull;

public abstract class TestAbstractOidcRestModule extends TestAbstractAuthentication {

    public static final String USER_ADMINISTRATOR_USERNAME = "administrator";

    public static final File SECURITY_POLICY_ISSUER_URI =
            new File(BASE_REPO_DIR, "security-policy-issuer-uri.xml");
    public static final File SECURITY_POLICY_JWS_URI =
            new File(BASE_REPO_DIR, "security-policy-jws-uri.xml");
    public static final File SECURITY_POLICY_JWS_URI_WRONG_ALG =
            new File(BASE_REPO_DIR, "security-policy-jws-uri-wrong-alg.xml");
    public static final File SECURITY_POLICY_PUBLIC_KEY =
            new File(BASE_REPO_DIR, "security-policy-public-key.xml");
    public static final File SECURITY_POLICY_PUBLIC_KEY_WRONG_ALG =
            new File(BASE_REPO_DIR, "security-policy-public-key-wrong-alg.xml");
    public static final File SECURITY_POLICY_PUBLIC_KEY_KEYSTORE =
            new File(BASE_REPO_DIR, "security-policy-public-key-keystore.xml");
    public static final File SECURITY_POLICY_WRONG_ATTRIBUTE_NAME =
            new File(BASE_REPO_DIR, "security-policy-wrong-attribute-name.xml");

    private static final String MIDPOINT_USER_PASSWORD_KEY = "midpointUserPassword";
    protected static final String CLIENT_ID_KEY = "clientId";
    protected static final String CLIENT_SECRET_KEY = "clientSecret";
    private static final String ISSUER_URI_KEY = "issuerUri";
    private static final String JWK_SET_URI_KEY = "jwkSetUri";
    private static final String TRUSTING_ASYMMETRIC_CERT_KEY = "trustingAsymmetricCertificate";
    private static final String KEY_STORE_PATH_KEY = "keyStorePath";

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

    protected abstract String getServerPrefix();

    protected String createTag(String key){
        return "||" + key + "||";
    }

    public abstract AuthzClient getAuthzClient();

    protected String getSecurityPolicy(File securityPolicyFile) throws IOException {
        return FileUtils.readFileToString(
                securityPolicyFile,
                StandardCharsets.UTF_8);
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
        String securityContent = getSecurityPolicy(SECURITY_POLICY_PUBLIC_KEY);
        securityContent = securityContent.replace(createTag(TRUSTING_ASYMMETRIC_CERT_KEY), getPublicKey());
        replaceSecurityPolicy(securityContent);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertSuccess(response);
    }

    @Test
    public void test005oidcAuthByPublicKeyWithWrongAlg() throws Exception {
        String securityContent = getSecurityPolicy(SECURITY_POLICY_PUBLIC_KEY_WRONG_ALG);
        securityContent = securityContent.replace(createTag(TRUSTING_ASYMMETRIC_CERT_KEY), getPublicKey());
        replaceSecurityPolicy(securityContent);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertUnsuccess(response);
    }

    @Test
    public void test006OidcAuthByPublicKeyAsKeystore() throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null);
        String publicKey = getPublicKey();

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

        String pathToKeystore = MIDPOINT_HOME + "/test-keystore";
        File file = new File(pathToKeystore);
        file.deleteOnExit();
        if (file.exists()) {
            file.delete();
        }

        FileOutputStream out = new FileOutputStream(file);
        keystore.store(out, "secret".toCharArray());
        out.close();

        String securityContent = getSecurityPolicy(SECURITY_POLICY_PUBLIC_KEY_KEYSTORE);
        securityContent = securityContent.replace(createTag(KEY_STORE_PATH_KEY), pathToKeystore);
        replaceSecurityPolicy(securityContent);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertSuccess(response);
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

    protected abstract String getPublicKey();

    private WebClient prepareClient() {
        AccessTokenResponse result = getAuthzClient().obtainAccessToken(
                USER_ADMINISTRATOR_USERNAME,
                getProperty(MIDPOINT_USER_PASSWORD_KEY));

        WebClient client = prepareClient(result.getTokenType(), result.getToken());
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
        return client;
    }

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
}
