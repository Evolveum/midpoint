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

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class CertDefinitionDto implements Serializable {

    public static final String F_PRISM_OBJECT = "prismObject";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_NUMBER_OF_STAGES = "numberOfStages";
    public static final String F_XML = "xml";
    public static final String F_OWNER = "owner";
    public static final String F_REMEDIATION_STYLE = "remediationStyle";
    public static final String F_SCOPE_DEFINITION = "scopeDefinition";
    public static final String F_STAGE_DEFINITION = "stageDefinition";
    public static final String F_LAST_STARTED = "lastStarted";
    public static final String F_LAST_CLOSED = "lastClosed";
    public static final String F_OUTCOME_STRATEGY = "outcomeStrategy";
    public static final String F_STOP_REVIEW_ON = "stopReviewOn";
    //public static final String F_ADVANCE_TO_NEXT_STAGE_ON = "advanceToNextStageOn";

    private AccessCertificationDefinitionType oldDefinition;            // to be able to compute the delta when saving
    private AccessCertificationDefinitionType definition;               // definition that is (at least partially) dynamically updated when editing the form
    private final DefinitionScopeDto definitionScopeDto;
    @NotNull private final List<StageDefinitionDto> stageDefinition;
    private AccessCertificationRemediationStyleType remediationStyle;
    private List<AccessCertificationResponseType> revokeOn;
    private AccessCertificationCaseOutcomeStrategyType outcomeStrategy;
    //private List<AccessCertificationResponseType> stopReviewOn, advanceToNextStageOn;
    private ObjectViewDto<UserType> owner;

    public CertDefinitionDto(AccessCertificationDefinitionType definition, PageBase page, PrismContext prismContext)
            throws SchemaException {
        this.oldDefinition = definition.clone();
        this.definition = definition;
        owner = loadOwnerReference(definition.getOwnerRef());

        definitionScopeDto = createDefinitionScopeDto(definition.getScopeDefinition(), page.getPrismContext());
        stageDefinition = new ArrayList<>();
        for (AccessCertificationStageDefinitionType stageDef : definition.getStageDefinition()) {
            stageDefinition.add(createStageDefinitionDto(stageDef, prismContext));
        }
        if (definition.getRemediationDefinition() != null) {
            remediationStyle = definition.getRemediationDefinition().getStyle();
            revokeOn = new ArrayList<>(definition.getRemediationDefinition().getRevokeOn());
        } else {
            remediationStyle = AccessCertificationRemediationStyleType.AUTOMATED;           // TODO consider the default...
            revokeOn = new ArrayList<>();
        }
        if (definition.getReviewStrategy() != null) {
            outcomeStrategy = definition.getReviewStrategy().getOutcomeStrategy();
        } else {
            outcomeStrategy = AccessCertificationCaseOutcomeStrategyType.ONE_DENY_DENIES;   // TODO consider the default...
        }
    }

    private ObjectViewDto<UserType> loadOwnerReference(ObjectReferenceType ref) {
        ObjectViewDto<UserType> dto;

        if (ref != null) {
            if (ref.getTargetName() != null) {
                dto = new ObjectViewDto<>(ref.getOid(), WebComponentUtil.getOrigStringFromPoly(ref.getTargetName()));
                dto.setType(UserType.class);
                return dto;
            } else {
                dto = new ObjectViewDto<>(ObjectViewDto.BAD_OID);
                dto.setType(UserType.class);
                return dto;
            }
        } else {
            dto = new ObjectViewDto<>();
            dto.setType(UserType.class);
            return dto;
        }
    }

    public String getXml() {
		try {
			PrismContext prismContext = ((MidPointApplication) Application.get()).getPrismContext();
			return prismContext.xmlSerializer().serialize(getUpdatedDefinition(prismContext).asPrismObject());
		} catch (SchemaException|RuntimeException e) {
			return "Couldn't serialize campaign definition to XML: " + e.getMessage();
		}
	}

	public void setXml(String s) {
		// ignore
	}

    public String getName() {
        return WebComponentUtil.getName(definition);
    }

    public String getDescription() {
        return definition.getDescription();
    }

    public int getNumberOfStages() {
        return stageDefinition.size();
    }

    public AccessCertificationDefinitionType getDefinition() {
        return definition;
    }

    public AccessCertificationDefinitionType getUpdatedDefinition(PrismContext prismContext)
            throws SchemaException {
        updateOwner();
        updateScopeDefinition(prismContext);
        updateStageDefinition(prismContext);
        if (remediationStyle != null) {
            AccessCertificationRemediationDefinitionType remDef = new AccessCertificationRemediationDefinitionType(prismContext);
            remDef.setStyle(remediationStyle);
            remDef.getRevokeOn().addAll(revokeOn);
            definition.setRemediationDefinition(remDef);
        } else {
            definition.setRemediationDefinition(null);
        }
        if (outcomeStrategy != null) {
            if (definition.getReviewStrategy() == null) {
                definition.setReviewStrategy(new AccessCertificationCaseReviewStrategyType());
            }
            definition.getReviewStrategy().setOutcomeStrategy(outcomeStrategy);
        } else {
            if (definition.getReviewStrategy() != null) {
                definition.getReviewStrategy().setOutcomeStrategy(null);
            }
        }
        return definition;
    }

    private void updateOwner() {
        String oid = owner.getKnownOid();
        if (oid != null) {
            definition.setOwnerRef(ObjectTypeUtil.createObjectRef(owner.getKnownOid(), ObjectTypes.USER));
        } else {
            definition.setOwnerRef(null);
        }
    }

    public AccessCertificationDefinitionType getOldDefinition() {
        return oldDefinition;
    }

    public void setDefinition(AccessCertificationDefinitionType definition) {
        this.definition = definition;
    }

    public void setName(String name){
        PolyStringType namePolyString  = new PolyStringType(name);
        definition.setName(namePolyString);
    }

    public void setDescription(String description){
        definition.setDescription(description);
    }

    public ObjectViewDto getOwner() {
        return owner;
    }

    public void setOwner(PrismReferenceValue owner) {
        ObjectReferenceType ownerRef = new ObjectReferenceType();
        ownerRef.setupReferenceValue(owner);
        definition.setOwnerRef(ownerRef);
    }

    public AccessCertificationRemediationStyleType getRemediationStyle() {
        return remediationStyle;
    }

    public void setRemediationStyle(AccessCertificationRemediationStyleType remediationStyle) {
        this.remediationStyle = remediationStyle;
    }

    private DefinitionScopeDto createDefinitionScopeDto(AccessCertificationScopeType scopeTypeObj, PrismContext prismContext) {
        DefinitionScopeDto dto = new DefinitionScopeDto();

        // default values, optionally overridden below
        dto.setIncludeAssignments(true);
        dto.setIncludeInducements(true);
        dto.setIncludeResources(true);
        dto.setIncludeRoles(true);
        dto.setIncludeOrgs(true);
        dto.setIncludeServices(true);
        dto.setEnabledItemsOnly(true);

        if (scopeTypeObj != null) {
            dto.setName(scopeTypeObj.getName());
            dto.setDescription(scopeTypeObj.getDescription());
            if (scopeTypeObj instanceof AccessCertificationObjectBasedScopeType) {
                AccessCertificationObjectBasedScopeType objScopeType = (AccessCertificationObjectBasedScopeType) scopeTypeObj;
                dto.setItemSelectionExpression(objScopeType.getItemSelectionExpression());
                if (objScopeType.getObjectType() != null) {
                    dto.setObjectType(DefinitionScopeObjectType.valueOf(objScopeType.getObjectType().getLocalPart()));
                }
                dto.loadSearchFilter(objScopeType.getSearchFilter(), prismContext);
                if (objScopeType instanceof AccessCertificationAssignmentReviewScopeType) {
                    AccessCertificationAssignmentReviewScopeType assignmentScope =
                            (AccessCertificationAssignmentReviewScopeType) objScopeType;
                    dto.setIncludeAssignments(!Boolean.FALSE.equals(assignmentScope.isIncludeAssignments()));
                    dto.setIncludeInducements(!Boolean.FALSE.equals(assignmentScope.isIncludeInducements()));
                    dto.setIncludeResources(!Boolean.FALSE.equals(assignmentScope.isIncludeResources()));
                    dto.setIncludeRoles(!Boolean.FALSE.equals(assignmentScope.isIncludeRoles()));
                    dto.setIncludeOrgs(!Boolean.FALSE.equals(assignmentScope.isIncludeOrgs()));
                    dto.setIncludeServices(!Boolean.FALSE.equals(assignmentScope.isIncludeServices()));
                    dto.setIncludeUsers(!Boolean.FALSE.equals(assignmentScope.isIncludeUsers()));
                    dto.setEnabledItemsOnly(!Boolean.FALSE.equals(assignmentScope.isEnabledItemsOnly()));
                    dto.setRelationList(new ArrayList<>(assignmentScope.getRelation()));
                }
            }
        }
        return dto;
    }

    private StageDefinitionDto createStageDefinitionDto(AccessCertificationStageDefinitionType stageDefObj,
            PrismContext prismContext)
            throws SchemaException {
        StageDefinitionDto dto = new StageDefinitionDto(stageDefObj, prismContext);
        return dto;
    }


    private List<ObjectReferenceType> cloneListObjects(List<ObjectReferenceType> listToClone){
        List<ObjectReferenceType> list = new ArrayList<>();
        if (listToClone != null){
            for (ObjectReferenceType objectReferenceType : listToClone){
                list.add(objectReferenceType.clone());
            }
        }
        return list;
    }

    public DefinitionScopeDto getScopeDefinition() {
        return definitionScopeDto;
    }

    public void updateScopeDefinition(PrismContext prismContext) {
        AccessCertificationAssignmentReviewScopeType scopeTypeObj = null;
        if (definitionScopeDto != null) {
            scopeTypeObj = new AccessCertificationAssignmentReviewScopeType(prismContext);
            scopeTypeObj.setName(definitionScopeDto.getName());
            scopeTypeObj.setDescription(definitionScopeDto.getDescription());
            scopeTypeObj.setObjectType(definitionScopeDto.getObjectType() != null ? new QName(definitionScopeDto.getObjectType().name()) : null);
            SearchFilterType parsedSearchFilter = definitionScopeDto.getParsedSearchFilter(prismContext);
            if (parsedSearchFilter != null) {
                // check if everything is OK
                try {
                    QueryConvertor.parseFilterPreliminarily(parsedSearchFilter.getFilterClauseXNode(), prismContext);
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't parse search filter: " + e.getMessage(), e);
                }
            }
            scopeTypeObj.setSearchFilter(parsedSearchFilter);
            scopeTypeObj.setIncludeAssignments(definitionScopeDto.isIncludeAssignments());
            scopeTypeObj.setIncludeInducements(definitionScopeDto.isIncludeInducements());
            scopeTypeObj.setIncludeResources(definitionScopeDto.isIncludeResources());
            scopeTypeObj.setIncludeRoles(definitionScopeDto.isIncludeRoles());
            scopeTypeObj.setIncludeOrgs(definitionScopeDto.isIncludeOrgs());
            scopeTypeObj.setIncludeServices(definitionScopeDto.isIncludeServices());
            scopeTypeObj.setIncludeUsers(definitionScopeDto.isIncludeUsers());
            scopeTypeObj.setEnabledItemsOnly(definitionScopeDto.isEnabledItemsOnly());
            scopeTypeObj.setItemSelectionExpression(definitionScopeDto.getItemSelectionExpression());
            // TODO caseGenerationExpression (it is ignored in business logic anyway, now)
            scopeTypeObj.getRelation().addAll(definitionScopeDto.getRelationList());
        }
        definition.setScopeDefinition(scopeTypeObj);
    }

    public void updateStageDefinition(PrismContext prismContext) throws SchemaException {
        definition.getStageDefinition().clear();
		for (StageDefinitionDto stageDefinitionDto : stageDefinition) {
            definition.getStageDefinition().add(createStageDefinitionType(stageDefinitionDto, prismContext));
		}
    }

    private AccessCertificationStageDefinitionType createStageDefinitionType(StageDefinitionDto stageDefDto, PrismContext prismContext)
            throws SchemaException {
        AccessCertificationStageDefinitionType stageDefType = new AccessCertificationStageDefinitionType(prismContext);
        if (stageDefDto != null) {
            stageDefType.setNumber(stageDefDto.getNumber());
            stageDefType.setName(stageDefDto.getName());
            stageDefType.setDescription(stageDefDto.getDescription());
            if (StringUtils.isNotBlank(stageDefDto.getDuration())) {
                stageDefType.setDuration(XmlTypeConverter.createDuration(stageDefDto.getDuration()));
            }
            stageDefType.setDeadlineRounding(stageDefDto.getDeadlineRounding());
            stageDefType.getNotifyBeforeDeadline().clear();
            stageDefType.getNotifyBeforeDeadline().addAll(convertStringToDurationList(stageDefDto.getNotifyBeforeDeadline()));
            stageDefType.setNotifyOnlyWhenNoDecision(Boolean.TRUE.equals(stageDefDto.isNotifyOnlyWhenNoDecision()));
            stageDefType.setReviewerSpecification(createAccessCertificationReviewerType(stageDefDto.getReviewerDto(), prismContext));
            stageDefType.setOutcomeStrategy(stageDefDto.getOutcomeStrategy());
            stageDefType.setOutcomeIfNoReviewers(stageDefDto.getOutcomeIfNoReviewers());
            stageDefType.getStopReviewOn().addAll(stageDefDto.getStopReviewOnRaw());
            stageDefType.getAdvanceToNextStageOn().addAll(stageDefDto.getAdvanceToNextStageOnRaw());
            if (stageDefDto.getTimedActionsTypes() != null) {
                stageDefType.getTimedActions().addAll(CloneUtil.cloneCollectionMembers(stageDefDto.getTimedActionsTypes()));        // because of parents
            }
        }
        return stageDefType;
    }

    private AccessCertificationReviewerSpecificationType createAccessCertificationReviewerType(
            AccessCertificationReviewerDto reviewerDto, PrismContext prismContext) throws SchemaException {
        AccessCertificationReviewerSpecificationType reviewerObject = new AccessCertificationReviewerSpecificationType(prismContext);
        if (reviewerDto != null) {
            reviewerObject.setName(reviewerDto.getName());
            reviewerObject.setDescription(reviewerDto.getDescription());
            reviewerObject.setUseTargetOwner(Boolean.TRUE.equals(reviewerDto.isUseTargetOwner()));
            reviewerObject.setUseTargetApprover(Boolean.TRUE.equals(reviewerDto.isUseTargetApprover()));
            reviewerObject.setUseObjectOwner(Boolean.TRUE.equals(reviewerDto.isUseObjectOwner()));
            reviewerObject.setUseObjectApprover(Boolean.TRUE.equals(reviewerDto.isUseObjectApprover()));
            if (reviewerDto.isUseObjectManagerPresent()) {
				reviewerObject.setUseObjectManager(createManagerSearchType(reviewerDto.getUseObjectManager()));
			}
			reviewerObject.getReviewerExpression().addAll(CloneUtil.cloneCollectionMembers(reviewerDto.getReviewerExpressionList()));
            reviewerObject.getDefaultReviewerRef().clear();
            reviewerObject.getDefaultReviewerRef().addAll(reviewerDto.getDefaultReviewersAsObjectReferenceList(prismContext));
            reviewerObject.getAdditionalReviewerRef().clear();
            reviewerObject.getAdditionalReviewerRef().addAll(reviewerDto.getAdditionalReviewersAsObjectReferenceList(prismContext));
        }
        return reviewerObject;
    }

    private ManagerSearchType createManagerSearchType(ManagerSearchDto managerSearchDto){
        ManagerSearchType managerSearchType = new ManagerSearchType();
        if (managerSearchDto != null){
            managerSearchType.setOrgType(managerSearchDto.getOrgType());
            managerSearchType.setAllowSelf(managerSearchDto.isAllowSelf());
        }
        return  managerSearchType;
    }

    private List<Duration> convertStringToDurationList(String object){
        List<Duration> list = new ArrayList<>();
        if (object != null) {
            String[] values = object.split(",");
            for (String value : values) {
                value = value.trim();
                if (!value.equals("")) {
                    list.add(XmlTypeConverter.createDuration(value));
                }
            }
        }
        return list;
    }

    public String getLastStarted() {
        return formatDate(definition.getLastCampaignStartedTimestamp());
    }

    private String formatDate(XMLGregorianCalendar dateGc) {
        if (dateGc == null) {
            return "-";
        } else {
            return WebComponentUtil.formatDate(XmlTypeConverter.toDate(dateGc));
        }
    }

    public String getLastClosed() {
        return formatDate(definition.getLastCampaignClosedTimestamp());
    }

    public AccessCertificationCaseOutcomeStrategyType getOutcomeStrategy() {
        return outcomeStrategy;
    }

    public void setOutcomeStrategy(AccessCertificationCaseOutcomeStrategyType outcomeStrategy) {
        this.outcomeStrategy = outcomeStrategy;
    }

    public List<AccessCertificationResponseType> getStopReviewOn() {
        if (definition.getReviewStrategy() == null) {
            return null;
        }
        AccessCertificationCaseReviewStrategyType strategy = definition.getReviewStrategy();
        if (strategy.getStopReviewOn().isEmpty() && strategy.getAdvanceToNextStageOn().isEmpty()) {
            return null;
        }
        return CertCampaignTypeUtil.getOutcomesToStopOn(strategy.getStopReviewOn(), strategy.getAdvanceToNextStageOn());
    }

	public PrismObject<AccessCertificationDefinitionType> getPrismObject() {
		return definition.asPrismObject();
	}
}
