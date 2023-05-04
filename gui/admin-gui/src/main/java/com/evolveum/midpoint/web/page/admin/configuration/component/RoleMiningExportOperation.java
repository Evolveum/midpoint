/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class RoleMiningExportOperation implements Serializable {

    private static final String APPLICATION_ROLE_PREFIX = "Application_";
    private static final String BUSINESS_ROLE_PREFIX = "Business_";

    private static final String APPLICATION_ROLE_OID = "00000000-0000-0000-0000-000000000328";
    private static final String BUSINESS_ROLE_OID = "00000000-0000-0000-0000-000000000321";

    protected String key;
    protected String applicationRolePrefix;
    protected String applicationRoleSuffix;
    protected String businessRolePrefix;
    protected String businessRoleSuffix;
    NameModeExport nameModeExport = NameModeExport.SEQUENTIAL;
    int rolesIterator = 0;
    int organizationsIterator = 0;
    int membersIterator = 0;
    boolean orgExport = true;


    public enum NameModeExport {
        ENCRYPTED("ENCRYPTED"),
        SEQUENTIAL("SEQUENTIAL"),
        UNECRYPTED("UNECRYPTED");

        private final String displayString;

        NameModeExport(String displayString) {
            this.displayString = displayString;
        }

        public String getDisplayString() {
            return displayString;
        }
    }


    @NotNull
    private OrgType getPreparedOrgObject(@NotNull FocusType object) {
        organizationsIterator++;
        OrgType orgObject = new OrgType();
        orgObject.setName(getEncryptedOrgName(object.getName().toString(), organizationsIterator));
        orgObject.setOid(getEncryptedUUID(object.getOid()));

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())) {
                orgObject.getAssignment().add(getEncryptedObjectReference(assignmentObject));
            }

        }
        return orgObject;
    }

    @NotNull
    private UserType getPreparedUserObject(@NotNull FocusType object) {
        membersIterator++;
        UserType userType = new UserType();
        userType.setName(getEncryptedUserName(object.getName().toString(), membersIterator));
        userType.setOid(getEncryptedUUID(object.getOid()));

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())
                    || targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())) {
                userType.getAssignment().add(getEncryptedObjectReference(assignmentObject));

            }

        }
        return userType;
    }

    @NotNull
    private RoleType getPreparedRoleObject(@NotNull FocusType object) {
        this.rolesIterator++;

        RoleType roleType = new RoleType();

        PolyStringType encryptedName = getEncryptedRoleName(object.getName().toString(), rolesIterator);
        roleType.setName(encryptedName);
        roleType.setOid(getEncryptedUUID(object.getOid()));

        List<AssignmentType> inducement = ((RoleType) object).getInducement();

        for (AssignmentType inducementObject : inducement) {
            ObjectReferenceType targetRef = inducementObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())) {
                roleType.getInducement().add(getEncryptedObjectReference(inducementObject));

            }
        }

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(ArchetypeType.class.getSimpleName())) {
                AssignmentType assignmentType = new AssignmentType();
                if (targetRef.getOid().equals(APPLICATION_ROLE_OID)) {
                    roleType.setName(PolyStringType.fromOrig(APPLICATION_ROLE_PREFIX + encryptedName));
                    assignmentType.targetRef(assignmentObject.getTargetRef());
                    roleType.getAssignment().add(assignmentType);
                } else if (targetRef.getOid().equals(BUSINESS_ROLE_OID)) {
                    roleType.setName(PolyStringType.fromOrig(BUSINESS_ROLE_PREFIX + encryptedName));
                    assignmentType.targetRef(assignmentObject.getTargetRef());
                    roleType.getAssignment().add(assignmentType);
                }
            }

        }
        return roleType;
    }

    private String getEncryptedUUID(String oid) {
        UUID uuid = UUID.fromString(oid);
        byte[] bytes = uuidToBytes(uuid);
        return UUID.nameUUIDFromBytes(encryptOid(bytes).getBytes()).toString();
    }

    private PolyStringType getEncryptedUserName(String name, int iterator) {
        if (getNameModeExport().equals(NameModeExport.ENCRYPTED)) {
            return PolyStringType.fromOrig(encrypt(name));
        } else if (getNameModeExport().equals(NameModeExport.SEQUENTIAL)) {
            return PolyStringType.fromOrig("User" + iterator);
        } else {
            return PolyStringType.fromOrig(name);
        }
    }

    private PolyStringType getEncryptedOrgName(String name, int iterator) {
        if (getNameModeExport().equals(NameModeExport.ENCRYPTED)) {
            return PolyStringType.fromOrig(encrypt(name));
        } else if (getNameModeExport().equals(NameModeExport.SEQUENTIAL)) {
            return PolyStringType.fromOrig("Organization" + iterator);
        } else {
            return PolyStringType.fromOrig(name);
        }
    }

    private PolyStringType getEncryptedRoleName(String name, int iterator) {
        String prefix = getNamePrefix(name);
        if (getNameModeExport().equals(NameModeExport.ENCRYPTED)) {
            return PolyStringType.fromOrig(prefix + encrypt(name));
        } else if (getNameModeExport().equals(NameModeExport.SEQUENTIAL)) {
            return PolyStringType.fromOrig(prefix + "Role" + iterator);
        } else {
            return PolyStringType.fromOrig(name);
        }

    }

    private String getNamePrefix(String name) {
        String prefix = "";
        if (applicationRolePrefix != null && name.startsWith(applicationRolePrefix)) {
            prefix = APPLICATION_ROLE_PREFIX;
        } else if (applicationRoleSuffix != null && name.endsWith(applicationRoleSuffix)) {
            prefix = APPLICATION_ROLE_PREFIX;
        } else if (businessRolePrefix != null && name.startsWith(businessRolePrefix)) {
            prefix = BUSINESS_ROLE_PREFIX;
        } else if (businessRoleSuffix != null && name.endsWith(businessRoleSuffix)) {
            prefix = BUSINESS_ROLE_PREFIX;
        }
        return prefix;
    }

    private AssignmentType getEncryptedObjectReference(AssignmentType assignmentObject) {
        ObjectReferenceType encryptedTargetRef = assignmentObject.getTargetRef();
        encryptedTargetRef.setOid(getEncryptedUUID(encryptedTargetRef.getOid()));
        return new AssignmentType().targetRef(encryptedTargetRef);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private String encryptOid(byte[] value) {
        if (getKey() == null) {
            return new String(value, StandardCharsets.UTF_8);
        }

        Cipher cipher;
        byte[] ciphertext;
        try {
            byte[] keyBytes = getKey().getBytes();
            cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            ciphertext = cipher.doFinal(value);
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Error: Invalid key - " + e.getMessage() + ". \n "
                    + "Possible causes: \n" + "- The key is not the right size or format for this operation. \n"
                    + "- The key is not appropriate for the selected algorithm or mode of operation. \n"
                    + "- The key has been damaged or corrupted.");
        }
    }

    private String encrypt(String value) {

        if (value == null) {
            return null;
        } else if (getKey() == null) {
            return value;
        }

        Cipher cipher;
        byte[] ciphertext;
        try {
            byte[] keyBytes = getKey().getBytes();
            cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Error: Invalid key - " + e.getMessage() + ". \n "
                    + "Possible causes: \n" + "- The key is not the right size or format for this operation. \n"
                    + "- The key is not appropriate for the selected algorithm or mode of operation. \n"
                    + "- The key has been damaged or corrupted.");
        }
    }


    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    public String generateRandomKey() {
        int keyLength = 32;
        return RandomStringUtils.random(keyLength, 0, 0, true, true, null, new SecureRandom());
    }

    public void setApplicationRolePrefix(String applicationRolePrefix) {
        this.applicationRolePrefix = applicationRolePrefix;
    }

    public void setApplicationRoleSuffix(String applicationRoleSuffix) {
        this.applicationRoleSuffix = applicationRoleSuffix;
    }

    public void setBusinessRolePrefix(String businessRolePrefix) {
        this.businessRolePrefix = businessRolePrefix;
    }

    public void setBusinessRoleSuffix(String businessRoleSuffix) {
        this.businessRoleSuffix = businessRoleSuffix;
    }

    public void dumpMining(final Writer writer, OperationResult result, final PageBase page) throws Exception {
        dumpRoleTypeMining(writer, result, page);
        dumpUserTypeMining(writer, result, page);
        if(isOrgExport()) {
            dumpOrgTypeMining(writer, result, page);
        }
    }

    private static final String DOT_CLASS = PageDebugDownloadBehaviour.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECT = DOT_CLASS + "loadObjects";

    private void dumpRoleTypeMining(final Writer writer, OperationResult result, final PageBase page) throws Exception {

        ResultHandler<RoleType> handler = (object, parentResult) -> {
            try {
                RoleType roleType = getPreparedRoleObject(object.asObjectable());
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

        service.searchObjectsIterative(RoleType.class, null, handler, optionsBuilder.build(),
                page.createSimpleTask(OPERATION_SEARCH_OBJECT), result);
    }

    private void dumpUserTypeMining(final Writer writer, OperationResult result, PageBase page) throws Exception {

        ResultHandler<UserType> handler = (object, parentResult) -> {
            try {
                UserType userType = getPreparedUserObject(object.asObjectable());
                String xml = page.getPrismContext().xmlSerializer().serialize(userType.asPrismObject());
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

        service.searchObjectsIterative(UserType.class, null, handler, optionsBuilder.build(),
                page.createSimpleTask(OPERATION_SEARCH_OBJECT), result);
    }

    private void dumpOrgTypeMining(final Writer writer, OperationResult result, PageBase page) throws Exception {

        ResultHandler<OrgType> handler = (object, parentResult) -> {
            try {
                OrgType orgObject = getPreparedOrgObject(object.asObjectable());
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

        service.searchObjectsIterative(OrgType.class, null, handler, optionsBuilder.build(),
                page.createSimpleTask(OPERATION_SEARCH_OBJECT), result);
    }

    public void setNameModeExport(NameModeExport nameModeExport) {
        this.nameModeExport = nameModeExport;
    }

    public NameModeExport getNameModeExport() {
        return nameModeExport;
    }

    public boolean isOrgExport() {
        return orgExport;
    }

    public void setOrgExport(boolean orgExport) {
        this.orgExport = orgExport;
    }
}
