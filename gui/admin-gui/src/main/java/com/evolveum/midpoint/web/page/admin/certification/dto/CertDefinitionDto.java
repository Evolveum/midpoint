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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.io.Serializable;

/**
 * @author mederly
 */
public class CertDefinitionDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_OWNER_NAME = "ownerName";
    public static final String F_NUMBER_OF_STAGES = "numberOfStages";
    public static final String F_XML = "xml";
    public static final String F_OWNER= "owner";
    public static final String F_SCOPE_DEFINITION = "scopeDefinition";

    private AccessCertificationDefinitionType definition;           // TODO consider replacing this by constituent primitive data items
    private DefinitionScopeDto definitionScopeDto = null;
    private String ownerName;
    private String xml;
    private PrismReferenceValue owner;

    public CertDefinitionDto(AccessCertificationDefinitionType definition, PageBase page, Task task, OperationResult result) {
        this.definition = definition;
        ownerName = CertCampaignDto.resolveOwnerName(definition.getOwnerRef(), page, task, result);

        try {
            xml = page.getPrismContext().serializeObjectToString(definition.asPrismObject(), PrismContext.LANG_XML);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't serialize campaign definition to XML", e);
        }
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getXml() {
        return xml;
    }

    public String getName() {
        return WebMiscUtil.getName(definition);
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

    public void setDefinition(AccessCertificationDefinitionType definition) {
        this.definition = definition;
    }

    public void setOwnerName(String ownerName) {
//        definition.setOwnerRef();
    }

    public void setName(String name){
        PolyStringType namePolyString  = new PolyStringType(name);
        definition.setName(namePolyString);
        String test = "";
        test.substring(0,0);
    }

    public void setDescription(String description){
        definition.setDescription(description);
    }

    public PrismReferenceValue getOwner() {
        return definition.getOwnerRef() != null ? definition.getOwnerRef().asReferenceValue() : null;
    }

    public void setOwner(PrismReferenceValue owner) {
        ObjectReferenceType ownerRef = new ObjectReferenceType();
        ownerRef.setupReferenceValue(owner);
        definition.setOwnerRef(ownerRef);
    }

    public DefinitionScopeDto getScopeDefinition() {
        if (definitionScopeDto == null){
            AccessCertificationAssignmentReviewScopeType scopeTypeObj = (AccessCertificationAssignmentReviewScopeType)definition.getScopeDefinition();
            if (scopeTypeObj != null){
                definitionScopeDto = new DefinitionScopeDto();
                definitionScopeDto.setName(scopeTypeObj.getName());
                definitionScopeDto.setDescription(scopeTypeObj.getDescription());
                definitionScopeDto.setObjectType(scopeTypeObj.getObjectType());
                definitionScopeDto.setSearchFilter(scopeTypeObj.getSearchFilter());
                definitionScopeDto.setIncludeAssignments(scopeTypeObj.isIncludeAssignments() == null ? false : scopeTypeObj.isIncludeAssignments());
                definitionScopeDto.setIncludeInducements(scopeTypeObj.isIncludeInducements() == null ? false : scopeTypeObj.isIncludeInducements());
                definitionScopeDto.setIncludeResources(scopeTypeObj.isIncludeResources() == null ? false : scopeTypeObj.isIncludeResources());
                definitionScopeDto.setIncludeOrgs(scopeTypeObj.isIncludeOrgs() == null ? false : scopeTypeObj.isIncludeOrgs());
                definitionScopeDto.setEnabledItemsOnly(scopeTypeObj.isEnabledItemsOnly() == null ? false : scopeTypeObj.isEnabledItemsOnly());
            }
        }
        return definitionScopeDto == null ? new DefinitionScopeDto() : definitionScopeDto;
    }

    public void setScopeDefinition(DefinitionScopeDto scopeType) {
        AccessCertificationAssignmentReviewScopeType scopeTypeObj = null;
        if (scopeType != null){
            scopeTypeObj = new AccessCertificationAssignmentReviewScopeType();
            scopeTypeObj.setName(definitionScopeDto.getName());
            scopeTypeObj.setDescription(definitionScopeDto.getDescription());
            scopeTypeObj.setObjectType(definitionScopeDto.getObjectType());
            scopeTypeObj.setSearchFilter(definitionScopeDto.getSearchFilter());
            scopeTypeObj.setIncludeAssignments(definitionScopeDto.isIncludeAssignments());
            scopeTypeObj.setIncludeInducements(definitionScopeDto.isIncludeInducements());
            scopeTypeObj.setIncludeResources(definitionScopeDto.isIncludeResources());
            scopeTypeObj.setIncludeOrgs(definitionScopeDto.isIncludeOrgs());
            scopeTypeObj.setEnabledItemsOnly(definitionScopeDto.isEnabledItemsOnly());
        }
        definition.setScopeDefinition(scopeTypeObj);
    }
}
