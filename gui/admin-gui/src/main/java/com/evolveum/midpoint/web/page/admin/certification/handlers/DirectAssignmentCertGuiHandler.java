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

package com.evolveum.midpoint.web.page.admin.certification.handlers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseOrDecisionDto;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class DirectAssignmentCertGuiHandler implements CertGuiHandler {
    @Override
    public String getCaseInfoButtonTitle(IModel<? extends CertCaseOrDecisionDto> rowModel, PageBase page) {

        CertCaseOrDecisionDto dto = rowModel.getObject();
        AccessCertificationCaseType _case = dto.getCertCase();
        if (!(_case instanceof AccessCertificationAssignmentCaseType)) {
            return null;            // should not occur, TODO treat gracefully
        }
        AccessCertificationAssignmentCaseType assignmentCase = (AccessCertificationAssignmentCaseType) _case;
        AssignmentType assignment = assignmentCase.getAssignment();

        List<String> infoList = new ArrayList<>();

        String assignmentOrInducement;
        if (Boolean.TRUE.equals(assignmentCase.isIsInducement())) {
            assignmentOrInducement = page.createStringResource("PageCert.message.textInducement").getString();
        } else {
            assignmentOrInducement = page.createStringResource("PageCert.message.textAssignment").getString();
        }
        String targetType = getLocalizedTypeName(_case.getTargetRef().getType(), page);
        String targetName = dto.getTargetName();
        String objectType = getLocalizedTypeName(_case.getObjectRef().getType(), page);
        String objectName = dto.getObjectName();

        infoList.add(page.createStringResource("PageCert.message.assignment",
                assignmentOrInducement,
                emptyToDash(targetType), emptyToDash(targetName),
                emptyToDash(objectType), emptyToDash(objectName)).getString());

        if (StringUtils.isNotEmpty(assignment.getDescription())) {
            infoList.add(page.createStringResource("PageCert.message.textDescription", assignment.getDescription()).getString());
        }
        if (assignment.getOrder() != null) {
            infoList.add(page.createStringResource("PageCert.message.textOrder", assignment.getOrder()).getString());
        }
        if (assignment.getConstruction() != null) {
            if (assignment.getConstruction().getKind() != null) {
                infoList.add(page.createStringResource("PageCert.message.textKind",
                        page.createStringResource(assignment.getConstruction().getKind()).getString()).getString());
            }
            if (assignment.getConstruction().getIntent() != null) {
                infoList.add(page.createStringResource("PageCert.message.textIntent",
                        assignment.getConstruction().getIntent()).getString());
            }
        }
        if (_case.getTargetRef().getRelation() != null) {
            infoList.add(page.createStringResource("PageCert.message.textRelation", _case.getTargetRef().getRelation().getLocalPart()).getString());
        }
        Task task = page.createSimpleTask("dummy");
        if (assignment.getOrgRef()  != null) {
            String orgName = WebModelServiceUtils.resolveReferenceName(assignment.getOrgRef(), page, task, task.getResult());
            infoList.add(page.createStringResource("PageCert.message.textOrg", orgName).getString());
        }
        if (assignment.getTenantRef() != null) {
            String tenantName = WebModelServiceUtils.resolveReferenceName(assignment.getTenantRef(), page, task, task.getResult());
            infoList.add(page.createStringResource("PageCert.message.textTenant", tenantName).getString());
        }

        PrismContainer<? extends Containerable> extensionContainer = assignment.asPrismContainerValue().findContainer(AssignmentType.F_EXTENSION);
        if (extensionContainer != null && !extensionContainer.isEmpty()) {
            List<String> extensionItemNameList = new ArrayList<>();
            for (Item extensionItem : extensionContainer.getValue().getItems()) {
                extensionItemNameList.add(extensionItem.getElementName().getLocalPart());
            }
            infoList.add(page.createStringResource("PageCert.message.textExtensions",
                    StringUtils.join(extensionItemNameList, ", ")).getString());
        }

        if (assignment.getActivation() != null) {
            String validFrom = WebComponentUtil.formatDate(assignment.getActivation().getValidFrom());
            if (validFrom != null) {
                infoList.add(page.createStringResource("PageCert.message.textValidFrom", validFrom).getString());
            }
            String validTo = WebComponentUtil.formatDate(assignment.getActivation().getValidTo());
            if (validTo != null) {
                infoList.add(page.createStringResource("PageCert.message.textValidTo", validTo).getString());
            }
            if (assignment.getActivation().getAdministrativeStatus() != null) {
                infoList.add(page.createStringResource("PageCert.message.textAdministrativeState",
                        page.createStringResource(assignment.getActivation().getAdministrativeStatus()).getString()).getString());
            }
        }

        String rv = StringUtils.join(infoList, "<br/>");
        return rv;
    }

    protected String getLocalizedTypeName(QName typeQName, PageBase page) {
        String targetTypeKey = ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(typeQName)).getLocalizationKey();
        return page.createStringResource(targetTypeKey).getString();
    }

    private String emptyToDash(String s) {
        return StringUtils.isNotEmpty(s) ? s : "-";
    }
}
