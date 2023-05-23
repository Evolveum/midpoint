/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import static com.evolveum.midpoint.common.RoleMiningExportUtils.*;
import static com.evolveum.midpoint.repo.api.RepositoryService.LOGGER;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.RoleMiningExportUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleMiningExportOperation implements Serializable {

    private static final String DOT_CLASS = PageDebugDownloadBehaviour.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECT = DOT_CLASS + "loadObjects";
    OperationResult result = new OperationResult(DOT_CLASS + "searchObjectByCondition");
    public static String applicationArchetypeOid;
    public static String businessArchetypeOid;
    protected List<String> applicationRolePrefix;
    protected List<String> applicationRoleSuffix;
    protected List<String> businessRolePrefix;
    protected List<String> businessRoleSuffix;
    RoleMiningExportUtils.NameMode nameMode = RoleMiningExportUtils.NameMode.SEQUENTIAL;
    RoleMiningExportUtils.SecurityMode securityMode = RoleMiningExportUtils.SecurityMode.ADVANCED;
    public ObjectFilter roleQuery;
    public ObjectFilter orgQuery;
    public ObjectFilter userQuery;
    int rolesIterator = 0;
    int organizationsIterator = 0;
    int membersIterator = 0;
    boolean orgExport = true;
    String key;

    @NotNull
    private OrgType getPreparedOrgObject(@NotNull FocusType object, PageBase pageBase) {
        OrgType org = new OrgType();
        org.setName(encryptOrgName(object.getName().toString(), organizationsIterator++, nameMode, key));
        org.setOid(encryptedUUID(object.getOid(), securityMode, key));

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())
                    && filterAllowedOrg(targetRef.getOid(), pageBase)) {
                org.getAssignment().add(encryptObjectReference(assignmentObject, securityMode, key));
            }
        }

        return org;
    }

    @NotNull
    private UserType getPreparedUserObject(@NotNull FocusType object, PageBase pageBase) {
        UserType user = new UserType();
        List<AssignmentType> assignment = object.getAssignment();
        if (assignment == null || assignment.isEmpty()) {
            return user;
        }

        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();

            if (targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())
                    && filterAllowedRole(targetRef.getOid(), pageBase)) {
                user.getAssignment().add(encryptObjectReference(assignmentObject, securityMode, key));
            }

            if (orgExport && targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())
                    && filterAllowedOrg(targetRef.getOid(), pageBase)) {
                user.getAssignment().add(encryptObjectReference(assignmentObject, securityMode, key));
            }

        }

        user.setName(encryptUserName(object.getName().toString(), membersIterator++, nameMode, key));
        user.setOid(encryptedUUID(object.getOid(), securityMode, key));

        return user;
    }

    @NotNull
    private RoleType getPreparedRoleObject(@NotNull FocusType object, PageBase pageBase) {
        RoleType role = new RoleType();
        String roleName = object.getName().toString();
        PolyStringType encryptedName = encryptRoleName(roleName, rolesIterator++, nameMode, key);
        role.setName(encryptedName);
        role.setOid(encryptedUUID(object.getOid(), securityMode, key));

        String identifier = "";

        List<AssignmentType> inducement = ((RoleType) object).getInducement();

        for (AssignmentType inducementObject : inducement) {
            ObjectReferenceType targetRef = inducementObject.getTargetRef();
            if (targetRef != null
                    && targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())
                    && filterAllowedRole(targetRef.getOid(), pageBase)) {
                role.getInducement().add(encryptObjectReference(inducementObject, securityMode, key));

            }
        }

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(ArchetypeType.class.getSimpleName())) {
                AssignmentType assignmentType = new AssignmentType();
                if (targetRef.getOid().equals(applicationArchetypeOid)) {
                    identifier = APPLICATION_ROLE_IDENTIFIER;
                    assignmentType.targetRef(assignmentObject.getTargetRef());
                    role.getAssignment().add(assignmentType);
                } else if (targetRef.getOid().equals(businessArchetypeOid)) {
                    identifier = BUSINESS_ROLE_IDENTIFIER;
                    assignmentType.targetRef(assignmentObject.getTargetRef());
                    role.getAssignment().add(assignmentType);
                }
            }

        }

        if (!identifier.isEmpty()) {
            role.setIdentifier(identifier);
        } else {
            String prefixCheckedIdentifier = RoleMiningExportUtils.determineRoleCategory(roleName, applicationRolePrefix,
                    businessRolePrefix, applicationRoleSuffix, businessRoleSuffix);
            if (prefixCheckedIdentifier != null) {
                role.setIdentifier(prefixCheckedIdentifier);
            }
        }

        return role;
    }

    private boolean filterAllowedOrg(String oid, PageBase pageBase) {

        if (orgQuery == null) {
            return true;
        }

        ObjectQuery objectQuery = pageBase.getPrismContext().queryFactory().createQuery(orgQuery);
        objectQuery.addFilter(pageBase.getPrismContext().queryFor(OrgType.class).id(oid).buildFilter());
        try {

            return !pageBase.getRepositoryService().searchObjects(OrgType.class,
                    objectQuery, null, result).isEmpty();
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Failed to search organization object. ", e);
        }
        return false;
    }

    private boolean filterAllowedRole(String oid, PageBase pageBase) {
        if (roleQuery == null) {
            return true;
        }

        ObjectQuery objectQuery = pageBase.getPrismContext().queryFactory().createQuery(roleQuery);
        objectQuery.addFilter(pageBase.getPrismContext().queryFor(RoleType.class).id(oid).buildFilter());
        try {
            return !pageBase.getRepositoryService().searchObjects(RoleType.class,
                    objectQuery, null, result).isEmpty();
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Failed to search role object. ", e);
        }
        return false;
    }

    public void setBusinessRoleIdentifier(String businessArchetypeOid, List<String> businessRolePrefix,
            List<String> businessRoleSuffix) {
        RoleMiningExportOperation.businessArchetypeOid = businessArchetypeOid;
        this.businessRolePrefix = businessRolePrefix;
        this.businessRoleSuffix = businessRoleSuffix;
    }

    public void setApplicationRoleIdentifiers(String applicationArchetypeOid, List<String> applicationRolePrefix,
            List<String> applicationRoleSuffix) {
        RoleMiningExportOperation.applicationArchetypeOid = applicationArchetypeOid;
        this.applicationRolePrefix = applicationRolePrefix;
        this.applicationRoleSuffix = applicationRoleSuffix;
    }

    public void dumpMining(final Writer writer, OperationResult result, final PageBase page) throws Exception {
        dumpRoleTypeMining(writer, result, page);
        dumpUserTypeMining(writer, result, page);
        if (isOrgExport()) {
            dumpOrgTypeMining(writer, result, page);
        }
    }

    private void dumpRoleTypeMining(final Writer writer, OperationResult result, final @NotNull PageBase page) throws Exception {

        ResultHandler<RoleType> handler = (object, parentResult) -> {
            try {
                RoleType roleType = getPreparedRoleObject(object.asObjectable(), page);
                String xml = page.getPrismContext().xmlSerializer().serialize(roleType.asPrismObject());
                writer.write('\t');
                writer.write(xml);
                writer.write('\n');

            } catch (IOException | SchemaException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }
            return true;
        };

        ModelService service = page.getModelService();
        GetOperationOptionsBuilder optionsBuilder = page.getSchemaService().getOperationOptionsBuilder()
                .raw();

        ObjectQuery objectQuery = page.getPrismContext().queryFactory().createQuery(roleQuery);
        service.searchObjectsIterative(RoleType.class, objectQuery, handler, optionsBuilder.build(),
                page.createSimpleTask(OPERATION_SEARCH_OBJECT), result);
    }

    private void dumpUserTypeMining(final Writer writer, OperationResult result, @NotNull PageBase page) throws Exception {

        ResultHandler<UserType> handler = (object, parentResult) -> {
            try {
                UserType user = getPreparedUserObject(object.asObjectable(), page);
                if (user.getAssignment() != null && !user.getAssignment().isEmpty()) {
                    String xml = page.getPrismContext().xmlSerializer().serialize(user.asPrismObject());
                    writer.write('\t');
                    writer.write(xml);
                    writer.write('\n');
                }
            } catch (IOException | SchemaException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }
            return true;
        };

        ModelService service = page.getModelService();
        GetOperationOptionsBuilder optionsBuilder = page.getSchemaService().getOperationOptionsBuilder()
                .raw();

        ObjectQuery objectQuery = page.getPrismContext().queryFactory().createQuery(userQuery);
        service.searchObjectsIterative(UserType.class, objectQuery, handler, optionsBuilder.build(),
                page.createSimpleTask(OPERATION_SEARCH_OBJECT), result);
    }

    private void dumpOrgTypeMining(final Writer writer, OperationResult result, @NotNull PageBase page) throws Exception {

        ResultHandler<OrgType> handler = (object, parentResult) -> {
            try {
                OrgType orgObject = getPreparedOrgObject(object.asObjectable(), page);
                String xml = page.getPrismContext().xmlSerializer().serialize(orgObject.asPrismObject());
                writer.write('\t');
                writer.write(xml);
                writer.write('\n');

            } catch (IOException | SchemaException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }
            return true;
        };

        ModelService service = page.getModelService();
        GetOperationOptionsBuilder optionsBuilder = page.getSchemaService().getOperationOptionsBuilder()
                .raw();

        ObjectQuery objectQuery = page.getPrismContext().queryFactory().createQuery(orgQuery);
        service.searchObjectsIterative(OrgType.class, objectQuery, handler, optionsBuilder.build(),
                page.createSimpleTask(OPERATION_SEARCH_OBJECT), result);
    }

    public void setNameModeExport(RoleMiningExportUtils.NameMode nameMode) {
        this.nameMode = nameMode;
    }

    public boolean isOrgExport() {
        return orgExport;
    }

    public void setOrgExport(boolean orgExport) {
        this.orgExport = orgExport;
    }

    public void setQueryParameters(ObjectFilter roleQuery, ObjectFilter orgQuery, ObjectFilter userQuery) {
        this.roleQuery = roleQuery;
        this.orgQuery = orgQuery;
        this.userQuery = userQuery;
    }

    public void setSecurityLevel(RoleMiningExportUtils.SecurityMode securityMode) {
        this.securityMode = securityMode;
    }

    public void setKey(String key) {
        this.key = key;
    }

}
