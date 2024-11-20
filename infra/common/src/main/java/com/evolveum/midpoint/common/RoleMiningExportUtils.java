/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.Nullable;

public class RoleMiningExportUtils implements Serializable {

    public static final String APPLICATION_ROLE_IDENTIFIER = "Application role";
    public static final String BUSINESS_ROLE_IDENTIFIER = "Business role";
    private static final String EXPORT_SUFFIX = "_AE";

    public enum NameMode {
        ENCRYPTED("ENCRYPTED"),
        SEQUENTIAL("SEQUENTIAL"),
        ORIGINAL("ORIGINAL");

        private final String displayString;

        NameMode(String displayString) {
            this.displayString = displayString;
        }

        public String getDisplayString() {
            return displayString;
        }
    }

    public enum SecurityMode {
        STANDARD("STANDARD"),
        ADVANCED("ADVANCED");
        private final String displayString;

        SecurityMode(String displayString) {
            this.displayString = displayString;
        }

        public String getDisplayString() {
            return displayString;
        }
    }

    public static class SequentialAnonymizer {
        private final Map<String, String> anonymizedValues = new HashMap<>();
        private final String baseName;
        private int index = 0;

        public SequentialAnonymizer(String baseName) {
            this.baseName = baseName;
        }

        public String anonymize(String value) {
            if (!anonymizedValues.containsKey(value)) {
                anonymizedValues.put(value, baseName + index++);
            }
            return anonymizedValues.get(value);
        }
    }

    private static class ScopedSequentialAnonymizer {
        private final Map<String, SequentialAnonymizer> scopedAnonymizers = new HashMap<>();
        private final String baseName;

        public ScopedSequentialAnonymizer(String baseName) {
            this.baseName = baseName;
        }

        public String anonymize(String scope, String value) {
            if (!scopedAnonymizers.containsKey(scope)) {
                scopedAnonymizers.put(scope, new SequentialAnonymizer(baseName));
            }
            return scopedAnonymizers.get(scope).anonymize(value);
        }
    }

    public static class AttributeValueAnonymizer {
        private final ScopedSequentialAnonymizer sequentialAnonymizer = new ScopedSequentialAnonymizer("att");
        private final NameMode nameMode;
        private final String encryptKey;

        public AttributeValueAnonymizer(NameMode nameMode, String encryptKey) {
            this.nameMode = nameMode;
            this.encryptKey = encryptKey;
        }

        public String anonymize(String attributeName, String attributeValue) {
            var anonymized = switch(nameMode) {
                case ENCRYPTED -> encrypt(attributeValue, encryptKey);
                case SEQUENTIAL -> sequentialAnonymizer.anonymize(attributeName, attributeValue);
                case ORIGINAL -> attributeValue;
            };
            return anonymized + EXPORT_SUFFIX;
        }
    }

    private static PolyStringType encryptName(String name, int iterator, String prefix, @NotNull NameMode nameMode, String key) {
        if (nameMode.equals(NameMode.ENCRYPTED)) {
            return PolyStringType.fromOrig(encrypt(name, key) + EXPORT_SUFFIX);
        } else if (nameMode.equals(NameMode.SEQUENTIAL)) {
            return PolyStringType.fromOrig(prefix + iterator + EXPORT_SUFFIX);
        } else if (nameMode.equals(NameMode.ORIGINAL)) {
            return PolyStringType.fromOrig(name + EXPORT_SUFFIX);
        }
        return PolyStringType.fromOrig(prefix + iterator + EXPORT_SUFFIX);
    }

    public static PolyStringType encryptUserName(String name, int iterator, NameMode nameMode, String key) {
        return encryptName(name, iterator, "User", nameMode, key);
    }

    public static PolyStringType encryptOrgName(String name, int iterator, NameMode nameMode, String key) {
        return encryptName(name, iterator, "Organization", nameMode, key);
    }

    public static PolyStringType encryptRoleName(String name, int iterator, NameMode nameMode, String key) {
        return encryptName(name, iterator, "Role", nameMode, key);
    }

    public static ObjectReferenceType encryptObjectReference(ObjectReferenceType targetRef, SecurityMode securityMode, String key) {
        ObjectReferenceType encryptedTargetRef = targetRef.clone();
        encryptedTargetRef.setOid(encryptedUUID(encryptedTargetRef.getOid(), securityMode, key));
        return encryptedTargetRef;
    }

    public static AssignmentType encryptObjectReference(@NotNull AssignmentType assignmentObject,
            SecurityMode securityMode, String key) {
        ObjectReferenceType encryptedTargetRef = assignmentObject.getTargetRef();
        encryptedTargetRef.setOid(encryptedUUID(encryptedTargetRef.getOid(), securityMode, key));
        return new AssignmentType().targetRef(encryptedTargetRef);
    }

    public static String encryptedUUID(String oid, SecurityMode securityMode, String key) {
        UUID uuid = UUID.fromString(oid);
        byte[] bytes = uuidToBytes(uuid, securityMode);
        return UUID.nameUUIDFromBytes(encryptOid(bytes, key).getBytes()).toString();
    }

    private static byte @NotNull [] uuidToBytes(UUID uuid, @NotNull SecurityMode securityMode) {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        if (securityMode.equals(SecurityMode.STANDARD)) {
            buffer = ByteBuffer.allocate(16);
        }
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private static String encryptOid(byte[] value, String key) {

        if (value == null) {
            return null;
        }
        else if (key == null) {
            return new String(value, StandardCharsets.UTF_8);
        }

        Cipher cipher;
        byte[] ciphertext;
        try {
            byte[] keyBytes = key.getBytes();
            cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            ciphertext = cipher.doFinal(value);
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new UnsupportedOperationException(getErrorEncryptMessage(e));
        }
    }

    private static String encrypt(String value, String key) {

        if (value == null) {
            return null;
        } else if (key == null) {
            return value;
        }

        Cipher cipher;
        byte[] ciphertext;
        try {
            byte[] keyBytes = key.getBytes();
            cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new UnsupportedOperationException(getErrorEncryptMessage(e));
        }
    }

    public static @NotNull String updateEncryptKey(@NotNull SecurityMode securityMode) {
        int keyLength = 32;
        if (securityMode.equals(SecurityMode.STANDARD)) {
            keyLength = 16;
        }

        return RandomStringUtils.random(keyLength, 0, 0, true, true, null,
                new SecureRandom());
    }

    public static @Nullable String determineRoleCategory(String name, List<String> applicationRolePrefix,
            List<String> businessRolePrefix, List<String> applicationRoleSuffix, List<String> businessRoleSuffix) {

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

    private static @NotNull String getErrorEncryptMessage(@NotNull Exception e) {
        return "Error: Invalid key - Possible causes:\n"
                + "- The key is not the right size or format for this operation.\n"
                + "- The key is not appropriate for the selected algorithm or mode of operation.\n"
                + "- The key has been damaged or corrupted.\n"
                + "Error message: " + e.getMessage();
    }

}
