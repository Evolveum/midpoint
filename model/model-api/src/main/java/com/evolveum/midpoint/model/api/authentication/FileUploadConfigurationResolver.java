/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.authentication;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_JPEG;
import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_PNG;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileUploadConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileUploadItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Compiles configured file upload rules and resolves the effective policy for
 * a particular item path.
 */
public final class FileUploadConfigurationResolver {

    private static final ItemPath JPEG_PHOTO_PATH = ItemPath.create(FocusType.F_JPEG_PHOTO);
    private static final List<String> DEFAULT_JPEG_PHOTO_ALLOWED_TYPES = List.of(MIME_IMAGE_JPEG, MIME_IMAGE_PNG);

    private FileUploadConfigurationResolver() {
    }

    /**
     * Resolves the effective upload policy for the given item path.
     *
     * An explicitly configured rule takes precedence. If no rule matches,
     * the built-in secure policy is used for {@code jpegPhoto}; other items
     * receive a policy with validation and processing disabled.
     *
     * @param itemPath path of the uploaded item
     * @param configuredPolicies compiled configured policies
     * @return effective policy for the item
     */
    public static EffectiveFileUploadPolicy resolve(
            ItemPath itemPath,
            List<EffectiveFileUploadPolicy> configuredPolicies) {

        EffectiveFileUploadPolicy configuredPolicy = findConfiguredPolicy(itemPath, configuredPolicies);

        return configuredPolicy != null
                ? configuredPolicy : createBuiltInPolicy(itemPath);
    }

    /**
     * Compiles schema configuration items into immutable effective policies.
     *
     * @param configuration file upload configuration
     * @return compiled configured policies
     * @throws ConfigurationException if a rule has an empty path, duplicate
     *         path, or enables validation without defining allowed MIME types
     */
    public static List<EffectiveFileUploadPolicy> compileConfiguredPolicies(FileUploadConfigurationType configuration)
            throws ConfigurationException {
        if (configuration == null) {
            return List.of();
        }

        List<EffectiveFileUploadPolicy> policies = new ArrayList<>();
        for (FileUploadItemConfigurationType item : configuration.getItem()) {
            EffectiveFileUploadPolicy policy = compileConfiguredPolicy(item);
            assertNoDuplicatePath(policy.getPath(), policies);
            policies.add(policy);
        }
        return List.copyOf(policies);
    }

    /**
     * Determines whether the path identifies the built-in {@code jpegPhoto}
     * item.
     *
     * @param itemPath item path to check
     * @return {@code true} if the path identifies {@code jpegPhoto}
     */
    public static boolean isJpegPhotoPath(ItemPath itemPath) {
        return itemPath != null && itemPath.equivalent(JPEG_PHOTO_PATH);
    }

    private static EffectiveFileUploadPolicy compileConfiguredPolicy(FileUploadItemConfigurationType item)
            throws ConfigurationException {
        ItemPath path = item.getPath() != null ? item.getPath().getItemPath() : null;
        if (ItemPath.isEmpty(path)) {
            throw new ConfigurationException("File upload configuration item must specify a non-empty path.");
        }

        boolean checkContentType = !Boolean.FALSE.equals(item.isCheckContentType());
        List<String> allowedContentTypes = List.copyOf(item.getAllowedContentType());
        if (checkContentType && allowedContentTypes.isEmpty()) {
            if (isJpegPhotoPath(path)) {
                allowedContentTypes = DEFAULT_JPEG_PHOTO_ALLOWED_TYPES;
            } else {
                throw new ConfigurationException(
                        "File upload configuration for " + path + " enables content type checking but has no allowedContentType.");
            }
        }

        return new EffectiveFileUploadPolicy(
                path,
                checkContentType,
                allowedContentTypes,
                item.getConvertImageTo(),
                Boolean.TRUE.equals(item.isStripMetadata()));
    }

    private static EffectiveFileUploadPolicy findConfiguredPolicy(
            ItemPath itemPath, List<EffectiveFileUploadPolicy> configuredPolicies) {

        if (itemPath == null || configuredPolicies == null) {
            return null;
        }

        ItemPath normalizedItemPath = itemPath.namedSegmentsOnly();
        for (EffectiveFileUploadPolicy policy : configuredPolicies) {
            if (normalizedItemPath.equivalent(policy.getPath())) {
                return policy;
            }
        }

        return null;
    }

    private static void assertNoDuplicatePath(ItemPath path, List<EffectiveFileUploadPolicy> policies)
            throws ConfigurationException {
        for (EffectiveFileUploadPolicy policy : policies) {
            if (path.equivalent(policy.getPath())) {
                throw new ConfigurationException("Duplicate file upload configuration for item path " + path + ".");
            }
        }
    }

    private static EffectiveFileUploadPolicy createBuiltInPolicy(ItemPath itemPath) {

        if (isJpegPhotoPath(itemPath)) {
            return new EffectiveFileUploadPolicy(
                    itemPath,
                    true,
                    DEFAULT_JPEG_PHOTO_ALLOWED_TYPES,
                    null,
                    false);
        }

        return new EffectiveFileUploadPolicy(
                itemPath,
                false,
                List.of(),
                null,
                false);
    }
}
