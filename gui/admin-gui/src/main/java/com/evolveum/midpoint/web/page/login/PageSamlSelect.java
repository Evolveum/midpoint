/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.forgetpassword.PageForgotPassword;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.saml.provider.config.ExternalProviderConfiguration;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ModelProvider;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author skublik
 */
@PageDescriptor(urls = {
//                    @Url(mountUrl = "/saml/", matchUrlForSecurity = "/saml/"),
                    @Url(mountUrl = "/saml", matchUrlForSecurity = "/saml")
                }, permitAll = false)
public class PageSamlSelect extends PageBase implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSamlSelect.class);

    @SpringBean(name = "samlServiceProviderProvisioning")
    SamlProviderProvisioning<ServiceProviderService> samlProvisioning;

    public PageSamlSelect() {
        List<Provider> providers = getProviders();
        add(new ListView<Provider>("providers", providers) {
            @Override
            protected void populateItem(ListItem<Provider> item) {
                item.add(new ExternalLink("provider", item.getModelObject().getRedirectLink(), item.getModelObject().getLinkText()));
            }
        });

    }

    private List<Provider> getProviders(){
        ServiceProviderService provider = samlProvisioning.getHostedProvider();
        LocalServiceProviderConfiguration configuration = provider.getConfiguration();
        List<Provider> providers = new ArrayList<>();
        configuration.getProviders().stream().forEach(
                p -> {
                    try {
                        Provider mp = new Provider()
                                .setLinkText(p.getLinktext())
                                .setRedirectLink(getDiscoveryRedirect(provider, p));
                        providers.add(mp);
                    } catch (Exception x) {
                        LOGGER.debug("Unable to retrieve metadata for provider:" + p.getMetadata() + " with message:" + x.getMessage());
                    }
                }
        );
        return providers;
    }

    private String getDiscoveryRedirect(ServiceProviderService provider,
                                          ExternalProviderConfiguration p) throws UnsupportedEncodingException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                provider.getConfiguration().getBasePath()
        );
        builder.pathSegment(stripSlashes(provider.getConfiguration().getPrefix()) + "/discovery");
        builder.pathSegment("saml/discovery");
        IdentityProviderMetadata metadata = provider.getRemoteProvider(p);
        builder.queryParam("idp", UriUtils.encode(metadata.getEntityId(), UTF_8.toString()));
        return builder.build().toUriString();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        ServletWebRequest req = (ServletWebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = req.getContainerRequest();
        HttpSession httpSession = httpReq.getSession();

        Exception ex = (Exception) httpSession.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (ex == null) {
            return;
        }

        String msg = ex.getMessage();
        if (StringUtils.isEmpty(msg)) {
            msg = "web.security.provider.unavailable";
        }

        msg = getLocalizationService().translate(msg, null, getLocale(), msg);
        error(msg);

        httpSession.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        clearBreadcrumbs();
    }

    @Override
    protected void createBreadcrumb() {
        //don't create breadcrumb for login page
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (SecurityUtils.getPrincipalUser() != null) {
            MidPointApplication app = getMidpointApplication();
            throw new RestartResponseException(app.getHomePage());
        }
    }

    @Override
    protected boolean isSideMenuVisible(boolean visibleIfLoggedIn) {
        return false;
    }

    private class Provider implements Serializable{
        private static final long serialVersionUID = 1L;

        private String linkText = "";
        private String redirectLink = "";

        public String getLinkText() {
            return linkText;
        }

        public String getRedirectLink() {
            return redirectLink;
        }

        public Provider setLinkText(String linkText) {
            this.linkText = linkText;
            return this;
        }

        public Provider setRedirectLink(String redirectLink) {
            this.redirectLink = redirectLink;
            return this;
        }
    }
}
