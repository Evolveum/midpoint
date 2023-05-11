package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/invitation", matchUrlForSecurity = "/invitation")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_INVITATION_URL)})
public class PageInvitation extends PageSelfRegistration {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageInvitation.class);
    private SelfRegistrationDto invitationDto;

    public PageInvitation() {
    }

    @Override
    public void initializeModel() {
        userModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected UserType load() {
                PrismObject<? extends FocusType> focus = getPrincipalFocus().asPrismObject();
                if (focus == null) {
                    AuthUtil.clearMidpointAuthentication();
                    return null;
                }
                return (UserType) focus.asObjectable();
            }
        };
    }

    @Override
    protected IModel<String> getTitleModel() {
        return createStringResource("PageInvitation.title");
    }

    @Override
    protected IModel<String> getDescriptionModel() {
        return createStringResource("PageInvitation.description");
    }

    @Override
    protected boolean afterRegistrationAuthenticationNeeded() {
        return false;
    }

    @Override
    public SelfRegistrationDto getFlowConfiguration() {
        if (invitationDto == null) {
            initInvitationConfiguration();
        }

        return invitationDto;
    }

    private void initInvitationConfiguration() {

        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        invitationDto = new SelfRegistrationDto();
        try {
            invitationDto.initInvitationDto(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Failed to initialize invitation configuration.", e);
            getSession().error(
                    createStringResource("PageSelfRegistration.selfRegistration.configuration.init.failed")
                            .getString());
            throw new RestartResponseException(PageLogin.class);
        }

    }

//    @Override
//    protected ObjectDelta<UserType> prepareUserDelta(Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
//        LOGGER.trace("Preparing user MODIFY delta (user invitation)");
//        UserType userType = prepareUserToSave(task, result);
//        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(userType.asPrismObject());
//        LOGGER.trace("Going to register user {}", userDelta);
//        return userDelta;
//    }

    @Override
    protected String getChannel() {
        return SchemaConstants.CHANNEL_INVITATION_URI;
    }
}
