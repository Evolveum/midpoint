/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.impl.handlers;

import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationRunType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author mederly
 */
@Component
public abstract class BaseCertificationHandler implements CertificationHandler {

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected SecurityEnforcer securityEnforcer;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected CertificationManagerImpl certificationManager;

    @Override
    public AccessCertificationRunType createCertificationRunType(AccessCertificationTypeType typeType, AccessCertificationRunType runType, Task task, OperationResult result) throws SecurityViolationException {
        AccessCertificationRunType newRunType = new AccessCertificationRunType(prismContext);
        Date now = new Date();

        if (runType != null && runType.getName() != null) {
            newRunType.setName(runType.getName());
        } else {
            newRunType.setName(new PolyStringType("Run of " + typeType.getName().getOrig() + " started " + now));
        }

        if (runType != null && runType.getDescription() != null) {
            newRunType.setDescription(runType.getDescription());
        } else {
            newRunType.setDescription(typeType.getDescription());
        }

        if (runType != null && runType.getOwnerRef() != null) {
            newRunType.setOwnerRef(runType.getOwnerRef());
        } else if (typeType.getOwnerRef() != null) {
            newRunType.setOwnerRef(typeType.getOwnerRef());
        } else {
            newRunType.setOwnerRef(securityEnforcer.getPrincipal().toObjectReference());
        }

        if (runType != null && runType.getScope() != null) {
            newRunType.setScope(runType.getScope());
        } else {
            typeType.setScope(typeType.getScope());
        }

        if (runType != null && runType.getTenantRef() != null) {
            newRunType.setTenantRef(runType.getTenantRef());
        } else {
            newRunType.setTenantRef(typeType.getTenantRef());
        }

        ObjectReferenceType typeRef = new ObjectReferenceType();
        typeRef.setType(AccessCertificationTypeType.COMPLEX_TYPE);
        typeRef.setOid(typeType.getOid());
        newRunType.setTypeRef(typeRef);

        return newRunType;
    }

    protected ObjectReferenceType createRunRef(AccessCertificationRunType runType) {
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setType(AccessCertificationRunType.COMPLEX_TYPE);
        ort.setOid(runType.getOid());
        return ort;
    }
}
