/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.evaluator.context;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public abstract class AbstractAuthenticationContext {

    // TODO which ones can be null and which ones cannot?

    private final String username;

    private final Class<? extends FocusType> principalType;

    private final List<ObjectReferenceType> requireAssignments;

    private final boolean supportActivationByChannel;

    public AbstractAuthenticationContext(
            String username,
            Class<? extends FocusType> principalType,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel) {
        this.username = username;
        this.requireAssignments = requireAssignment;
        this.principalType = principalType;
        this.supportActivationByChannel = channel == null || channel.isSupportActivationByChannel();
    }

    public String getUsername() {
        return username;
    }

    public Class<? extends FocusType> getPrincipalType() {
        return principalType;
    }

    public boolean isSupportActivationByChannel() {
        return supportActivationByChannel;
    }

    public List<ObjectReferenceType> getRequireAssignments() {
        return requireAssignments;
    }

    public abstract Object getEnteredCredential();

    public ObjectQuery createFocusQuery() {
        if (StringUtils.isNotBlank(username)) {
            PolyString usernamePoly = new PolyString(username);
            return ObjectQueryUtil.createNormNameQuery(usernamePoly, PrismContext.get());
        }
        return null;
    }

}
