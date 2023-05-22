/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;

import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * TODO this is just work in progress, almost empty now [viliam]
 */
@Component
public class AuditHelper {

    @Autowired
    private AuditService auditService;

    public void auditAddShadow(ShadowType shadow, OperationResult result) {
        AuditEventRecord record = new AuditEventRecord()
    }

    public void auditModifyShadow(ShadowType shadow, (Collection<Operation> operations, OperationResult result) {

    }

    public void auditDeleteShadow(ShadowType shadow, OperationResult result) {

    }
}
