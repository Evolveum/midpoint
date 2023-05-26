/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import static com.evolveum.midpoint.common.RoleMiningExportUtils.*;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.RoleMiningExportUtils;
import com.evolveum.midpoint.ninja.action.worker.AbstractWriterConsumerWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ExportMiningConsumerWorker extends AbstractWriterConsumerWorker<ExportMiningOptions, FocusType> {

    OperationResult operationResult = new OperationResult(DOT_CLASS + "searchObjectByCondition");

    private PrismSerializer<String> serializer;
    private int processedRoleIterator = 0;
    private int processedUserIterator = 0;
    private int processedOrgIterator = 0;

    private boolean orgAllowed;
    private boolean firstObject = true;
    private boolean jsonFormat = false;

    private RoleMiningExportUtils.SecurityMode securityMode;
    private RoleMiningExportUtils.NameMode nameMode;

    private String encryptKey;
    private ObjectFilter filterRole;
    private ObjectFilter filterOrg;

    private static String applicationArchetypeOid;
    private static String businessArchetypeOid;
    private List<String> applicationRolePrefix;
    private List<String> applicationRoleSuffix;
    private List<String> businessRolePrefix;
    private List<String> businessRoleSuffix;

    public ExportMiningConsumerWorker(NinjaContext context, ExportMiningOptions options, BlockingQueue<FocusType> queue,
            OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void init() {
        loadFilters(options.getRoleFilter(), options.getOrgFilter());
        loadRoleCategoryIdentifiers();

        securityMode = options.getSecurityLevel();
        encryptKey = RoleMiningExportUtils.updateEncryptKey(securityMode);
        orgAllowed = options.isIncludeOrg();

        nameMode = options.getNameMode();

        SerializationOptions serializationOptions = SerializationOptions.createSerializeForExport()
                .serializeReferenceNames(true)
                .serializeForExport(true)
                .skipContainerIds(true);

        jsonFormat = options.getOutput().getName().endsWith(".json");
        if (jsonFormat) {
            serializer = context.getPrismContext().jsonSerializer().options(serializationOptions);
        } else {
            serializer = context.getPrismContext().xmlSerializer().options(serializationOptions);
        }
    }

    @Override
    protected String getProlog() {
        if (jsonFormat) {
            return NinjaUtils.JSON_OBJECTS_PREFIX;
        }
        return NinjaUtils.XML_OBJECTS_PREFIX;
    }

    @Override
    protected String getEpilog() {
        if (jsonFormat) {
            return NinjaUtils.JSON_OBJECTS_SUFFIX;
        }
        return NinjaUtils.XML_OBJECTS_SUFFIX;
    }

    @Override
    protected void write(Writer writer, @NotNull FocusType object) throws SchemaException, IOException {
        if (object.asPrismObject().isOfType(RoleType.class)) {
            RoleType role = getPreparedRoleObject(object);
            write(writer, role.asPrismContainerValue());
        } else if (object.asPrismObject().isOfType(UserType.class)) {
            UserType user = getPreparedUserObject(object);
            if (user.getAssignment() != null && !user.getAssignment().isEmpty()) {
                write(writer, user.asPrismContainerValue());
            }
        } else if (object.asPrismObject().isOfType(OrgType.class)) {
            OrgType org = getPreparedOrgObject(object);
            write(writer, org.asPrismContainerValue());
        }
    }

    private void write(Writer writer, PrismContainerValue<?> prismContainerValue) throws SchemaException, IOException {
        String xml = serializer.serialize(prismContainerValue);
        if (jsonFormat && !firstObject) {
            writer.write(",\n" + xml);
        } else {
            writer.write(xml);
        }
        firstObject = false;
    }

    @NotNull
    private OrgType getPreparedOrgObject(@NotNull FocusType object) {
        OrgType org = new OrgType();
        org.setName(encryptOrgName(object.getName().toString(), processedOrgIterator++, nameMode, encryptKey));
        org.setOid(encryptedUUID(object.getOid(), securityMode, encryptKey));

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())
                    && filterAllowedOrg(targetRef.getOid())) {
                org.getAssignment().add(encryptObjectReference(assignmentObject, securityMode, encryptKey));
            }
        }

        return org;
    }

    @NotNull
    private UserType getPreparedUserObject(@NotNull FocusType object) {
        UserType user = new UserType();

        List<AssignmentType> assignment = object.getAssignment();
        if (assignment == null || assignment.isEmpty()) {
            return new UserType();
        }

        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();

            if (targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())
                    && filterAllowedRole(targetRef.getOid())) {
                user.getAssignment().add(encryptObjectReference(assignmentObject, securityMode, encryptKey));
            }

            if (orgAllowed && targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())
                    && filterAllowedOrg(targetRef.getOid())) {
                user.getAssignment().add(encryptObjectReference(assignmentObject, securityMode, encryptKey));
            }

        }

        user.setName(encryptUserName(object.getName().toString(), processedUserIterator++, nameMode, encryptKey));
        user.setOid(encryptedUUID(object.getOid(), securityMode, encryptKey));

        return user;
    }

    @NotNull
    private RoleType getPreparedRoleObject(@NotNull FocusType object) {
        RoleType role = new RoleType();
        String roleName = object.getName().toString();
        PolyStringType encryptedName = encryptRoleName(roleName, processedRoleIterator++, nameMode, encryptKey);
        role.setName(encryptedName);
        role.setOid(encryptedUUID(object.getOid(), securityMode, encryptKey));

        String identifier = "";

        List<AssignmentType> inducement = ((RoleType) object).getInducement();

        for (AssignmentType inducementObject : inducement) {
            ObjectReferenceType targetRef = inducementObject.getTargetRef();
            if (targetRef != null
                    && targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())
                    && filterAllowedRole(targetRef.getOid())) {
                role.getInducement().add(encryptObjectReference(inducementObject, securityMode, encryptKey));

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
            String prefixCheckedIdentifier = determineRoleCategory(roleName, applicationRolePrefix, businessRolePrefix,
                    applicationRoleSuffix, businessRoleSuffix);
            if (prefixCheckedIdentifier != null) {
                role.setIdentifier(prefixCheckedIdentifier);
            }
        }

        return role;
    }

    private boolean filterAllowedOrg(String oid) {
        if (filterOrg == null) {
            return true;
        }

        ObjectQuery objectQuery = context.getPrismContext().queryFactory().createQuery(filterOrg);
        objectQuery.addFilter(context.getPrismContext().queryFor(OrgType.class).id(oid).buildFilter());
        try {
            return !context.getRepository().searchObjects(OrgType.class,
                    objectQuery, null, operationResult).isEmpty();
        } catch (SchemaException e) {
            context.getLog().error("Failed to search organization object. ", e);
        }
        return false;
    }

    private boolean filterAllowedRole(String oid) {
        if (filterRole == null) {
            return true;
        }

        ObjectQuery objectQuery = context.getPrismContext().queryFactory().createQuery(filterRole);
        objectQuery.addFilter(context.getPrismContext().queryFor(RoleType.class).id(oid).buildFilter());
        try {
            return !context.getRepository().searchObjects(RoleType.class,
                    objectQuery, null, operationResult).isEmpty();
        } catch (SchemaException e) {
            context.getLog().error("Failed to search role object. ", e);

        }
        return false;
    }

    private void loadFilters(FileReference roleFileReference, FileReference orgFileReference) {
        try {
            this.filterRole = NinjaUtils.createObjectFilter(roleFileReference, context, RoleType.class);
            this.filterOrg = NinjaUtils.createObjectFilter(orgFileReference, context, OrgType.class);
        } catch (IOException | SchemaException e) {
            context.getLog().error("Failed to crate object filter. ", e);
        }
    }

    private void loadRoleCategoryIdentifiers() {
        applicationArchetypeOid = options.getApplicationRoleArchetypeOid();
        businessArchetypeOid = options.getBusinessRoleArchetypeOid();

        this.applicationRolePrefix = options.getApplicationRolePrefix();
        this.applicationRoleSuffix = options.getApplicationRoleSuffix();
        this.businessRolePrefix = options.getBusinessRolePrefix();
        this.businessRoleSuffix = options.getBusinessRoleSuffix();
    }
}
