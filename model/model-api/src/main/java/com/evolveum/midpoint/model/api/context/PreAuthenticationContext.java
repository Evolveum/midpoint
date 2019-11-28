/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

/**
 * @author skublik
 */

public class PreAuthenticationContext extends AbstractAuthenticationContext {

    public PreAuthenticationContext(String username){
        super(username);
    }

    public PreAuthenticationContext(String username, List<ObjectReferenceType> requireAssignment) {
        super(username, requireAssignment);
    }

    @Override
    public Object getEnteredCredential() {
        throw new UnsupportedOperationException();
    }
}
