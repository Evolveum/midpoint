/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.forgotpassword;

import java.io.Serializable;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsResetPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

public class ResetPolicyDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private ObjectReferenceType formRef;

    public void initResetPolicyDto(SecurityPolicyType securityPolicyType) throws SchemaException {
        if (securityPolicyType == null) {
            return;
        }

        CredentialsResetPolicyType credReset = securityPolicyType.getCredentialsReset();

        if (credReset == null) {
            return;
        }

        this.formRef = credReset.getFormRef();
    }

    public ObjectReferenceType getFormRef() {
        return formRef;
    }
}
