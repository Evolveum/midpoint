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
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.ReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang3.StringUtils;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationReviewerSpecificationType.F_ADDITIONAL_REVIEWER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationReviewerSpecificationType.F_DEFAULT_REVIEWER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType.F_REVIEWER_SPECIFICATION;

/**
 * @author mederly
 */
public class CertDefinitionDto implements Serializable {

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
    private final List<StageDefinitionDto> stageDefinition;
    private AccessCertificationRemediationStyleType remediationStyle;
    private AccessCertificationCaseOutcomeStrategyType outcomeStrategy;
    //private List<AccessCertificationResponseType> stopReviewOn, advanceToNextStageOn;
    private String xml;
    private ObjectViewDto owner;

    public CertDefinitionDto(AccessCertificationDefinitionType definition, PageBase page,
            PrismContext prismContext)
            throws SchemaException {
        this.oldDefinition = definition.clone();
        this.definition = definition;
        owner = loadOwnerReference(definition.getOwnerRef());

        try {
            xml = page.getPrismContext().serializeObjectToString(definition.asPrismObject(), PrismContext.LANG_XML);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't serialize campaign definition to XML", e);
        }

        definitionScopeDto = createDefinitionScopeDto(definition.getScopeDefinition(), page.getPrismContext());
        stageDefinition = new ArrayList<>();
        for (AccessCertificationStageDefinitionType stageDef  : definition.getStageDefinition()){
            stageDefinition.add(createStageDefinitionDto(stageDef, prismContext));
        }
        if (definition.getRemediationDefinition() != null) {
            remediationStyle = definition.getRemediationDefinition().getStyle();
        } else {
            remediationStyle = AccessCertificationRemediationStyleType.AUTOMATED;           // TODO consider the default...
        }
        if (definition.getReviewStrategy() != null) {
            outcomeStrategy = definition.getReviewStrategy().getOutcomeStrategy();
        } else {
            outcomeStrategy = AccessCertificationCaseOutcomeStrategyType.ONE_DENY_DENIES;   // TODO consider the default...
        }
    }

    private ObjectViewDto loadOwnerReference(ObjectReferenceType ref) {
        ObjectViewDto dto;

        if (ref != null) {
            if (ref.getTargetName() != null) {
                dto = new ObjectViewDto(ref.getOid(), WebComponentUtil.getOrigStringFromPoly(ref.getTargetName()));
                dto.setType(UserType.class);
                return dto;
            } else {
                dto = new ObjectViewDto(ObjectViewDto.BAD_OID);
                dto.setType(UserType.class);
                return dto;
            }
        } else {
            dto = new ObjectViewDto();
            dto.setType(UserType.class);
            return dto;
        }
    }

    public String getXml() {
        return xml;
    }

    public String getName() {
        return WebComponentUtil.getName(definition);
    }

    public String getDescription() {
        return definition.getDescription();
    }

