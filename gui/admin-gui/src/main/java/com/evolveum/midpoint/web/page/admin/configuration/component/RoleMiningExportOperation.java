/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleMiningExportOperation implements Serializable {

    private static final String APPLICATION_ROLE_PREFIX = "Application role";
    private static final String BUSINESS_ROLE_PREFIX = "Business role";

    private static final String MINING_EXPORT_SUFFIX = "_AE";
    public static String applicationArchetypeOid;
    public static String businessArchetypeOid;

    protected String key;
    protected List<String> applicationRolePrefix;
    protected List<String> applicationRoleSuffix;
    protected List<String> businessRolePrefix;
    protected List<String> businessRoleSuffix;
    NameModeExport nameModeExport = NameModeExport.SEQUENTIAL;
    SecurityLevel securityLevel = SecurityLevel.STRONG;

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

    int rolesIterator = 0;
    int organizationsIterator = 0;
    int membersIterator = 0;
    boolean orgExport = true;

    public enum NameModeExport {
        ENCRYPTED("ENCRYPTED"),
        SEQUENTIAL("SEQUENTIAL"),
        ORIGINAL("ORIGINAL");

        private final String displayString;

        NameModeExport(String displayString) {
            this.displayString = displayString;
        }

        public String getDisplayString() {
            return displayString;
        }
    }

    public enum SecurityLevel {
        STANDARD("STANDARD"),
        STRONG("STRONG");
        private final String displayString;

        SecurityLevel(String displayString) {
            this.displayString = displayString;
        }

        public String getDisplayString() {
            return displayString;
        }
    }

    @NotNull
    private OrgType getPreparedOrgObject(@NotNull FocusType object) {
        organizationsIterator++;
        OrgType org = new OrgType();
        org.setName(getEncryptedOrgName(object.getName().toString(), organizationsIterator));
        org.setOid(getEncryptedUUID(object.getOid()));

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())) {
                org.getAssignment().add(getEncryptedObjectReference(assignmentObject));
            }

        }
        return org;
    }

    @NotNull
    private UserType getPreparedUserObject(@NotNull FocusType object) {
        membersIterator++;
        UserType user = new UserType();
        user.setName(getEncryptedUserName(object.getName().toString(), membersIterator));
        user.setOid(getEncryptedUUID(object.getOid()));

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())) {
                user.getAssignment().add(getEncryptedObjectReference(assignmentObject));

            }

            if (isOrgExport() && targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())) {
                user.getAssignment().add(getEncryptedObjectReference(assignmentObject));
            }

        }
        return user;
    }

    @NotNull
    private RoleType getPreparedRoleObject(@NotNull FocusType object) {
        this.rolesIterator++;

        RoleType role = new RoleType();
        String roleName = object.getName().toString();
        PolyStringType encryptedName = getEncryptedRoleName(roleName, rolesIterator);
        role.setName(encryptedName);
        role.setOid(getEncryptedUUID(object.getOid()));

        String identifier = "";

        List<AssignmentType> inducement = ((RoleType) object).getInducement();

        for (AssignmentType inducementObject : inducement) {
            ObjectReferenceType targetRef = inducementObject.getTargetRef();
            if (targetRef != null && targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())) {
                role.getInducement().add(getEncryptedObjectReference(inducementObject));

            }
        }

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(ArchetypeType.class.getSimpleName())) {
                AssignmentType assignmentType = new AssignmentType();
                if (targetRef.getOid().equals(applicationArchetypeOid)) {
                    identifier = APPLICATION_ROLE_PREFIX;
                    role.setName(PolyStringType.fromOrig(String.valueOf(encryptedName)));
                    assignmentType.targetRef(assignmentObject.getTargetRef());
                    role.getAssignment().add(assignmentType);
                } else if (targetRef.getOid().equals(businessArchetypeOid)) {
                    identifier = BUSINESS_ROLE_PREFIX;
                    role.setName(PolyStringType.fromOrig(String.valueOf(encryptedName)));
                    assignmentType.targetRef(assignmentObject.getTargetRef());
                    role.getAssignment().add(assignmentType);
                }
            }

        }

        if (!identifier.isEmpty()) {
            role.setIdentifier(identifier);
        } else {
            String prefixCheckedIdentifier = getRoleCategory(roleName);
            if (!prefixCheckedIdentifier.isEmpty()) {
                role.setIdentifier(prefixCheckedIdentifier);
            }
        }

        return role;
    }

    public byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        if (securityLevel.equals(SecurityLevel.STANDARD)) {
            buffer = ByteBuffer.allocate(16);
        }
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private String getEncryptedUUID(String oid) {
        UUID uuid = UUID.fromString(oid);
        byte[] bytes = uuidToBytes(uuid);
        return UUID.nameUUIDFromBytes(encryptOid(bytes).getBytes()).toString();
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

    private PolyStringType getEncryptedUserName(String name, int iterator) {
        if (getNameModeExport().equals(NameModeExport.ENCRYPTED)) {
            return PolyStringType.fromOrig(encrypt(name) + MINING_EXPORT_SUFFIX);
        } else if (getNameModeExport().equals(NameModeExport.SEQUENTIAL)) {
            return PolyStringType.fromOrig("User" + iterator + MINING_EXPORT_SUFFIX);
        } else {
            return PolyStringType.fromOrig(name + MINING_EXPORT_SUFFIX);
        }
    }

    private PolyStringType getEncryptedOrgName(String name, int iterator) {
        if (getNameModeExport().equals(NameModeExport.ENCRYPTED)) {
            return PolyStringType.fromOrig(encrypt(name) + MINING_EXPORT_SUFFIX);
        } else if (getNameModeExport().equals(NameModeExport.SEQUENTIAL)) {
            return PolyStringType.fromOrig("Organization" + iterator + MINING_EXPORT_SUFFIX);
        } else {
            return PolyStringType.fromOrig(name + MINING_EXPORT_SUFFIX);
        }
    }

    private PolyStringType getEncryptedRoleName(String name, int iterator) {

        if (getNameModeExport().equals(NameModeExport.ENCRYPTED)) {
            return PolyStringType.fromOrig(encrypt(name) + MINING_EXPORT_SUFFIX);
        } else if (getNameModeExport().equals(NameModeExport.SEQUENTIAL)) {
            return PolyStringType.fromOrig("Role" + iterator + MINING_EXPORT_SUFFIX);
        } else {
            return PolyStringType.fromOrig(name + MINING_EXPORT_SUFFIX);
        }

    }

    private String getRoleCategory(String name) {
        StringBuilder prefix = new StringBuilder();
        if (applicationRolePrefix != null && !applicationRolePrefix.isEmpty()) {
            if (applicationRolePrefix.stream().anyMatch(rolePrefix -> name.toLowerCase().startsWith(rolePrefix.toLowerCase()))) {
                prefix.append(APPLICATION_ROLE_PREFIX);
            }
        }
        if (applicationRoleSuffix != null && !applicationRoleSuffix.isEmpty()) {
            if (applicationRoleSuffix.stream().anyMatch(roleSuffix -> name.toLowerCase().endsWith(roleSuffix.toLowerCase()))) {
                prefix.append(APPLICATION_ROLE_PREFIX);
            }
        }
        if (businessRolePrefix != null && !businessRolePrefix.isEmpty()) {
            if (businessRolePrefix.stream().anyMatch(rolePrefix -> name.toLowerCase().startsWith(rolePrefix.toLowerCase()))) {
                prefix.append(BUSINESS_ROLE_PREFIX);
            }
        }
        if (businessRoleSuffix != null && !businessRoleSuffix.isEmpty()) {
            if (businessRoleSuffix.stream().anyMatch(roleSuffix -> name.toLowerCase().endsWith(roleSuffix.toLowerCase()))) {
                prefix.append(BUSINESS_ROLE_PREFIX);
            }
        }

        return String.valueOf(prefix);
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

    public String generateRandomKey(SecurityLevel securityLevel) {
        int keyLength = 32;
        if (securityLevel.equals(SecurityLevel.STANDARD)) {
            keyLength = 16;
        }
        return RandomStringUtils.random(keyLength, 0, 0, true, true, null, new SecureRandom());
    }

    public void setApplicationRolePrefix(List<String> applicationRolePrefix) {
        this.applicationRolePrefix = applicationRolePrefix;
    }

    public void setApplicationRoleSuffix(List<String> applicationRoleSuffix) {
        this.applicationRoleSuffix = applicationRoleSuffix;
    }

    public void setBusinessRolePrefix(List<String> businessRolePrefix) {
        this.businessRolePrefix = businessRolePrefix;
    }

    public void setBusinessRoleSuffix(List<String> businessRoleSuffix) {
        this.businessRoleSuffix = businessRoleSuffix;
    }

    public void dumpMining(final Writer writer, OperationResult result, final PageBase page) throws Exception {
        dumpRoleTypeMining(writer, result, page);
        dumpUserTypeMining(writer, result, page);
        if (isOrgExport()) {
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

    public static void setApplicationArchetypeOid(String applicationArchetypeOid) {
        RoleMiningExportOperation.applicationArchetypeOid = applicationArchetypeOid;
    }

    public static void setBusinessArchetypeOid(String businessArchetypeOid) {
        RoleMiningExportOperation.businessArchetypeOid = businessArchetypeOid;
    }
}
