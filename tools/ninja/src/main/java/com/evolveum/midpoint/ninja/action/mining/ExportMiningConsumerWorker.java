/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.RandomStringUtils;

import com.evolveum.midpoint.ninja.action.worker.AbstractWriterConsumerWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

public class ExportMiningConsumerWorker extends AbstractWriterConsumerWorker<ExportMiningOptions, FocusType> {

    protected String key;
    private boolean firstObject = true;
    private boolean jsonFormat = false;

    private PrismSerializer<String> serializer;
    private static final String APPLICATION_ROLE_PREFIX = "Application_";
    private static final String BUSINESS_ROLE_PREFIX = "Business_";

    private static final String APPLICATION_ROLE_OID = "00000000-0000-0000-0000-000000000328";
    private static final String BUSINESS_ROLE_OID = "00000000-0000-0000-0000-000000000321";

    protected String applicationRolePrefix;
    protected String applicationRoleSuffix;
    protected String businessRolePrefix;
    protected String businessRoleSuffix;

    public ExportMiningConsumerWorker(NinjaContext context, ExportMiningOptions options, BlockingQueue<FocusType> queue,
            OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void init() {
        this.applicationRolePrefix = options.getApplicationRolePrefix();
        this.applicationRoleSuffix = options.getApplicationRoleSuffix();
        this.businessRolePrefix = options.getBusinessRolePrefix();
        this.businessRoleSuffix = options.getBusinessRoleSuffix();
        key = options.getKey();
        if (options.isRandomKey()) {
            key = generateRandomKey();
        }

        jsonFormat = options.getOutput().getName().endsWith(".json");

        if (!jsonFormat) {
            serializer = context.getPrismContext().xmlSerializer().options(SerializationOptions.createSerializeForExport()
                    .serializeReferenceNames(true).serializeForExport(true).skipContainerIds(true));
        } else {
            serializer = context.getPrismContext().jsonSerializer().options(SerializationOptions.createSerializeForExport()
                    .serializeReferenceNames(true).serializeForExport(true).skipContainerIds(true));
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
            RoleType roleType = getPreparedRoleObject(object);
            write(writer, roleType.asPrismContainerValue());
        } else if (object.asPrismObject().isOfType(UserType.class)) {
            UserType userType = getPreparedUserObject(object);
            write(writer, userType.asPrismContainerValue());

        } else if (object.asPrismObject().isOfType(OrgType.class)) {
            OrgType orgObject = getPreparedOrgObject(object);
            write(writer, orgObject.asPrismContainerValue());
        }

    }

    @NotNull
    private OrgType getPreparedOrgObject(@NotNull FocusType object) {
        OrgType orgObject = new OrgType();
        orgObject.setName(getEncryptedName(object.getName().toString()));
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
        UserType userType = new UserType();
        userType.setName(getEncryptedName(object.getName().toString()));
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
        RoleType roleType = new RoleType();

        PolyStringType encryptedName = getEncryptedRoleName(object.getName().toString());
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

    private void write(Writer writer, PrismContainerValue<?> prismContainerValue) throws SchemaException, IOException {
        String xml = serializer.serialize(prismContainerValue);
        if (jsonFormat && !firstObject) {
            writer.write(",\n" + xml);
        } else {
            writer.write(xml);
        }
        firstObject = false;
    }

    private String getEncryptedUUID(String oid) {
        return UUID.nameUUIDFromBytes(encrypt(oid).getBytes()).toString();
    }

    private PolyStringType getEncryptedName(String name) {
            return PolyStringType.fromOrig(encrypt(name));
    }

    private PolyStringType getEncryptedRoleName(String name) {
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
        if (prefix.isEmpty()) {
            return PolyStringType.fromOrig(encrypt(name));
        } else {
            return PolyStringType.fromOrig(prefix + encrypt(name));
        }
    }

    private AssignmentType getEncryptedObjectReference(@NotNull AssignmentType assignmentObject) {
        ObjectReferenceType encryptedTargetRef = assignmentObject.getTargetRef();
        encryptedTargetRef.setOid(getEncryptedUUID(encryptedTargetRef.getOid()));
        return new AssignmentType().targetRef(encryptedTargetRef);
    }

    private String encrypt(String value) {

        if (value == null) {
            return null;
        } else if (getKey() == null && !options.isRandomKey()) {
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

        } catch (Exception e) {
            throw new NinjaException("Error: Invalid key - " + e.getMessage() + ". \n "
                    + "Possible causes: \n" + "- The key is not the right size or format for this operation. \n"
                    + "- The key is not appropriate for the selected algorithm or mode of operation. \n"
                    + "- The key has been damaged or corrupted.");
        }

        return Base64.getEncoder().encodeToString(ciphertext);
    }

    private static @NotNull String generateRandomKey() {
        int keyLength = 16; // specify the desired length of the key in bytes
        return RandomStringUtils.random(keyLength, 0, 0, true, true, null, new SecureRandom());
    }

    private String getKey() {
        return key;
    }
}
