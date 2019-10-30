/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.*;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.saml.provider.service.config.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.accept.ContentNegotiationStrategy;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MidpointFilterChainProxy extends FilterChainProxy {

    private static final transient Trace LOGGER = TraceManager.getTrace(MidpointFilterChainProxy.class);

    private final static String FILTER_APPLIED = MidpointFilterChainProxy.class.getName().concat(
            ".APPLIED");

    private List<SecurityFilterChain> filterChainsIgnoredPath;
    private List<SecurityFilterChain> filterChains;
    private MidpointFilterChainProxy.FilterChainValidator filterChainValidator = new MidpointFilterChainProxy.NullFilterChainValidator();
    private HttpFirewall firewall = new StrictHttpFirewall();
    private String lastAuthentication;


    @Autowired(required = false)
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @Autowired
    ApplicationContext context;

    @Autowired
    private MidPointGuiAuthorizationEvaluator accessDecisionManager;

    @Autowired
    private MidPointAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private MidPointAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private AuditedLogoutHandler auditedLogoutHandler;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private AuthenticationProvider midPointAuthenticationProvider;

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter;

    @Autowired(required = false)
    private RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter;

    @Autowired(required = false)
    private CasAuthenticationFilter casFilter;

    @Autowired(required = false)
    private LogoutFilter requestSingleLogoutFilter;

    @Autowired
    private SamlConfiguration samlConfiguration;

    @Autowired
    private SamlProviderServerBeanConfiguration samlProviderServerBeanConfiguration;

    @Value("${security.enable-csrf:true}")
    private boolean csrfEnabled;

    @Value("${security.authentication.active:saml}")
    private String authenticationProfile;

    public MidpointFilterChainProxy() {
    }

    public MidpointFilterChainProxy(List<SecurityFilterChain> filterChains) {
        this.filterChainsIgnoredPath = filterChains;
    }

    @Override
    public void afterPropertiesSet() {
        filterChainValidator.validate(this);
    }

    private int i = 0;

    private SecurityFilterChain getSamlFilter() throws Exception {
        if(i>=1){
            String metadata = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "              <!--\n" +
                    "                   This is example metadata only. Do *NOT* supply it as is without review,\n" +
                    "                   and do *NOT* provide it in real time to your partners.\n" +
                    "\n" +
                    "                   This metadata is not dynamic - it will not change as your configuration changes.\n" +
                    "              -->\n" +
                    "              <EntityDescriptor  xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:shibmd=\"urn:mace:shibboleth:metadata:1.0\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xmlns:mdui=\"urn:oasis:names:tc:SAML:metadata:ui\" entityID=\"https://idptestbed/idp/shibboleth\">\n" +
                    "\n" +
                    "                  <IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol urn:mace:shibboleth:1.0\">\n" +
                    "\n" +
                    "                      <Extensions>\n" +
                    "                          <shibmd:Scope regexp=\"false\">example.org</shibmd:Scope>\n" +
                    "              <!--\n" +
                    "                  Fill in the details for your IdP here\n" +
                    "\n" +
                    "                          <mdui:UIInfo>\n" +
                    "                              <mdui:DisplayName xml:lang=\"en\">A Name for the IdP at idptestbed</mdui:DisplayName>\n" +
                    "                              <mdui:Description xml:lang=\"en\">Enter a description of your IdP at idptestbed</mdui:Description>\n" +
                    "                              <mdui:Logo height=\"80\" width=\"80\">https://idptestbed/Path/To/Logo.png</mdui:Logo>\n" +
                    "                          </mdui:UIInfo>\n" +
                    "              -->\n" +
                    "                      </Extensions>\n" +
                    "\n" +
                    "                      <KeyDescriptor use=\"signing\">\n" +
                    "                          <ds:KeyInfo>\n" +
                    "                                  <ds:X509Data>\n" +
                    "                                      <ds:X509Certificate>\n" +
                    "              MIIDEzCCAfugAwIBAgIUS9SuTXwsFVVG+LjOEAbLqqT/el0wDQYJKoZIhvcNAQEL\n" +
                    "              BQAwFTETMBEGA1UEAwwKaWRwdGVzdGJlZDAeFw0xNTEyMTEwMjIwMjZaFw0zNTEy\n" +
                    "              MTEwMjIwMjZaMBUxEzARBgNVBAMMCmlkcHRlc3RiZWQwggEiMA0GCSqGSIb3DQEB\n" +
                    "              AQUAA4IBDwAwggEKAoIBAQCMAoDHx8xCIfv/6QKqt9mcHYmEJ8y2dKprUbpdcOjH\n" +
                    "              YvNPIl/lHPsUyrb+Nc+q2CDeiWjVk1mWYq0UpIwpBMuw1H6+oOqr4VQRi65pin0M\n" +
                    "              SfE0MWIaFo5FPvpvoptkHD4gvREbm4swyXGMczcMRfqgalFXhUD2wz8W3XAM5Cq2\n" +
                    "              03XeJbj6TwjvKatG5XPdeUe2FBGuOO2q54L1hcIGnLMCQrg7D31lR13PJbjnJ0No\n" +
                    "              5C3k8TPuny6vJsBC03GNLNKfmrKVTdzr3VKp1uay1G3DL9314fgmbl8HA5iRQmy+\n" +
                    "              XInUU6/8NXZSF59p3ITAOvZQeZsbJjg5gGDip5OZo9YlAgMBAAGjWzBZMB0GA1Ud\n" +
                    "              DgQWBBRPlM4VkKZ0U4ec9GrIhFQl0hNbLDA4BgNVHREEMTAvggppZHB0ZXN0YmVk\n" +
                    "              hiFodHRwczovL2lkcHRlc3RiZWQvaWRwL3NoaWJib2xldGgwDQYJKoZIhvcNAQEL\n" +
                    "              BQADggEBAIZ0a1ov3my3ljJG588I/PHx+TxAWONWmpKbO9c/qI3Drxk4oRIffiac\n" +
                    "              ANxdvtabgIzrlk5gMMisD7oyqHJiWgKv5Bgctd8w3IS3lLl7wHX65mTKQRXniG98\n" +
                    "              NIjkvfrhe2eeJxecOqnDI8GOhIGCIqZUn8ShdM/yHjhQ2Mh0Hj3U0LlKvnmfGSQl\n" +
                    "              j0viGwbFCaNaIP3zc5UmCrdE5h8sWL3Fu7ILKM9RyFa2ILHrJScV9t623IcHffHP\n" +
                    "              IeaY/WtuapsrqRFxuQL9QFWN0FsRIdLmjTq+00+B/XnnKRKFBuWfjhHLF/uu8f+E\n" +
                    "              t6Lf23Kb8yD6ZR7dihMZAGHnYQ/hlhM=\n" +
                    "                                      </ds:X509Certificate>\n" +
                    "                                  </ds:X509Data>\n" +
                    "                          </ds:KeyInfo>\n" +
                    "\n" +
                    "                      </KeyDescriptor>\n" +
                    "                      <KeyDescriptor use=\"signing\">\n" +
                    "                          <ds:KeyInfo>\n" +
                    "                                  <ds:X509Data>\n" +
                    "                                      <ds:X509Certificate>\n" +
                    "              MIIDFDCCAfygAwIBAgIVAN3vv+b7KN5Se9m1RZsCllp/B/hdMA0GCSqGSIb3DQEB\n" +
                    "              CwUAMBUxEzARBgNVBAMMCmlkcHRlc3RiZWQwHhcNMTUxMjExMDIyMDE0WhcNMzUx\n" +
                    "              MjExMDIyMDE0WjAVMRMwEQYDVQQDDAppZHB0ZXN0YmVkMIIBIjANBgkqhkiG9w0B\n" +
                    "              AQEFAAOCAQ8AMIIBCgKCAQEAh91caeY0Q85uhaUyqFwP2bMjwMFxMzRlAoqBHd7g\n" +
                    "              u6eo4duaeLz1BaoR2XTBpNNvFR5oHH+TkKahVDGeH5+kcnIpxI8JPdsZml1srvf2\n" +
                    "              Z6dzJsulJZUdpqnngycTkGtZgEoC1vmYVky2BSAIIifmdh6s0epbHnMGLsHzMKfJ\n" +
                    "              Cb/Q6dYzRWTCPtzE2VMuQqqWgeyMr7u14x/Vqr9RPEFsgY8GIu5jzB6AyUIwrLg+\n" +
                    "              MNkv6aIdcHwxYTGL7ijfy6rSWrgBflQoYRYNEnseK0ZHgJahz4ovCag6wZAoPpBs\n" +
                    "              uYlY7lEr89Ucb6NHx3uqGMsXlDFdE4QwfDLLhCYHPvJ0uwIDAQABo1swWTAdBgNV\n" +
                    "              HQ4EFgQUAkOgED3iYdmvQEOMm6u/JmD/UTQwOAYDVR0RBDEwL4IKaWRwdGVzdGJl\n" +
                    "              ZIYhaHR0cHM6Ly9pZHB0ZXN0YmVkL2lkcC9zaGliYm9sZXRoMA0GCSqGSIb3DQEB\n" +
                    "              CwUAA4IBAQBIdd4YWlnvJjql8+zKKgmWgIY7U8DA8e6QcbAf8f8cdE33RSnjI63X\n" +
                    "              sv/y9GfmbAVAD6RIAXPFFeRYJ08GOxGI9axfNaKdlsklJ9bk4ducHqgCSWYVer3s\n" +
                    "              RQBjxyOfSTvk9YCJvdJVQRJLcCvxwKakFCsOSnV3t9OvN86Ak+fKPVB5j2fM/0fZ\n" +
                    "              Kqjn3iqgdNPTLXPsuJLJO5lITRiBa4onmVelAiCstI9PQiaEck+oAHnMTnC9JE/B\n" +
                    "              DHv3e4rwq3LznlqPw0GSd7xqNTdMDwNOWjkuOr3sGpWS8ms/ZHHXV1Vd22uPe70i\n" +
                    "              s00xrv14zLifcc8oj5DYzOhYRifRXgHX\n" +
                    "                                      </ds:X509Certificate>\n" +
                    "                                  </ds:X509Data>\n" +
                    "                          </ds:KeyInfo>\n" +
                    "\n" +
                    "                      </KeyDescriptor>\n" +
                    "                      <KeyDescriptor use=\"encryption\">\n" +
                    "                          <ds:KeyInfo>\n" +
                    "                                  <ds:X509Data>\n" +
                    "                                      <ds:X509Certificate>\n" +
                    "              MIIDEzCCAfugAwIBAgIUG6Nn1rlERS1vsi88tcdzSYX0oqAwDQYJKoZIhvcNAQEL\n" +
                    "              BQAwFTETMBEGA1UEAwwKaWRwdGVzdGJlZDAeFw0xNTEyMTEwMjIwMTRaFw0zNTEy\n" +
                    "              MTEwMjIwMTRaMBUxEzARBgNVBAMMCmlkcHRlc3RiZWQwggEiMA0GCSqGSIb3DQEB\n" +
                    "              AQUAA4IBDwAwggEKAoIBAQCBXv0o3fmT8iluyLjJ4lBAVCW+ZRVyEXPYQuRi7vfD\n" +
                    "              cO4a6d1kxiJLsaK0W88VNxjFQRr8PgDkWr28vwoH1rgk4pLsszLD48DBzD942peJ\n" +
                    "              l/S6FnsIJjmaHcBh4pbNhU4yowu63iKkvttrcZAEbpEro6Z8CziWEx8sywoaYEQG\n" +
                    "              ifPkr9ORV6Cn3txq+9gMBePG41GrtZrUGIu+xrndL0Shh4Pq0eq/9MAsVlIIXEa8\n" +
                    "              9WfH8J2kFcTOfoWtIc70b7TLZQsx4YnNcnrGLSUEcstFyPLX+Xtv5SNZF89OOIxX\n" +
                    "              VNjNvgE5DbJb9hMM4UAFqI+1bo9QqtxwThjc/sOvIxzNAgMBAAGjWzBZMB0GA1Ud\n" +
                    "              DgQWBBStTyogRPuAVG6q7yPyav1uvE+7pTA4BgNVHREEMTAvggppZHB0ZXN0YmVk\n" +
                    "              hiFodHRwczovL2lkcHRlc3RiZWQvaWRwL3NoaWJib2xldGgwDQYJKoZIhvcNAQEL\n" +
                    "              BQADggEBAFMfoOv+oISGjvamq7+Y4G7ep5vxlAPeK3RATYPYvAmyH946qZXh98ni\n" +
                    "              QXyuqZW5P5eEt86toY45IwDU5r09SKwHughEe99iiEkxh0mb2qo84qX9/qcg+kyN\n" +
                    "              jeLd/OSyolpUCEFNwOFcog7pj7Eer+6AHbwTn1Mjb5TBsKwtDMJsaxPvdj0u7M5r\n" +
                    "              xL/wHkFhn1rCo2QiojzjSlV3yLTh49iTyhE3cG+RxaNKDCxhp0jSSLX1BW/ZoPA8\n" +
                    "              +PMJEA+Q0QbyRD8aJOHN5O8jGxCa/ZzcOnYVL6AsEXoDiY3vAUYh1FUonOWw0m9H\n" +
                    "              p+tGUbGS2l873J5PrsbpeKEVR/IIoKo=\n" +
                    "                                      </ds:X509Certificate>\n" +
                    "                                  </ds:X509Data>\n" +
                    "                          </ds:KeyInfo>\n" +
                    "\n" +
                    "                      </KeyDescriptor>\n" +
                    "\n" +
                    "                      <ArtifactResolutionService Binding=\"urn:oasis:names:tc:SAML:1.0:bindings:SOAP-binding\" Location=\"https://idptestbed:8443/idp/profile/SAML1/SOAP/ArtifactResolution\" index=\"1\"/>\n" +
                    "                      <ArtifactResolutionService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\" Location=\"https://idptestbed:8443/idp/profile/SAML2/SOAP/ArtifactResolution\" index=\"2\"/>\n" +
                    "\n" +
                    "                      <!--\n" +
                    "                      <SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://idptestbed/idp/profile/SAML2/Redirect/SLO\"/>\n" +
                    "                      <SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://idptestbed/idp/profile/SAML2/POST/SLO\"/>\n" +
                    "                      <SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST-SimpleSign\" Location=\"https://idptestbed/idp/profile/SAML2/POST-SimpleSign/SLO\"/>\n" +
                    "                      <SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\" Location=\"https://idptestbed:8443/idp/profile/SAML2/SOAP/SLO\"/>\n" +
                    "                      -->\n" +
                    "\n" +
                    "\n" +
                    "                      <NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</NameIDFormat>\n" +
                    "                      <NameIDFormat>urn:mace:shibboleth:1.0:nameIdentifier</NameIDFormat>\n" +
                    "\n" +
                    "                      <SingleSignOnService Binding=\"urn:mace:shibboleth:1.0:profiles:AuthnRequest\" Location=\"https://idptestbed/idp/profile/Shibboleth/SSO\"/>\n" +
                    "                      <SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://idptestbed/idp/profile/SAML2/POST/SSO\"/>\n" +
                    "                      <SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST-SimpleSign\" Location=\"https://idptestbed/idp/profile/SAML2/POST-SimpleSign/SSO\"/>\n" +
                    "                      <SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://idptestbed/idp/profile/SAML2/Redirect/SSO\"/>\n" +
                    "\n" +
                    "                  </IDPSSODescriptor>\n" +
                    "\n" +
                    "\n" +
                    "                  <AttributeAuthorityDescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:1.1:protocol\">\n" +
                    "\n" +
                    "                      <Extensions>\n" +
                    "                          <shibmd:Scope regexp=\"false\">example.org</shibmd:Scope>\n" +
                    "                      </Extensions>\n" +
                    "\n" +
                    "                      <KeyDescriptor use=\"signing\">\n" +
                    "                          <ds:KeyInfo>\n" +
                    "                                  <ds:X509Data>\n" +
                    "                                      <ds:X509Certificate>\n" +
                    "              MIIDEzCCAfugAwIBAgIUS9SuTXwsFVVG+LjOEAbLqqT/el0wDQYJKoZIhvcNAQEL\n" +
                    "              BQAwFTETMBEGA1UEAwwKaWRwdGVzdGJlZDAeFw0xNTEyMTEwMjIwMjZaFw0zNTEy\n" +
                    "              MTEwMjIwMjZaMBUxEzARBgNVBAMMCmlkcHRlc3RiZWQwggEiMA0GCSqGSIb3DQEB\n" +
                    "              AQUAA4IBDwAwggEKAoIBAQCMAoDHx8xCIfv/6QKqt9mcHYmEJ8y2dKprUbpdcOjH\n" +
                    "              YvNPIl/lHPsUyrb+Nc+q2CDeiWjVk1mWYq0UpIwpBMuw1H6+oOqr4VQRi65pin0M\n" +
                    "              SfE0MWIaFo5FPvpvoptkHD4gvREbm4swyXGMczcMRfqgalFXhUD2wz8W3XAM5Cq2\n" +
                    "              03XeJbj6TwjvKatG5XPdeUe2FBGuOO2q54L1hcIGnLMCQrg7D31lR13PJbjnJ0No\n" +
                    "              5C3k8TPuny6vJsBC03GNLNKfmrKVTdzr3VKp1uay1G3DL9314fgmbl8HA5iRQmy+\n" +
                    "              XInUU6/8NXZSF59p3ITAOvZQeZsbJjg5gGDip5OZo9YlAgMBAAGjWzBZMB0GA1Ud\n" +
                    "              DgQWBBRPlM4VkKZ0U4ec9GrIhFQl0hNbLDA4BgNVHREEMTAvggppZHB0ZXN0YmVk\n" +
                    "              hiFodHRwczovL2lkcHRlc3RiZWQvaWRwL3NoaWJib2xldGgwDQYJKoZIhvcNAQEL\n" +
                    "              BQADggEBAIZ0a1ov3my3ljJG588I/PHx+TxAWONWmpKbO9c/qI3Drxk4oRIffiac\n" +
                    "              ANxdvtabgIzrlk5gMMisD7oyqHJiWgKv5Bgctd8w3IS3lLl7wHX65mTKQRXniG98\n" +
                    "              NIjkvfrhe2eeJxecOqnDI8GOhIGCIqZUn8ShdM/yHjhQ2Mh0Hj3U0LlKvnmfGSQl\n" +
                    "              j0viGwbFCaNaIP3zc5UmCrdE5h8sWL3Fu7ILKM9RyFa2ILHrJScV9t623IcHffHP\n" +
                    "              IeaY/WtuapsrqRFxuQL9QFWN0FsRIdLmjTq+00+B/XnnKRKFBuWfjhHLF/uu8f+E\n" +
                    "              t6Lf23Kb8yD6ZR7dihMZAGHnYQ/hlhM=\n" +
                    "                                      </ds:X509Certificate>\n" +
                    "                                  </ds:X509Data>\n" +
                    "                          </ds:KeyInfo>\n" +
                    "\n" +
                    "                      </KeyDescriptor>\n" +
                    "                      <KeyDescriptor use=\"signing\">\n" +
                    "                          <ds:KeyInfo>\n" +
                    "                                  <ds:X509Data>\n" +
                    "                                      <ds:X509Certificate>\n" +
                    "              MIIDFDCCAfygAwIBAgIVAN3vv+b7KN5Se9m1RZsCllp/B/hdMA0GCSqGSIb3DQEB\n" +
                    "              CwUAMBUxEzARBgNVBAMMCmlkcHRlc3RiZWQwHhcNMTUxMjExMDIyMDE0WhcNMzUx\n" +
                    "              MjExMDIyMDE0WjAVMRMwEQYDVQQDDAppZHB0ZXN0YmVkMIIBIjANBgkqhkiG9w0B\n" +
                    "              AQEFAAOCAQ8AMIIBCgKCAQEAh91caeY0Q85uhaUyqFwP2bMjwMFxMzRlAoqBHd7g\n" +
                    "              u6eo4duaeLz1BaoR2XTBpNNvFR5oHH+TkKahVDGeH5+kcnIpxI8JPdsZml1srvf2\n" +
                    "              Z6dzJsulJZUdpqnngycTkGtZgEoC1vmYVky2BSAIIifmdh6s0epbHnMGLsHzMKfJ\n" +
                    "              Cb/Q6dYzRWTCPtzE2VMuQqqWgeyMr7u14x/Vqr9RPEFsgY8GIu5jzB6AyUIwrLg+\n" +
                    "              MNkv6aIdcHwxYTGL7ijfy6rSWrgBflQoYRYNEnseK0ZHgJahz4ovCag6wZAoPpBs\n" +
                    "              uYlY7lEr89Ucb6NHx3uqGMsXlDFdE4QwfDLLhCYHPvJ0uwIDAQABo1swWTAdBgNV\n" +
                    "              HQ4EFgQUAkOgED3iYdmvQEOMm6u/JmD/UTQwOAYDVR0RBDEwL4IKaWRwdGVzdGJl\n" +
                    "              ZIYhaHR0cHM6Ly9pZHB0ZXN0YmVkL2lkcC9zaGliYm9sZXRoMA0GCSqGSIb3DQEB\n" +
                    "              CwUAA4IBAQBIdd4YWlnvJjql8+zKKgmWgIY7U8DA8e6QcbAf8f8cdE33RSnjI63X\n" +
                    "              sv/y9GfmbAVAD6RIAXPFFeRYJ08GOxGI9axfNaKdlsklJ9bk4ducHqgCSWYVer3s\n" +
                    "              RQBjxyOfSTvk9YCJvdJVQRJLcCvxwKakFCsOSnV3t9OvN86Ak+fKPVB5j2fM/0fZ\n" +
                    "              Kqjn3iqgdNPTLXPsuJLJO5lITRiBa4onmVelAiCstI9PQiaEck+oAHnMTnC9JE/B\n" +
                    "              DHv3e4rwq3LznlqPw0GSd7xqNTdMDwNOWjkuOr3sGpWS8ms/ZHHXV1Vd22uPe70i\n" +
                    "              s00xrv14zLifcc8oj5DYzOhYRifRXgHX\n" +
                    "                                      </ds:X509Certificate>\n" +
                    "                                  </ds:X509Data>\n" +
                    "                          </ds:KeyInfo>\n" +
                    "\n" +
                    "                      </KeyDescriptor>\n" +
                    "                      <KeyDescriptor use=\"encryption\">\n" +
                    "                          <ds:KeyInfo>\n" +
                    "                                  <ds:X509Data>\n" +
                    "                                      <ds:X509Certificate>\n" +
                    "              MIIDEzCCAfugAwIBAgIUG6Nn1rlERS1vsi88tcdzSYX0oqAwDQYJKoZIhvcNAQEL\n" +
                    "              BQAwFTETMBEGA1UEAwwKaWRwdGVzdGJlZDAeFw0xNTEyMTEwMjIwMTRaFw0zNTEy\n" +
                    "              MTEwMjIwMTRaMBUxEzARBgNVBAMMCmlkcHRlc3RiZWQwggEiMA0GCSqGSIb3DQEB\n" +
                    "              AQUAA4IBDwAwggEKAoIBAQCBXv0o3fmT8iluyLjJ4lBAVCW+ZRVyEXPYQuRi7vfD\n" +
                    "              cO4a6d1kxiJLsaK0W88VNxjFQRr8PgDkWr28vwoH1rgk4pLsszLD48DBzD942peJ\n" +
                    "              l/S6FnsIJjmaHcBh4pbNhU4yowu63iKkvttrcZAEbpEro6Z8CziWEx8sywoaYEQG\n" +
                    "              ifPkr9ORV6Cn3txq+9gMBePG41GrtZrUGIu+xrndL0Shh4Pq0eq/9MAsVlIIXEa8\n" +
                    "              9WfH8J2kFcTOfoWtIc70b7TLZQsx4YnNcnrGLSUEcstFyPLX+Xtv5SNZF89OOIxX\n" +
                    "              VNjNvgE5DbJb9hMM4UAFqI+1bo9QqtxwThjc/sOvIxzNAgMBAAGjWzBZMB0GA1Ud\n" +
                    "              DgQWBBStTyogRPuAVG6q7yPyav1uvE+7pTA4BgNVHREEMTAvggppZHB0ZXN0YmVk\n" +
                    "              hiFodHRwczovL2lkcHRlc3RiZWQvaWRwL3NoaWJib2xldGgwDQYJKoZIhvcNAQEL\n" +
                    "              BQADggEBAFMfoOv+oISGjvamq7+Y4G7ep5vxlAPeK3RATYPYvAmyH946qZXh98ni\n" +
                    "              QXyuqZW5P5eEt86toY45IwDU5r09SKwHughEe99iiEkxh0mb2qo84qX9/qcg+kyN\n" +
                    "              jeLd/OSyolpUCEFNwOFcog7pj7Eer+6AHbwTn1Mjb5TBsKwtDMJsaxPvdj0u7M5r\n" +
                    "              xL/wHkFhn1rCo2QiojzjSlV3yLTh49iTyhE3cG+RxaNKDCxhp0jSSLX1BW/ZoPA8\n" +
                    "              +PMJEA+Q0QbyRD8aJOHN5O8jGxCa/ZzcOnYVL6AsEXoDiY3vAUYh1FUonOWw0m9H\n" +
                    "              p+tGUbGS2l873J5PrsbpeKEVR/IIoKo=\n" +
                    "                                      </ds:X509Certificate>\n" +
                    "                                  </ds:X509Data>\n" +
                    "                          </ds:KeyInfo>\n" +
                    "\n" +
                    "                      </KeyDescriptor>\n" +
                    "\n" +
                    "                      <AttributeService Binding=\"urn:oasis:names:tc:SAML:1.0:bindings:SOAP-binding\" Location=\"https://idptestbed:8443/idp/profile/SAML1/SOAP/AttributeQuery\"/>\n" +
                    "                      <!-- <AttributeService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\" Location=\"https://idptestbed:8443/idp/profile/SAML2/SOAP/AttributeQuery\"/> -->\n" +
                    "                      <!-- If you uncomment the above you should add urn:oasis:names:tc:SAML:2.0:protocol to the protocolSupportEnumeration above -->\n" +
                    "\n" +
                    "                  </AttributeAuthorityDescriptor>\n" +
                    "\n" +
                    "              </EntityDescriptor>";
            LocalServiceProviderConfiguration sp = samlConfiguration.getServiceProvider();
            ExternalIdentityProviderConfiguration newProvider = new ExternalIdentityProviderConfiguration();
            newProvider.setAlias("new_shibboleth");
            newProvider.setMetadata(metadata);
            newProvider.setSkipSslValidation(true);
            newProvider.setLinktext("new Shibboleth");
            newProvider.setAuthenticationRequestBinding(new URI("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"));
            sp.getProviders().add(newProvider);
            samlConfiguration.setServiceProvider(sp);
        }
        i++;
        SamlWebSecurity webSec = objectObjectPostProcessor.postProcess(new SamlWebSecurity(samlProviderServerBeanConfiguration, samlConfiguration));
        webSec.setObjectPostProcessor(objectObjectPostProcessor);
        webSec.setApplicationContext(context);
//        try {
//            webSec.setTrustResolver(context.getBean(AuthenticationTrustResolver.class));
//        } catch( NoSuchBeanDefinitionException e ) {
//        }
//        try {
//            webSec.setContentNegotationStrategy(context.getBean(ContentNegotiationStrategy.class));
//        } catch( NoSuchBeanDefinitionException e ) {
//        }
//        try {
//            webSec.setAuthenticationConfiguration(context.getBean(AuthenticationConfiguration.class));
//        } catch( NoSuchBeanDefinitionException e ) {
//        }
        HttpSecurity http = webSec.getNewHttp();
        return http.build();
    }

    private SecurityFilterChain getSamlBasicFilter() throws Exception {
        DefaultWebSecurityConfig webSec = objectObjectPostProcessor.postProcess(new DefaultWebSecurityConfig());
        webSec.setObjectPostProcessor(objectObjectPostProcessor);
        webSec.setApplicationContext(context);
//        try {
//            webSec.setTrustResolver(context.getBean(AuthenticationTrustResolver.class));
//        } catch( NoSuchBeanDefinitionException e ) {
//        }
//        try {
//            webSec.setContentNegotationStrategy(context.getBean(ContentNegotiationStrategy.class));
//        } catch( NoSuchBeanDefinitionException e ) {
//        }
//        try {
//            webSec.setAuthenticationConfiguration(context.getBean(AuthenticationConfiguration.class));
//        } catch( NoSuchBeanDefinitionException e ) {
//        }
        HttpSecurity http = webSec.getNewHttp();

        http.antMatcher("/**")
                .authorizeRequests()
                .accessDecisionManager(accessDecisionManager)
                .antMatchers("/**").authenticated()
                .and()
                .formLogin().loginPage("/saml");

        http.exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler);
        http.logout().clearAuthentication(false)
                .invalidateHttpSession(false)
                .logoutUrl("/logout")
                .logoutSuccessUrl("/saml/logout");
//                .logoutSuccessHandler(auditedLogoutHandler);
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.NEVER)
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .maxSessionsPreventsLogin(true);

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("cas"))) {
            http.addFilterAt(casFilter, CasAuthenticationFilter.class);
            http.addFilterBefore(requestSingleLogoutFilter, LogoutFilter.class);
        }

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("sso"))) {
            http.addFilterBefore(requestHeaderAuthenticationFilter, LogoutFilter.class);
        }

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("ssoenv"))) {
            http.addFilterBefore(requestAttributeAuthenticationFilter, LogoutFilter.class);
        }

