/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.handlers;

import com.evolveum.midpoint.certification.api.AccessCertificationApiConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseOrWorkItemDto;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

@Component
public class DirectAssignmentCertGuiHandler implements CertGuiHandler {
    private static final Trace LOGGER = TraceManager.getTrace(DirectAssignmentCertGuiHandler.class);

    @Autowired
    protected CertGuiHandlerRegistry certGuiHandlerRegistry;

    @PostConstruct
    public void register() {
        certGuiHandlerRegistry.registerCertGuiHandler(AccessCertificationApiConstants.DIRECT_ASSIGNMENT_HANDLER_URI, this);
        certGuiHandlerRegistry.registerCertGuiHandler(AccessCertificationApiConstants.EXCLUSION_HANDLER_URI, this);
    }

    @Override
    public String getCaseInfoButtonTitle(IModel<? extends CertCaseOrWorkItemDto> rowModel, PageBase page) {

        CertCaseOrWorkItemDto dto = rowModel.getObject();
        AccessCertificationCaseType acase = dto.getCertCase();
        if (!(acase instanceof AccessCertificationAssignmentCaseType)) {
            return null;            // should not occur, TODO treat gracefully
        }
        AccessCertificationAssignmentCaseType assignmentCase = (AccessCertificationAssignmentCaseType) acase;
        AssignmentType assignment = assignmentCase.getAssignment();

        List<String> infoList = new ArrayList<>();

        String assignmentOrInducement;
        if (Boolean.TRUE.equals(assignmentCase.isIsInducement())) {
            assignmentOrInducement = page.createStringResource("PageCert.message.textInducement").getString();
        } else {
            assignmentOrInducement = page.createStringResource("PageCert.message.textAssignment").getString();
        }
        String targetType = getLocalizedTypeName(acase.getTargetRef().getType(), page);
        String targetName = dto.getTargetName();
        String objectType = getLocalizedTypeName(acase.getObjectRef().getType(), page);
        String objectName = dto.getObjectName();

        // If object is UserType, display user's fullName in addition to the name
        if (QNameUtil.match(acase.getObjectRef().getType(), UserType.COMPLEX_TYPE)) {
            try {
                PrismObject<UserType> object = page.getModelService().getObject(UserType.class, acase.getObjectRef().getOid(), null, page.getPageTask(), page.getPageTask().getResult());

                if (object != null) {
                    UserType userObj = object.asObjectable();
                    PolyStringType fullName = userObj.getFullName();
                    if (fullName != null && !StringUtils.isEmpty(fullName.getOrig())) {
                        objectName = fullName.getOrig() + " (" + objectName + ")";
                    }
                }
            } catch (Exception e) {
                //probably autz exception, mute it and return object name
                LOGGER.debug("Error retrieving full object in getCaseInfoButtonTitle: {}", e.getMessage());
            }
        }

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
        if (acase.getTargetRef().getRelation() != null) {
            infoList.add(page.createStringResource("PageCert.message.textRelation", acase.getTargetRef().getRelation().getLocalPart()).getString());
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
