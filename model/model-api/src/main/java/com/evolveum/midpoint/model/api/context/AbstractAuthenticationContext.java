/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

public abstract class AbstractAuthenticationContext {

    // TODO which ones can be null and which ones cannot?

    private final String username;

    private final Class<? extends FocusType> principalType;

    private final List<ObjectReferenceType> requireAssignments;

    private boolean supportActivationByChannel = true;

    public AbstractAuthenticationContext(
            String username, Class<? extends FocusType> principalType, List<ObjectReferenceType> requireAssignment) {
        this.username = username;
        this.requireAssignments = requireAssignment;
        this.principalType = principalType;
    }

    public String getUsername() {
        return username;
    }

    public Class<? extends FocusType> getPrincipalType() {
        return principalType;
    }

    public void setSupportActivationByChannel(boolean supportActivationByChannel) {
        this.supportActivationByChannel = supportActivationByChannel;
    }

    public boolean isSupportActivationByChannel() {
        return supportActivationByChannel;
    }

    public List<ObjectReferenceType> getRequireAssignments() {
        return requireAssignments;
    }

    public abstract Object getEnteredCredential();

    public ObjectQuery createFocusQuery() {
        if (username != null) {
            PolyString usernamePoly = new PolyString(username);
            return ObjectQueryUtil.createNormNameQuery(usernamePoly, PrismContext.get());
        }
        throw new UnsupportedOperationException("Username not specified, probably more concrete context should override this method");
    }

}
