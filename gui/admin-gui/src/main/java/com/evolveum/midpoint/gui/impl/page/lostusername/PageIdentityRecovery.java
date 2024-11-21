/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.lostusername;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.CorrelationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.login.PageSelfRegistration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.page.self.PageSelf;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/identityRecovery", matchUrlForSecurity = "/identityRecovery")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_IDENTITY_RECOVERY_URL) })
public class PageIdentityRecovery extends AbstractPageLogin {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageIdentityRecovery.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageIdentityRecovery.class);
    private static final String OPERATION_GET_SECURITY_POLICY = DOT_CLASS + "getSecurityPolicy";
    private static final String OPERATION_AUDIT_FOUND_IDENTITIES = DOT_CLASS + "auditFoundIdentities";

    private static final String ID_RECOVERED_IDENTITIES = "recoveredIdentities";
    private static final String ID_DETAILS_PANEL = "detailsPanel";
    private static final String ID_REGISTRATION_LINK = "registrationLink";
    private static final String ID_RESTART_FLOW_LINK = "restartFlow";
    private static final String ID_PAGING = "paging";

    private LoadableModel<List<UserType>> recoveredIdentitiesModel;
    private LoadableModel<SecurityPolicyType> securityPolicyModel;
    private boolean isAudited;

    private static final int IDENTITY_PER_PAGE = 3;

    public PageIdentityRecovery() {
        super();
        initModels();
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected void initCustomLayout() {
        PageableListView<UserType> recoveredIdentitiesPanel = new PageableListView<>(ID_RECOVERED_IDENTITIES,
                recoveredIdentitiesModel, IDENTITY_PER_PAGE) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onAfterRender() {
                super.onAfterRender();
                if (!isAudited) {
                    auditInformationDisclosure();
                    isAudited = true;
                }
            }

            @Override
            protected void populateItem(ListItem<UserType> item) {
                boolean isFirstItem = item.getIndex() == 0;
                IdentityDetailsPanel<UserType> detailsPanel = new IdentityDetailsPanel<>(ID_DETAILS_PANEL, item.getModel(),
                        securityPolicyModel.getObject(), isSingleRecoveredIdentity() || isFirstItem);
                detailsPanel.setOutputMarkupId(true);
                item.add(detailsPanel);
            }
        };
        recoveredIdentitiesPanel.setOutputMarkupId(true);
        add(recoveredIdentitiesPanel);

        NavigatorPanel paging = new NavigatorPanel(ID_PAGING, recoveredIdentitiesPanel, true) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String getPaginationCssClass() {
                return null;
            }
        };
        paging.add(new VisibleBehaviour(() -> !singlePageResult()));
        add(paging);

        AjaxLink<String> restartFlowLink = new AjaxLink<>(ID_RESTART_FLOW_LINK) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                AuthUtil.clearMidpointAuthentication();
                String identityRecoveryUrl = SecurityUtils.getIdentityRecoveryUrl(securityPolicyModel.getObject());
                throw new RedirectToUrlException(identityRecoveryUrl);
            }
        };
        add(restartFlowLink);

        String urlRegistration = SecurityUtils.getRegistrationUrl(securityPolicyModel.getObject());
        AjaxLink<String> registrationLink = new AjaxLink<String>(ID_REGISTRATION_LINK) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                var user = (UserType) SecurityUtils.findCorrelationModuleAuthentication(PageIdentityRecovery.this).getPreFocus();
                PageSelfRegistration p  = new PageSelfRegistration(user);
                AuthUtil.clearMidpointAuthentication();
                setResponsePage(p);
            }
        };
        registrationLink.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(urlRegistration)));
        add(registrationLink);
    }

    private void initModels() {
        recoveredIdentitiesModel = new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<UserType> load() {
                return getRecoveredIdentities();
            }
        };
        securityPolicyModel = new LoadableModel<>(false) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected SecurityPolicyType load() {
                var archetypeOid = getMidpointAuthentication().getArchetypeOid();
                return runPrivileged((Producer<SecurityPolicyType>) () -> {
                    var task = createAnonymousTask(OPERATION_GET_SECURITY_POLICY);
                    var result = new OperationResult(OPERATION_GET_SECURITY_POLICY);
                    try {
                        return getModelInteractionService().getSecurityPolicyForArchetype(archetypeOid, task, result);
                    } catch (Exception e) {
                        LOGGER.debug("Unable to load the configured items list for identity recovery page, ", e);
                    }
                    return null;
                });
            }
        };
    }

    @Override
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageIdentityRecovery.foundIdentities");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return createStringResource(getTitleDescriptionKey());
    }

    private String getTitleDescriptionKey() {
        if (recoveredIdentitiesExist()) {
            return "PageIdentityRecovery.title.success.description";
        }
        return "PageIdentityRecovery.title.fail.description";
    }

    private boolean isSingleRecoveredIdentity() {
        List<UserType> recoveredIdentities = getRecoveredIdentities();
        return recoveredIdentities != null && recoveredIdentities.size() == 1;
    }

    private boolean recoveredIdentitiesExist() {
        return CollectionUtils.isNotEmpty(getRecoveredIdentities());
    }

    private List<UserType> getRecoveredIdentities() {
        var correlationModuleAuth = SecurityUtils.findCorrelationModuleAuthentication(PageIdentityRecovery.this);
        if (isSuccessfullyAuthenticated(correlationModuleAuth)) {
            return correlationModuleAuth.getOwners()
                    .stream()
                    .filter(o -> o instanceof UserType)
                    .map(o -> (UserType) o)
                    .sorted((o1, o2) -> {
                        String name1 = WebComponentUtil.getDisplayNameOrName(o1.asPrismObject());
                        String name2 = WebComponentUtil.getDisplayNameOrName(o2.asPrismObject());
                        return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                    })
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean isSuccessfullyAuthenticated(CorrelationModuleAuthentication auth) {
        return auth != null && AuthenticationModuleState.SUCCESSFULLY.equals(auth.getState());
    }

    private MidpointAuthentication getMidpointAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            getSession().error(getString("No midPoint authentication is found"));
            throw new RestartResponseException(PageError.class);
        }
        return (MidpointAuthentication) authentication;
    }

    private boolean singlePageResult() {
        var userList = recoveredIdentitiesModel.getObject();
        int userCount = userList != null ? userList.size() : 0;
        return userCount <= IDENTITY_PER_PAGE;
    }

    private void auditInformationDisclosure() {
        OperationResult result = new OperationResult(OPERATION_AUDIT_FOUND_IDENTITIES);
        Task task = createAnonymousTask(OPERATION_AUDIT_FOUND_IDENTITIES);
        var principal = getMidpointAuthentication().getPrincipal();
        if (!(principal instanceof MidPointPrincipal mpPrincipal)) {
            LOGGER.error(getString("No midPoint principal is found"));
            throw new RestartResponseException(PageError.class);
        }
        try {
            PrismObject<UserType> administrator = getAdministratorPrivileged(result);
            runAsChecked(
                    (lResult) -> {
                        AuditEventRecord record = createAuditRecord(mpPrincipal.getFocusPrismObject());
                        getModelAuditService().audit(record, task, result);
                        return null;
                    },
                    administrator, result);
        } catch (Exception e) {
            LOGGER.error(getString("Unable to audit found identities, ", e));
        }
    }

    private AuditEventRecord createAuditRecord(PrismObject<? extends FocusType> principal) {
        AuditEventRecord record = new AuditEventRecord(AuditEventType.INFORMATION_DISCLOSURE);
        record.setInitiatorAndLoginParameter(principal);
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(OperationResultStatus.SUCCESS);

        record.setChannel(SchemaConstants.CHANNEL_IDENTITY_RECOVERY_URI);
        record.setSessionIdentifier(getSession().getId());
        HttpConnectionInformation connInfo = SecurityUtil.getCurrentConnectionInformation();
        if (connInfo != null) {
            record.setRemoteHostAddress(connInfo.getRemoteHostAddress());
            record.setHostIdentifier(connInfo.getLocalHostName());
        }
        record.addDeltas(createFoundIdentitiesDeltas());
        return record;
    }

    private List<ObjectDeltaOperation<UserType>> createFoundIdentitiesDeltas() {
        return recoveredIdentitiesModel.getObject()
                .stream()
                .map(this::createUserDeltaOperation)
                .collect(Collectors.toList());
    }

    private ObjectDeltaOperation<UserType> createUserDeltaOperation(UserType user) {
        ObjectDeltaOperation<UserType> deltaOperation = new ObjectDeltaOperation<>();
        deltaOperation.setObjectName(user.getName().toPolyString());
        ObjectDelta<UserType> delta = getPrismContext().deltaFactory().object().create(UserType.class, ChangeType.MODIFY);
        delta.setOid(user.getOid());
        deltaOperation.setObjectDelta(delta);
        deltaOperation.setObjectOid(user.getOid());
        return deltaOperation;
    }

}