//        if (!csrfEnabled) {
//            http.csrf().disable();
//        }
//        http.headers().disable();
//        http.headers().frameOptions().sameOrigin();

        return http.build();
    }

    private String readFile() throws IOException {
        String path = "/home/lskublik/test";
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.US_ASCII);
    }

    private SecurityFilterChain getInternalPasswordFilter() throws Exception {
        DefaultWebSecurityConfig webSec = initDefaultPasswordWebSecurityConfig();
        HttpSecurity http = webSec.getNewHttp();
        http.antMatcher("/internal/**")
                .authorizeRequests()
                .accessDecisionManager(accessDecisionManager)
                .anyRequest().fullyAuthenticated();
        http.formLogin()
                .loginPage("/internal/login")
                .loginProcessingUrl("/internal/spring_security_login")
                .successHandler(objectObjectPostProcessor.postProcess(new MidPointInternalAuthenticationSuccessHandler())).permitAll();
        http.exceptionHandling()
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/internal/login"));
        configureDefaultPasswordFilter(http);
        SecurityFilterChain filter = http.build();
        return filter;
    }

    private DefaultWebSecurityConfig initDefaultPasswordWebSecurityConfig(){
        DefaultWebSecurityConfig webSec = objectObjectPostProcessor.postProcess(new DefaultWebSecurityConfig());
        webSec.setObjectPostProcessor(objectObjectPostProcessor);
        webSec.setApplicationContext(context);
        webSec.addAuthenticationProvider(midPointAuthenticationProvider);
//        try {
//            webSec.setTrustResolver(context.getBean(AuthenticationTrustResolver.class));
//        } catch( NoSuchBeanDefinitionException e ) {
//        }
//        try {
//            webSec.setContentNegotationStrategy(context.getBean(ContentNegotiationStrategy.class));
//        } catch( NoSuchBeanDefinitionException e ) {
//        }
//        try {
//            webSec.setAuthenticationConfiguration(context.getBean(AuthenticationConfiguration.class));
//        } catch( NoSuchBeanDefinitionException e ) {
//        }
        return webSec;
    }

    private SecurityFilterChain getBasicPasswordFilter() throws Exception {
        DefaultWebSecurityConfig webSec = initDefaultPasswordWebSecurityConfig();
        HttpSecurity http = webSec.getNewHttp();
        http.authorizeRequests()
                .accessDecisionManager(accessDecisionManager)
                .antMatchers("/j_spring_security_check",
                        "/spring_security_login",
                        "/login",
                        "/forgotpassword",
                        "/registration",
                        "/confirm/registration",
                        "/confirm/reset",
                        "/error",
                        "/error/*",
                        "/bootstrap").permitAll()
                .anyRequest().fullyAuthenticated();
        http.formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/spring_security_login")
                .successHandler(authenticationSuccessHandler).permitAll();
        http.exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint);
        configureDefaultPasswordFilter(http);
        SecurityFilterChain filter = http.build();
        return filter;
    }

    private void configureDefaultPasswordFilter(HttpSecurity http) throws Exception {
        http.exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler);
        http.logout().clearAuthentication(true)
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(auditedLogoutHandler);
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.NEVER)
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .maxSessionsPreventsLogin(true);

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("cas"))) {
            http.addFilterAt(casFilter, CasAuthenticationFilter.class);
            http.addFilterBefore(requestSingleLogoutFilter, LogoutFilter.class);
        }

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("sso"))) {
            http.addFilterBefore(requestHeaderAuthenticationFilter, LogoutFilter.class);
        }

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("ssoenv"))) {
            http.addFilterBefore(requestAttributeAuthenticationFilter, LogoutFilter.class);
        }

        if (!csrfEnabled) {
            http.csrf().disable();
        }
        http.headers().disable();
        http.headers().frameOptions().sameOrigin();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {


        String actualAuthentication = readFile();
        if (!actualAuthentication.equals(lastAuthentication)) {
            lastAuthentication = actualAuthentication;
            if (filterChains != null) {
                filterChains.clear();
            }
            List<SecurityFilterChain> filters = new ArrayList<SecurityFilterChain>();
            filters.addAll(filterChainsIgnoredPath);

            if (actualAuthentication.equals("saml\n")) {
                try {
                    SecurityFilterChain filter = getInternalPasswordFilter();
                    filters.add(filter);
                } catch (Exception e) {
                    LOGGER.error("Couldn't build internal password filter", e);
                }

                try {
                    SecurityFilterChain filter = getSamlFilter();
                    filters.add(filter);
                } catch (Exception e) {
                    LOGGER.error("Couldn't build saml basic filter", e);
                }

                try {
                    SecurityFilterChain filter = getSamlBasicFilter();
                    filters.add(filter);
                } catch (Exception e) {
                    LOGGER.error("Couldn't build saml filter", e);
                }
            } else {

                try {
                    SecurityFilterChain filter = getSamlFilter();
                    filters.add(filter);
                } catch (Exception e) {
                    LOGGER.error("Couldn't build saml basic filter", e);
                }

                try {
                    SecurityFilterChain filter = getBasicPasswordFilter();
                    filters.add(filter);
                } catch (Exception e) {
                    LOGGER.error("Couldn't build basic password filter", e);
                }
            }

            this.filterChains = filters;
        }

        boolean clearContext = request.getAttribute(FILTER_APPLIED) == null;
        if (clearContext) {
            try {
                request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
                doFilterInternal(request, response, chain);
            }
            finally {
                SecurityContextHolder.clearContext();
                request.removeAttribute(FILTER_APPLIED);
            }
        }
        else {
            doFilterInternal(request, response, chain);
        }
    }

    private void doFilterInternal(ServletRequest request, ServletResponse response,
                                  FilterChain chain) throws IOException, ServletException {

        FirewalledRequest fwRequest = firewall
                .getFirewalledRequest((HttpServletRequest) request);
        HttpServletResponse fwResponse = firewall
                .getFirewalledResponse((HttpServletResponse) response);

        List<Filter> filters = getFilters(fwRequest);

        if (filters == null || filters.size() == 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(UrlUtils.buildRequestUrl(fwRequest)
                        + (filters == null ? " has no matching filters"
                        : " has an empty filter list"));
            }

            fwRequest.reset();

            chain.doFilter(fwRequest, fwResponse);

            return;
        }



        MidpointFilterChainProxy.VirtualFilterChain vfc = new MidpointFilterChainProxy.VirtualFilterChain(fwRequest, chain, filters);
        vfc.doFilter(fwRequest, fwResponse);
    }

    private List<Filter> getFilters(HttpServletRequest request) {
        for (SecurityFilterChain chain : filterChains) {
            if (chain.matches(request)) {
                return chain.getFilters();
            }
        }

        return null;
    }

    public List<Filter> getFilters(String url) {
        return getFilters(firewall.getFirewalledRequest((new FilterInvocation(url, "GET")
                .getRequest())));
    }

    public List<SecurityFilterChain> getFilterChains() {
        return Collections.unmodifiableList(filterChains);
    }

    public void setFilterChainValidator(MidpointFilterChainProxy.FilterChainValidator filterChainValidator) {
        this.filterChainValidator = filterChainValidator;
    }

    public void setFirewall(HttpFirewall firewall) {
        this.firewall = firewall;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FilterChainProxy[");
        sb.append("Filter Chains: ");
        sb.append(filterChains);
        sb.append("]");

        return sb.toString();
    }

    private static class VirtualFilterChain implements FilterChain {
        private final FilterChain originalChain;
        private final List<Filter> additionalFilters;
        private final FirewalledRequest firewalledRequest;
        private final int size;
        private int currentPosition = 0;

        private VirtualFilterChain(FirewalledRequest firewalledRequest,
                                   FilterChain chain, List<Filter> additionalFilters) {
            this.originalChain = chain;
            this.additionalFilters = additionalFilters;
            this.size = additionalFilters.size();
            this.firewalledRequest = firewalledRequest;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {
            if (currentPosition == size) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl(firewalledRequest)
                            + " reached end of additional filter chain; proceeding with original chain");
                }

                // Deactivate path stripping as we exit the security filter chain
                this.firewalledRequest.reset();

                originalChain.doFilter(request, response);
            }
            else {
                currentPosition++;

                Filter nextFilter = additionalFilters.get(currentPosition - 1);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl(firewalledRequest)
                            + " at position " + currentPosition + " of " + size
                            + " in additional filter chain; firing Filter: '"
                            + nextFilter.getClass().getSimpleName() + "'");
                }

                nextFilter.doFilter(request, response, this);
            }
        }
    }

    public interface FilterChainValidator {
        void validate(MidpointFilterChainProxy filterChainProxy);
    }

    private static class NullFilterChainValidator implements MidpointFilterChainProxy.FilterChainValidator {
        @Override
        public void validate(MidpointFilterChainProxy filterChainProxy) {
        }
    }

}
