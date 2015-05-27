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

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationReviewerSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class AccCertReviewersHelper {

    @Autowired
    private ModelService modelService;

    // TODO temporary hack because of some problems in model service...
    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    @Autowired
    private CertificationManagerImpl certificationManager;

    @Autowired
    protected ObjectResolver objectResolver;

    public AccessCertificationReviewerSpecificationType findReviewersSpecification(AccessCertificationCampaignType campaign,
                                                                                   int stage, Task task, OperationResult result) {
        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stage);
        return stageDef.getReviewerSpecification();
    }

    public void setupReviewersForCase(AccessCertificationCaseType _case, AccessCertificationCampaignType campaign,
                                      AccessCertificationReviewerSpecificationType reviewerSpec, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        _case.getReviewerRef().clear();
        if (reviewerSpec == null) {
            return;     // TODO issue a warning here?
        }

        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetObjectOwner())) {
            cloneAndMerge(_case.getReviewerRef(), getTargetObjectOwners(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetObjectApprover())) {
            cloneAndMerge(_case.getReviewerRef(), getTargetObjectApprovers(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseSubjectOwner())) {
            cloneAndMerge(_case.getReviewerRef(), getSubjectOwners(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseSubjectApprover())) {
            cloneAndMerge(_case.getReviewerRef(), getSubjectApprovers(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseSubjectManager())) {
            cloneAndMerge(_case.getReviewerRef(), getSubjectManagers(_case, task, result));
        }
        // TODO evaluate reviewer expressions
        if (_case.getReviewerRef().isEmpty()) {
            cloneAndMerge(_case.getReviewerRef(), reviewerSpec.getDefaultReviewerRef());
        }
        cloneAndMerge(_case.getReviewerRef(), reviewerSpec.getAdditionalReviewerRef());
    }

    private void cloneAndMerge(List<ObjectReferenceType> reviewers, Collection<ObjectReferenceType> newReviewers) {
        if (newReviewers == null) {
            return;
        }
        for (ObjectReferenceType newReviewer : newReviewers) {
            if (!containsOid(reviewers, newReviewer.getOid())) {
                reviewers.add(newReviewer.clone());
            }
        }
    }

    private boolean containsOid(List<ObjectReferenceType> reviewers, String oid) {
        for (ObjectReferenceType reviewer : reviewers) {
            if (reviewer.getOid().equals(oid)) {
                return true;
            }
        }
        return false;
    }

    // TODO implement this
    private Collection<ObjectReferenceType> getSubjectManagers(AccessCertificationCaseType _case, Task task, OperationResult result) {
        return null;
    }

    protected List<ObjectReferenceType> getTargetObjectOwners(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getTargetRef() == null) {
            return null;
        }
        ObjectType target = objectResolver.resolve(_case.getTargetRef(), ObjectType.class, null, "resolving cert case target", result);
        if (target instanceof AbstractRoleType) {
            ObjectReferenceType ownerRef = ((AbstractRoleType) target).getOwnerRef();
            if (ownerRef != null) {
                return Arrays.asList(ownerRef);
            } else {
                return null;
            }
        } else if (target instanceof ResourceType) {
            return ResourceTypeUtil.getOwnerRef((ResourceType) target);
        } else {
            return null;
        }
    }

    protected List<ObjectReferenceType> getSubjectOwners(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getSubjectRef() == null) {
            return null;
        }
        ObjectType subject = objectResolver.resolve(_case.getSubjectRef(), ObjectType.class, null, "resolving cert case subject", result);
        if (subject instanceof AbstractRoleType) {
            ObjectReferenceType ownerRef = ((AbstractRoleType) subject).getOwnerRef();
            if (ownerRef != null) {
                return Arrays.asList(ownerRef);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private Collection<ObjectReferenceType> getTargetObjectApprovers(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getTargetRef() == null) {
            return null;
        }
        ObjectType target = objectResolver.resolve(_case.getTargetRef(), ObjectType.class, null, "resolving cert case target", result);
        if (target instanceof AbstractRoleType) {
            return ((AbstractRoleType) target).getApproverRef();
        } else if (target instanceof ResourceType) {
            return ResourceTypeUtil.getApproverRef((ResourceType) target);
        } else {
            return null;
        }
    }

    private Collection<ObjectReferenceType> getSubjectApprovers(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getSubjectRef() == null) {
            return null;
        }
        ObjectType subject = objectResolver.resolve(_case.getSubjectRef(), ObjectType.class, null, "resolving cert case subject", result);
        if (subject instanceof AbstractRoleType) {
            return ((AbstractRoleType) subject).getApproverRef();
        } else {
            return null;
        }
    }



}
