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

import com.evolveum.midpoint.certification.impl.AccCertGeneralHelper;
import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationObjectBasedScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationReviewerSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public abstract class BaseCertificationHandler implements CertificationHandler {

    private static final transient Trace LOGGER = TraceManager.getTrace(BaseCertificationHandler.class);

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ObjectResolver objectResolver;

    // TODO temporary hack because of some problems in model service...
    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    @Autowired
    protected CertificationManagerImpl certificationManager;

    @Autowired
    protected AccCertGeneralHelper helper;

    @Override
    public void moveToNextStage(final AccessCertificationDefinitionType definition, final AccessCertificationCampaignType campaign,
                                final Task task, OperationResult result) throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException {
        Validate.notNull(definition, "certificationDefinition");
        Validate.notNull(definition.getOid(), "certificationDefinition.oid");
        Validate.notNull(campaign, "certificationCampaign");
        Validate.notNull(campaign.getOid(), "certificationCampaign.oid");

        int stageNumber = certificationManager.getCurrentStageNumber(campaign);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("moveToNextStage starting; campaign = {}, definition = {}, stage number = {}",
                    ObjectTypeUtil.toShortString(campaign), ObjectTypeUtil.toShortString(definition), stageNumber);
        }

        if (stageNumber == 0) {
            createCases(campaign, definition, task, result);
        } else {
            updateCases(campaign, definition, task, result);
        }

        LOGGER.trace("nextStage finishing");
    }

    private void createCases(final AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition,
                             final Task task, final OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException {
        String campaignShortName = ObjectTypeUtil.toShortString(campaign);

        AccessCertificationScopeType scope = definition.getScope();
        LOGGER.trace("Creating cases for scope {} in campaign {}", scope, campaignShortName);

        if (scope != null && !(scope instanceof AccessCertificationObjectBasedScopeType)) {
            throw new IllegalStateException("Unsupported access certification scope type: " + scope.getClass() + " for campaign " + campaignShortName);
        }
        AccessCertificationObjectBasedScopeType objectBasedScope = (AccessCertificationObjectBasedScopeType) scope;

        if (!campaign.getCase().isEmpty()) {
            throw new IllegalStateException("Unexpected " + campaign.getCase().size() + " certification case(s) in campaign object " + campaignShortName + ". At this time there should be none.");
        }

        // create a query to find target objects from which certification cases will be created
        ObjectQuery query = new ObjectQuery();
        QName objectType = objectBasedScope != null ? objectBasedScope.getObjectType() : null;
        if (objectType == null) {
            objectType = getDefaultObjectType();
        }
        if (objectType == null) {
            throw new IllegalStateException("Unspecified object type (and no default one provided) for campaign " + campaignShortName);
        }
        PrismObjectDefinition<? extends ObjectType> objectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(objectType);
        if (objectDef == null) {
            throw new IllegalStateException("Object definition not found for object type " + objectType + " in campaign " + campaignShortName);
        }
        Class<? extends ObjectType> objectClass = objectDef.getCompileTimeClass();
        if (objectClass == null) {
            throw new IllegalStateException("Object class not found for object type " + objectType + " in campaign " + campaignShortName);
        }

        SearchFilterType searchFilter = objectBasedScope != null ? objectBasedScope.getSearchFilter() : null;
        if (searchFilter != null) {
            ObjectFilter filter = QueryConvertor.parseFilter(searchFilter, objectClass, prismContext);
            query.setFilter(filter);
        }

        final List<ExpressionType> caseExpressionList = objectBasedScope != null ? objectBasedScope.getCaseExpression() : null;
        final List<AccessCertificationCaseType> caseList = new ArrayList<>();

        // create certification cases by executing the query and caseExpression on its results
        // here the subclasses of this class come into play
        ResultHandler<ObjectType> handler = new ResultHandler<ObjectType>() {
            @Override
            public boolean handle(PrismObject<ObjectType> object, OperationResult parentResult) {
                caseList.addAll(createCasesForObject(object, caseExpressionList, campaign, task, parentResult));
                return true;
            }
        };
        modelService.searchObjectsIterative(objectClass, query, (ResultHandler) handler, null, task, result);

        AccessCertificationReviewerSpecificationType reviewerSpec =
                findReviewersSpecification(campaign, definition, 1, task, result);

        // put the cases into repository
        ContainerDelta<AccessCertificationCaseType> caseDelta = ContainerDelta.createDelta(AccessCertificationCampaignType.F_CASE,
                AccessCertificationCampaignType.class, prismContext);
        for (int i = 0; i < caseList.size(); i++) {
            AccessCertificationCaseType _case = caseList.get(i);
            PrismContainerValue<AccessCertificationCaseType> caseCVal = _case.asPrismContainerValue();
            caseCVal.setId((long) (i + 1));
            caseDelta.addValueToAdd(caseCVal);

            setupReviewersForCase(_case, campaign, reviewerSpec, task, result);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Adding certification case:\n{}", caseCVal.debugDump());
            }
        }

        // there are some problems with container IDs when using model - as a temporary hack we go directly into repo
        // todo fix it and switch to model service
//        ObjectDelta<AccessCertificationCampaignType> campaignDelta = ObjectDelta.createModifyDelta(campaign.getOid(),
//                caseDelta, AccessCertificationCampaignType.class, prismContext);
//        modelService.executeChanges((Collection) Arrays.asList(campaignDelta), null, task, result);

        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(), Arrays.asList(caseDelta), result);
        LOGGER.trace("Created {} cases for campaign {}", caseList.size(), campaignShortName);
    }

    private AccessCertificationReviewerSpecificationType findReviewersSpecification(AccessCertificationCampaignType campaign,
                                                                                    AccessCertificationDefinitionType definition,
                                                                                    int stage, Task task, OperationResult result) {
        AccessCertificationStageDefinitionType stageDef = helper.findStageDefinition(definition, stage);
        return stageDef.getReviewerSpecification();
    }

    private void setupReviewersForCase(AccessCertificationCaseType _case, AccessCertificationCampaignType campaign,
                                       AccessCertificationReviewerSpecificationType reviewerSpec, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        _case.getReviewerRef().clear();
        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetObjectOwner())) {
            cloneAndMerge(_case.getReviewerRef(), getTargetObjectOwners(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetObjectApprover())) {
            cloneAndMerge(_case.getReviewerRef(), getTargetObjectApprovers(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseSubjectManager())) {
            cloneAndMerge(_case.getReviewerRef(), getSubjectManagers(_case, task, result));
        }
        // TODO evaluate reviewer expressions
        if (_case.getReviewerRef().isEmpty()) {
            cloneAndMerge(_case.getReviewerRef(), reviewerSpec.getDefaultReviewerRef());
        }
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
            return Arrays.asList(((AbstractRoleType) target).getOwnerRef());
        } else if (target instanceof ResourceType) {
            return ResourceTypeUtil.getOwnerRef((ResourceType) target);
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

    // default implementation, depending only on the expressions provided
    protected Collection<? extends AccessCertificationCaseType> createCasesForObject(PrismObject<ObjectType> object, List<ExpressionType> caseExpressionList, AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) {
        if (caseExpressionList == null) {
            throw new IllegalStateException("Unspecified case expression (and no default one provided) for campaign " + ObjectTypeUtil.toShortString(campaign));
        }
        return evaluateCaseExpressionList(caseExpressionList, object, task, parentResult);
    }

    protected Collection<? extends AccessCertificationCaseType> evaluateCaseExpressionList(List<ExpressionType> caseExpressionList, PrismObject<ObjectType> object, Task task, OperationResult parentResult) {
        List<AccessCertificationCaseType> caseList = new ArrayList<>();
        for (ExpressionType caseExpression : caseExpressionList) {
            caseList.addAll(evaluateCaseExpression(caseExpression, object, task, parentResult));
        }
        return caseList;
    }

    protected Collection<? extends AccessCertificationCaseType> evaluateCaseExpression(ExpressionType caseExpression, PrismObject<ObjectType> object, Task task, OperationResult parentResult) {
        // todo
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private void updateCases(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        LOGGER.trace("Updating reviewers for cases in {}", ObjectTypeUtil.toShortString(campaign));
        List<AccessCertificationCaseType> caseList = campaign.getCase();

        int stageToBe = certificationManager.getCurrentStageNumber(campaign) + 1;

        AccessCertificationReviewerSpecificationType reviewerSpec =
                findReviewersSpecification(campaign, definition, stageToBe, task, result);

        List<ReferenceDelta> reviewerDeltaList = new ArrayList<>(caseList.size());
        for (int i = 0; i < caseList.size(); i++) {
            AccessCertificationCaseType _case = caseList.get(i);
            setupReviewersForCase(_case, campaign, reviewerSpec, task, result);
            PrismReference reviewersRef = _case.asPrismContainerValue().findOrCreateReference(AccessCertificationCaseType.F_REVIEWER_REF);
            ReferenceDelta reviewerDelta = ReferenceDelta.createModificationReplace(
                    new ItemPath(
                            new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                            new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                            new NameItemPathSegment(AccessCertificationCaseType.F_REVIEWER_REF)),
                    certificationManager.getCampaignDefinition(), reviewersRef.getValues());
            reviewerDeltaList.add(reviewerDelta);
        }

        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(), reviewerDeltaList, result);
        LOGGER.debug("Updated reviewers in {} cases for campaign {}", caseList.size(), ObjectTypeUtil.toShortString(campaign));
    }

    protected QName getDefaultObjectType() {
        return null;
    }
}
