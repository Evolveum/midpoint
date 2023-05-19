/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import static com.evolveum.midpoint.repo.api.RepositoryService.LOGGER;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

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
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ExportMiningConsumerWorker extends AbstractWriterConsumerWorker<ExportMiningOptions, FocusType> {

    public enum NameMode {
        ENCRYPTED,
        SEQUENTIAL,
        ORIGINAL,
    }

    public enum SecurityMode {
        STANDARD,
        STRONG,
    }

    OperationResult operationResult = new OperationResult(DOT_CLASS + "searchObjectByCondition");

    private PrismSerializer<String> serializer;
    private static final String APPLICATION_ROLE_IDENTIFIER = "Application role";
    private static final String BUSINESS_ROLE_IDENTIFIER = "Business role";
    private static final String EXPORT_SUFFIX = "_AE";

    private int processedRoleIterator = 0;
    private int processedUserIterator = 0;
    private int processedOrgIterator = 0;

    protected String encryptKey;
    private boolean orgAllowed;
    private boolean firstObject = true;
    private boolean jsonFormat = false;

    private SecurityMode securityMode;
    private NameMode nameMode;

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

        encryptKey = generateRandomKey();

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
        org.setName(getEncryptedOrgName(object.getName().toString(), processedOrgIterator++));
        org.setOid(getEncryptedUUID(object.getOid()));

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();
            if (targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())
                    && filterAllowedOrg(targetRef.getOid())) {
                org.getAssignment().add(getEncryptedObjectReference(assignmentObject));
            }
        }

        return org;
    }

    @NotNull
    private UserType getPreparedUserObject(@NotNull FocusType object) {
        UserType user = new UserType();
        user.setName(getEncryptedUserName(object.getName().toString(), processedUserIterator++));
        user.setOid(getEncryptedUUID(object.getOid()));

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();

            if (targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())
                    && filterAllowedRole(targetRef.getOid())) {
                user.getAssignment().add(getEncryptedObjectReference(assignmentObject));
            }

            if (orgAllowed && targetRef.getType().getLocalPart().equals(OrgType.class.getSimpleName())
                    && filterAllowedOrg(targetRef.getOid())) {
                user.getAssignment().add(getEncryptedObjectReference(assignmentObject));
            }

        }
        return user;
    }

    @NotNull
    private RoleType getPreparedRoleObject(@NotNull FocusType object) {
        RoleType role = new RoleType();
        String roleName = object.getName().toString();
        PolyStringType encryptedName = getEncryptedRoleName(roleName, processedRoleIterator++);
        role.setName(encryptedName);
        role.setOid(getEncryptedUUID(object.getOid()));

        String identifier = "";

        List<AssignmentType> inducement = ((RoleType) object).getInducement();

        for (AssignmentType inducementObject : inducement) {
            ObjectReferenceType targetRef = inducementObject.getTargetRef();
            if (targetRef != null
                    && targetRef.getType().getLocalPart().equals(RoleType.class.getSimpleName())
                    && filterAllowedRole(targetRef.getOid())) {
                role.getInducement().add(getEncryptedObjectReference(inducementObject));

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
            String prefixCheckedIdentifier = getRoleCategory(roleName);
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
            LoggingUtils.logException(LOGGER, "Failed to search organization object. ", e);
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
            LoggingUtils.logException(LOGGER, "Failed to search role object. ", e);
        }
        return false;
    }

    private PolyStringType getEncryptedName(String name, int iterator, String prefix) {
        if (nameMode.equals(NameMode.ENCRYPTED)) {
            return PolyStringType.fromOrig(encrypt(name) + EXPORT_SUFFIX);
        } else if (nameMode.equals(NameMode.SEQUENTIAL)) {
            return PolyStringType.fromOrig(prefix + iterator + EXPORT_SUFFIX);
        } else if (nameMode.equals(NameMode.ORIGINAL)) {
            return PolyStringType.fromOrig(name + EXPORT_SUFFIX);
        }
        return PolyStringType.fromOrig(prefix + iterator + EXPORT_SUFFIX);
    }

    private PolyStringType getEncryptedUserName(String name, int iterator) {
        return getEncryptedName(name, iterator, "User");
    }

    private PolyStringType getEncryptedOrgName(String name, int iterator) {
        return getEncryptedName(name, iterator, "Organization");
    }

    private PolyStringType getEncryptedRoleName(String name, int iterator) {
        return getEncryptedName(name, iterator, "Role");
    }

    private String getRoleCategory(String name) {
        if (applicationRolePrefix != null && !applicationRolePrefix.isEmpty()) {
            if (applicationRolePrefix.stream().anyMatch(rolePrefix -> name.toLowerCase().startsWith(rolePrefix.toLowerCase()))) {
                return APPLICATION_ROLE_IDENTIFIER;
            }
        }

        if (applicationRoleSuffix != null && !applicationRoleSuffix.isEmpty()) {
            if (applicationRoleSuffix.stream().anyMatch(roleSuffix -> name.toLowerCase().endsWith(roleSuffix.toLowerCase()))) {
                return APPLICATION_ROLE_IDENTIFIER;
            }
        }

        if (businessRolePrefix != null && !businessRolePrefix.isEmpty()) {
            if (businessRolePrefix.stream().anyMatch(rolePrefix -> name.toLowerCase().startsWith(rolePrefix.toLowerCase()))) {
                return BUSINESS_ROLE_IDENTIFIER;
            }
        }

        if (businessRoleSuffix != null && !businessRoleSuffix.isEmpty()) {
            if (businessRoleSuffix.stream().anyMatch(roleSuffix -> name.toLowerCase().endsWith(roleSuffix.toLowerCase()))) {
                return BUSINESS_ROLE_IDENTIFIER;
            }
        }
        return null;
    }

    private AssignmentType getEncryptedObjectReference(@NotNull AssignmentType assignmentObject) {
        ObjectReferenceType encryptedTargetRef = assignmentObject.getTargetRef();
        encryptedTargetRef.setOid(getEncryptedUUID(encryptedTargetRef.getOid()));
        return new AssignmentType().targetRef(encryptedTargetRef);
    }

    private String encrypt(String value) {

        if (value == null) {
            return null;
        } else if (getEncryptKey() == null) {
            return value;
        }

        Cipher cipher;
        byte[] ciphertext;
        try {
            byte[] keyBytes = getEncryptKey().getBytes();
            cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            throw new UnsupportedOperationException(getErrorEncryptMessage(e));
        }

        return Base64.getEncoder().encodeToString(ciphertext);
    }

    private String encryptOid(byte[] value) {
        if (getEncryptKey() == null) {
            return new String(value, StandardCharsets.UTF_8);
        }

        Cipher cipher;
        byte[] ciphertext;
        try {
            byte[] keyBytes = getEncryptKey().getBytes();
            cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            ciphertext = cipher.doFinal(value);
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new UnsupportedOperationException(getErrorEncryptMessage(e));
        }
    }

    private @NotNull String getErrorEncryptMessage(@NotNull Exception e) {
        return "Error: Invalid key - Possible causes:\n"
                + "- The key is not the right size or format for this operation.\n"
                + "- The key is not appropriate for the selected algorithm or mode of operation.\n"
                + "- The key has been damaged or corrupted.\n"
                + "Error message: " + e.getMessage();
    }

    private String getEncryptedUUID(String oid) {
        UUID uuid = UUID.fromString(oid);
        byte[] bytes = uuidToBytes(uuid);
        return UUID.nameUUIDFromBytes(encryptOid(bytes).getBytes()).toString();
    }

    private byte @NotNull [] uuidToBytes(@NotNull UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(getByteSize(securityMode));
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private @NotNull String generateRandomKey() {
        int keyLength = getByteSize(securityMode);
        return RandomStringUtils.random(keyLength, 0, 0, true, true, null, new SecureRandom());
    }

    private String getEncryptKey() {
        return encryptKey;
    }

    public int getByteSize(@NotNull SecurityMode securityMode) {
        if (securityMode.equals(SecurityMode.STANDARD)) {
            return 16;
        } else {
            return 32;
        }
    }

    private void loadFilters(FileReference roleFileReference, FileReference orgFileReference) {
        try {
            this.filterRole = NinjaUtils.createObjectFilter(roleFileReference, context, RoleType.class);
            this.filterOrg = NinjaUtils.createObjectFilter(orgFileReference, context, OrgType.class);
        } catch (IOException | SchemaException e) {
            LoggingUtils.logException(LOGGER, "Failed to crate object filter. ", e);
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
