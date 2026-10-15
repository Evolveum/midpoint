/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.channel;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Channel for the account activation flow (issue 5490). The user opens the activation link from a notification,
 * authenticates by the mail nonce and by the password, and is then redirected to the account activation page.
 *
 * The channel grants exactly what the activation page does: access to the page, reading own shadows and setting
 * password and purpose on them. Anything else that happens during the activation in a particular deployment
 * (changes derived for the user by mappings, notifications reading other objects) is for the administrator to
 * authorize by roles. Assigned authorizations are kept in this channel, except for other UI pages.
 */
public class AccountActivationAuthenticationChannel extends AuthenticationChannelImpl {

    public AccountActivationAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    @Override
    public String getChannelId() {
        return SchemaConstants.CHANNEL_ACCOUNT_ACTIVATION_URI;
    }

    @Override
    public String getPathAfterSuccessfulAuthentication() {
        return SchemaConstants.ACCOUNT_ACTIVATION_PREFIX;
    }

    @Override
    public String getPathAfterUnsuccessfulAuthentication() {
        return "/";
    }

    @Override
    public Authorization resolveAuthorization(Authorization autz) {
        if (autz == null) {
            return null;
        }
        Authorization retAutz = autz.clone();
        retAutz.getAction().removeIf(action ->
                !AuthorizationConstants.AUTZ_UI_ACCOUNT_ACTIVATION_URL.equals(action)
                        && action.contains(AuthorizationConstants.NS_AUTHORIZATION_UI));
        if (retAutz.getAction().isEmpty()) {
            return null;
        }
        return retAutz;
    }

    @Override
    public @NotNull List<Authorization> getAdditionalAuthorities() {
        return List.of(
                new Authorization(new AuthorizationType()
                        .name("account-activation-ui")
                        .action(AuthorizationConstants.AUTZ_UI_ACCOUNT_ACTIVATION_URL)),
                new Authorization(new AuthorizationType()
                        .name("account-activation-read-own-shadows")
                        .action(ModelAuthorizationAction.READ.getUrl())
                        .object(ownShadows())),
                new Authorization(new AuthorizationType()
                        .name("account-activation-activate-own-shadows")
                        .action(ModelAuthorizationAction.MODIFY.getUrl())
                        .object(ownShadows())
                        .item(new ItemPathType(ItemPath.create(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)))
                        .item(new ItemPathType(ShadowType.F_PURPOSE))));
    }

    static OwnedObjectSelectorType ownShadows() {
        return new OwnedObjectSelectorType()
                .type(ShadowType.COMPLEX_TYPE)
                .owner(new SubjectedObjectSelectorType().special(SpecialObjectSpecificationType.SELF));
    }
}
