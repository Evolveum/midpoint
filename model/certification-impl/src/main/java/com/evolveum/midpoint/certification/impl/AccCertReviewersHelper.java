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
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationReviewerSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ManagerSearchType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class AccCertReviewersHelper {

    @Autowired
    private ModelService modelService;

    @Autowired
    private MidpointFunctions midpointFunctions;

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
                                      AccessCertificationReviewerSpecificationType reviewerSpec, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {

        _case.getReviewerRef().clear();
        if (reviewerSpec == null) {
            return;     // TODO issue a warning here?
        }

        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetOwner())) {
            cloneAndMerge(_case.getReviewerRef(), getTargetObjectOwners(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetApprover())) {
            cloneAndMerge(_case.getReviewerRef(), getTargetObjectApprovers(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseObjectOwner())) {
            cloneAndMerge(_case.getReviewerRef(), getObjectOwners(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseObjectApprover())) {
            cloneAndMerge(_case.getReviewerRef(), getObjectApprovers(_case, task, result));
        }
        if (reviewerSpec.getUseObjectManager() != null) {
            cloneAndMerge(_case.getReviewerRef(), getObjectManagers(_case, reviewerSpec.getUseObjectManager(), task, result));
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

    private Collection<ObjectReferenceType> getObjectManagers(AccessCertificationCaseType _case, ManagerSearchType managerSearch, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        ModelExpressionThreadLocalHolder.pushCurrentResult(result);
        ModelExpressionThreadLocalHolder.pushCurrentTask(task);
        try {
            ObjectReferenceType objectRef = _case.getObjectRef();
            ObjectType object = midpointFunctions.resolveReference(objectRef);
            if (object == null) {
                return null;
            }
            String orgType = managerSearch.getOrgType();
            boolean allowSelf = Boolean.TRUE.equals(managerSearch.isAllowSelf());
            Collection<UserType> managers;
            if (object instanceof UserType) {
                managers = midpointFunctions.getManagers((UserType) object, orgType, allowSelf);
            } else if (object instanceof OrgType) {
                // TODO more elaborate behavior; eliminate unneeded resolveReference above
                managers = midpointFunctions.getManagersOfOrg(object.getOid());
            } else if (object instanceof RoleType) {
                // TODO implement
                managers = new HashSet<>();
            } else {
                // TODO warning?
                managers = new HashSet<>();
            }
            List<ObjectReferenceType> retval = new ArrayList<>(managers.size());
            for (UserType manager : managers) {
                retval.add(ObjectTypeUtil.createObjectRef(manager));
            }
            return retval;
        } finally {
            ModelExpressionThreadLocalHolder.popCurrentResult();
            ModelExpressionThreadLocalHolder.popCurrentTask();
        }
    }

    protected List<ObjectReferenceType> getTargetObjectOwners(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getTargetRef() == null) {
            return null;
        }
        ObjectType target = objectResolver.resolve(_case.getTargetRef(), ObjectType.class, null, "resolving cert case target", task, result);
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

    protected List<ObjectReferenceType> getObjectOwners(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getObjectRef() == null) {
            return null;
        }
        ObjectType object = objectResolver.resolve(_case.getObjectRef(), ObjectType.class, null, "resolving cert case object", task, result);
        if (object instanceof AbstractRoleType) {
            ObjectReferenceType ownerRef = ((AbstractRoleType) object).getOwnerRef();
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
        ObjectType target = objectResolver.resolve(_case.getTargetRef(), ObjectType.class, null, "resolving cert case target", task, result);
        if (target instanceof AbstractRoleType) {
            return ((AbstractRoleType) target).getApproverRef();
        } else if (target instanceof ResourceType) {
            return ResourceTypeUtil.getApproverRef((ResourceType) target);
        } else {
            return null;
        }
    }

    private Collection<ObjectReferenceType> getObjectApprovers(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getObjectRef() == null) {
            return null;
        }
        ObjectType object = objectResolver.resolve(_case.getObjectRef(), ObjectType.class, null, "resolving cert case object", task, result);
        if (object instanceof AbstractRoleType) {
            return ((AbstractRoleType) object).getApproverRef();
        } else {
            return null;
        }
    }



}