    public int getNumberOfStages() {
        return definition.getStageDefinition().size();
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

        // default values, optionally overriden below
        dto.setIncludeAssignments(true);
        dto.setIncludeInducements(true);
        dto.setIncludeResources(true);
        dto.setIncludeRoles(true);
        dto.setIncludeOrgs(true);
        dto.setEnabledItemsOnly(true);

        if (scopeTypeObj != null) {
            dto.setName(scopeTypeObj.getName());
            dto.setDescription(scopeTypeObj.getDescription());
            if (scopeTypeObj instanceof AccessCertificationObjectBasedScopeType) {
                AccessCertificationObjectBasedScopeType objScopeType = (AccessCertificationObjectBasedScopeType) scopeTypeObj;
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
                    dto.setEnabledItemsOnly(!Boolean.FALSE.equals(assignmentScope.isEnabledItemsOnly()));
                }
            }
        }
        return dto;
    }

    private StageDefinitionDto createStageDefinitionDto(AccessCertificationStageDefinitionType stageDefObj,
            PrismContext prismContext)
            throws SchemaException {
        StageDefinitionDto dto = new StageDefinitionDto();
        if (stageDefObj != null) {
            dto.setNumber(stageDefObj.getNumber());
            dto.setName(stageDefObj.getName());
            dto.setDescription(stageDefObj.getDescription());
            if (stageDefObj.getDuration() != null) {
                dto.setDuration(stageDefObj.getDuration().toString());
            }
            dto.setNotifyBeforeDeadline(convertDurationListToString(stageDefObj.getNotifyBeforeDeadline()));
            dto.setNotifyOnlyWhenNoDecision(Boolean.TRUE.equals(stageDefObj.isNotifyOnlyWhenNoDecision()));
            dto.setReviewerDto(createAccessCertificationReviewerDto(stageDefObj.getReviewerSpecification(), prismContext));
            dto.setOutcomeStrategy(stageDefObj.getOutcomeStrategy());
            dto.setOutcomeIfNoReviewers(stageDefObj.getOutcomeIfNoReviewers());
            dto.setStopReviewOnRaw(new ArrayList<>(stageDefObj.getStopReviewOn()));
            dto.setAdvanceToNextStageOnRaw(new ArrayList<>(stageDefObj.getAdvanceToNextStageOn()));
        } else {
            dto.setReviewerDto(createAccessCertificationReviewerDto(null, prismContext));
        }
        return dto;
    }

    private AccessCertificationReviewerDto createAccessCertificationReviewerDto(
            AccessCertificationReviewerSpecificationType reviewer, PrismContext prismContext)
            throws SchemaException {
        AccessCertificationReviewerDto dto = new AccessCertificationReviewerDto();
        final PrismReference defaultReviewersReference;
        final PrismReference additionalReviewersReference;
        if (reviewer != null) {
            dto.setName(reviewer.getName());
            dto.setDescription(reviewer.getDescription());
            dto.setUseTargetOwner(Boolean.TRUE.equals(reviewer.isUseTargetOwner()));
            dto.setUseTargetApprover(Boolean.TRUE.equals(reviewer.isUseTargetApprover()));
            dto.setUseObjectOwner(Boolean.TRUE.equals(reviewer.isUseObjectOwner()));
            dto.setUseObjectApprover(Boolean.TRUE.equals(reviewer.isUseObjectApprover()));
            dto.setUseObjectManager(createManagerSearchDto(reviewer.getUseObjectManager()));
            defaultReviewersReference = reviewer.asPrismContainerValue().findOrCreateReference(AccessCertificationReviewerSpecificationType.F_DEFAULT_REVIEWER_REF);
            additionalReviewersReference = reviewer.asPrismContainerValue().findOrCreateReference(AccessCertificationReviewerSpecificationType.F_ADDITIONAL_REVIEWER_REF);
        } else {
            PrismReferenceDefinition defReviewerDef = prismContext.getSchemaRegistry().findItemDefinitionByFullPath(AccessCertificationDefinitionType.class,
                    PrismReferenceDefinition.class,
                    AccessCertificationDefinitionType.F_STAGE_DEFINITION, F_REVIEWER_SPECIFICATION, F_DEFAULT_REVIEWER_REF);
            defaultReviewersReference = defReviewerDef.instantiate();
            PrismReferenceDefinition additionalReviewerDef = prismContext.getSchemaRegistry().findItemDefinitionByFullPath(AccessCertificationDefinitionType.class,
                    PrismReferenceDefinition.class,
                    AccessCertificationDefinitionType.F_STAGE_DEFINITION, F_REVIEWER_SPECIFICATION, F_ADDITIONAL_REVIEWER_REF);
            additionalReviewersReference = additionalReviewerDef.instantiate();

        }
        dto.setDefaultReviewers(new ReferenceWrapper(null, defaultReviewersReference, false, ValueStatus.NOT_CHANGED));
        dto.setAdditionalReviewers(new ReferenceWrapper(null, additionalReviewersReference, false, ValueStatus.NOT_CHANGED));
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

    private List<ObjectReferenceType> cloneListObjectsForSave(List<ObjectReferenceType> listToClone){
        if (listToClone != null){
            if (listToClone.size() > 0) {
                List<ObjectReferenceType> list = new ArrayList<>();
                for (ObjectReferenceType objectReferenceType : listToClone) {
                    list.add(objectReferenceType.clone());
                }
                return list;
            }
        }
        return new ArrayList<ObjectReferenceType>();
    }

    public DefinitionScopeDto getScopeDefinition() {
        return definitionScopeDto;
    }

    public void updateScopeDefinition(PrismContext prismContext) {
        AccessCertificationAssignmentReviewScopeType scopeTypeObj = null;
        if (definitionScopeDto != null) {
            scopeTypeObj = new AccessCertificationAssignmentReviewScopeType();
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
            scopeTypeObj.setIncludeOrgs(definitionScopeDto.isIncludeOrgs());
            scopeTypeObj.setEnabledItemsOnly(definitionScopeDto.isEnabledItemsOnly());
            // needed because of prism implementation limitation (because the scopeDefinition is declared as AccessCertificationScopeType)
            scopeTypeObj.asPrismContainerValue().setConcreteType(AccessCertificationAssignmentReviewScopeType.COMPLEX_TYPE);
        }
        definition.setScopeDefinition(scopeTypeObj);
    }

    public void updateStageDefinition(PrismContext prismContext) throws SchemaException {
        List<AccessCertificationStageDefinitionType> stageDefinitionTypeList = new ArrayList<>();
        if (stageDefinition != null && stageDefinition.size() > 0) {
            for (StageDefinitionDto stageDefinitionDto : stageDefinition){
                stageDefinitionTypeList.add(createStageDefinitionType(stageDefinitionDto, prismContext));
            }
        }
        definition.getStageDefinition().clear();
        definition.getStageDefinition().addAll(stageDefinitionTypeList);
    }

    private AccessCertificationStageDefinitionType createStageDefinitionType(StageDefinitionDto stageDefDto, PrismContext prismContext)
            throws SchemaException {
        AccessCertificationStageDefinitionType stageDefType = new AccessCertificationStageDefinitionType();
        if (stageDefDto != null) {
            stageDefType.setNumber(stageDefDto.getNumber());
            stageDefType.setName(stageDefDto.getName());
            stageDefType.setDescription(stageDefDto.getDescription());
            if (StringUtils.isNotBlank(stageDefDto.getDuration())) {
                stageDefType.setDuration(XmlTypeConverter.createDuration(stageDefDto.getDuration()));
            }
            stageDefType.getNotifyBeforeDeadline().clear();
            stageDefType.getNotifyBeforeDeadline().addAll(convertStringToDurationList(stageDefDto.getNotifyBeforeDeadline()));
            stageDefType.setNotifyOnlyWhenNoDecision(Boolean.TRUE.equals(stageDefDto.isNotifyOnlyWhenNoDecision()));
            stageDefType.setReviewerSpecification(createAccessCertificationReviewerType(stageDefDto.getReviewerDto(), prismContext));
            stageDefType.setOutcomeStrategy(stageDefDto.getOutcomeStrategy());
            stageDefType.setOutcomeIfNoReviewers(stageDefDto.getOutcomeIfNoReviewers());
        }
        return stageDefType;
    }

    private AccessCertificationReviewerSpecificationType createAccessCertificationReviewerType(
            AccessCertificationReviewerDto reviewerDto, PrismContext prismContext) throws SchemaException {
        AccessCertificationReviewerSpecificationType reviewerObject = new AccessCertificationReviewerSpecificationType();
        if (reviewerDto != null) {
            reviewerObject.setName(reviewerDto.getName());
            reviewerObject.setDescription(reviewerDto.getDescription());
            reviewerObject.setUseTargetOwner(Boolean.TRUE.equals(reviewerDto.isUseTargetOwner()));
            reviewerObject.setUseTargetApprover(Boolean.TRUE.equals(reviewerDto.isUseTargetApprover()));
            reviewerObject.setUseObjectOwner(Boolean.TRUE.equals(reviewerDto.isUseObjectOwner()));
            reviewerObject.setUseObjectApprover(Boolean.TRUE.equals(reviewerDto.isUseObjectApprover()));
            reviewerObject.setUseObjectManager(createManagerSearchType(reviewerDto.getUseObjectManager()));
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

    private ManagerSearchDto createManagerSearchDto(ManagerSearchType managerSearchType){
        ManagerSearchDto managerSearchDto = new ManagerSearchDto();
        if (managerSearchType != null){
            managerSearchDto.setOrgType(managerSearchType.getOrgType());
            managerSearchDto.setAllowSelf(managerSearchType.isAllowSelf());
        }
        return managerSearchDto;
    }

    private String convertDurationListToString(List<Duration> list){
        String result = StringUtils.join(list, ", ");
        return result;
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
}
