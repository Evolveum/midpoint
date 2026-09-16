/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_JPEG;
import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_PNG;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.EffectiveFileUploadPolicy;
import com.evolveum.midpoint.model.api.authentication.FileUploadConfigurationResolver;
import com.evolveum.midpoint.model.impl.security.GuiProfileCompiler;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileUploadConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileUploadItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageFormatType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Tests per-item file upload policy compilation, resolution, and merge
 * semantics used by compiled GUI profiles.
 */
public class FileUploadConfigurationResolverTest {

    private static final ItemPath JPEG_PHOTO_PATH = ItemPath.create(FocusType.F_JPEG_PHOTO);
    private static final ItemPath DESCRIPTION_PATH = ItemPath.create(ObjectType.F_DESCRIPTION);
    private static final QName EXTENSION_TEST = new QName("test");

    @BeforeClass
    public void initializePrism() throws SchemaException, SAXException, IOException {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testNullConfigurationProducesNoConfiguredPolicies() throws Exception {
        assertTrue(FileUploadConfigurationResolver.compileConfiguredPolicies(null).isEmpty());
    }

    @Test
    public void testMissingItemPathIsRejected() {
        assertThrows(ConfigurationException.class,
                () -> FileUploadConfigurationResolver.compileConfiguredPolicies(
                        configuration(new FileUploadItemConfigurationType()
                                .allowedContentType(MIME_IMAGE_JPEG))));
    }

    @Test
    public void testEmptyItemPathIsRejected() {
        assertThrows(ConfigurationException.class,
                () -> FileUploadConfigurationResolver.compileConfiguredPolicies(
                        configuration(rule(ItemPath.EMPTY_PATH, MIME_IMAGE_JPEG))));
    }

    @Test
    public void testDuplicateEquivalentPathsAreRejected() {
        assertThrows(ConfigurationException.class,
                () -> FileUploadConfigurationResolver.compileConfiguredPolicies(
                        configuration(
                                rule(JPEG_PHOTO_PATH, MIME_IMAGE_JPEG),
                                rule(ItemPath.create(FocusType.F_JPEG_PHOTO), MIME_IMAGE_PNG))));
    }

    @Test
    public void testDuplicatePathsDifferingOnlyByContainerIdAreRejected() {
        assertThrows(ConfigurationException.class,
                () -> FileUploadConfigurationResolver.compileConfiguredPolicies(
                        configuration(
                                rule(assignmentExtensionPath(1L), MIME_IMAGE_JPEG),
                                rule(assignmentExtensionPath(42L), MIME_IMAGE_PNG))));
    }

    @Test
    public void testCheckContentTypeDefaultsToTrue() throws Exception {
        EffectiveFileUploadPolicy policy = singlePolicy(
                configuration(rule(DESCRIPTION_PATH, MIME_IMAGE_JPEG)));

        assertTrue(policy.isContentTypeCheckEnabled());
    }

    @Test
    public void testExplicitCheckContentTypeFalseDisablesValidation() throws Exception {
        EffectiveFileUploadPolicy policy = singlePolicy(
                configuration(rule(DESCRIPTION_PATH)
                        .checkContentType(false)
                        .convertImageTo(ImageFormatType.PNG)
                        .stripMetadata(true)));

        assertFalse(policy.isContentTypeCheckEnabled());
        assertTrue(policy.getAllowedContentTypes().isEmpty());
    }

    @Test
    public void testExplicitJpegPhotoRuleWithCheckingAndNoAllowedTypesGetsDefaults() throws Exception {
        EffectiveFileUploadPolicy policy = singlePolicy(
                configuration(rule(JPEG_PHOTO_PATH)));

        assertTrue(policy.isContentTypeCheckEnabled());
        assertEquals(policy.getAllowedContentTypes(), List.of(MIME_IMAGE_JPEG, MIME_IMAGE_PNG));
    }

    @Test
    public void testOtherItemWithCheckingAndNoAllowedTypesIsRejected() {
        assertThrows(ConfigurationException.class,
                () -> FileUploadConfigurationResolver.compileConfiguredPolicies(
                        configuration(rule(DESCRIPTION_PATH))));
    }

    @Test
    public void testImageProcessingOptionsAreCopiedToEffectivePolicy() throws Exception {
        EffectiveFileUploadPolicy policy = singlePolicy(
                configuration(rule(DESCRIPTION_PATH, MIME_IMAGE_JPEG)
                        .convertImageTo(ImageFormatType.PNG)
                        .stripMetadata(true)));

        assertEquals(policy.getConvertImageTo(), ImageFormatType.PNG);
        assertTrue(policy.isStripMetadata());
    }

    @Test
    public void testConfiguredAllowedMimeTypesArePreserved() throws Exception {
        EffectiveFileUploadPolicy policy = singlePolicy(
                configuration(rule(DESCRIPTION_PATH, MIME_IMAGE_JPEG, "image/*")));

        assertEquals(policy.getAllowedContentTypes(), List.of(MIME_IMAGE_JPEG, "image/*"));
    }

    @Test
    public void testConfiguredEquivalentPathWinsOverBuiltInJpegPhotoPolicy() {
        EffectiveFileUploadPolicy configured = new EffectiveFileUploadPolicy(
                ItemPath.create(FocusType.F_JPEG_PHOTO),
                false,
                List.of("application/octet-stream"),
                ImageFormatType.PNG,
                true);

        EffectiveFileUploadPolicy resolved =
                FileUploadConfigurationResolver.resolve(JPEG_PHOTO_PATH, List.of(configured));

        assertFalse(resolved.isContentTypeCheckEnabled());
        assertEquals(resolved.getAllowedContentTypes(), List.of("application/octet-stream"));
        assertEquals(resolved.getConvertImageTo(), ImageFormatType.PNG);
        assertTrue(resolved.isStripMetadata());
    }

    @Test
    public void testRuntimePathWithContainerIdMatchesConfiguredStructuralPath() throws Exception {
        ItemPath configuredPath = assignmentExtensionPath();
        ItemPath runtimePath1 = assignmentExtensionPath(1L);
        ItemPath runtimePath42 = assignmentExtensionPath(42L);

        List<EffectiveFileUploadPolicy> policies =
                FileUploadConfigurationResolver.compileConfiguredPolicies(
                        configuration(rule(configuredPath, MIME_IMAGE_JPEG)));

        assertEquals(
                FileUploadConfigurationResolver.resolve(runtimePath1, policies).getAllowedContentTypes(),
                List.of(MIME_IMAGE_JPEG));
        assertEquals(
                FileUploadConfigurationResolver.resolve(runtimePath42, policies).getAllowedContentTypes(),
                List.of(MIME_IMAGE_JPEG));
    }

    @Test
    public void testUnmatchedJpegPhotoGetsSecureBuiltInPolicy() {
        EffectiveFileUploadPolicy resolved =
                FileUploadConfigurationResolver.resolve(JPEG_PHOTO_PATH, List.of());

        assertTrue(resolved.isContentTypeCheckEnabled());
        assertEquals(resolved.getAllowedContentTypes(), List.of(MIME_IMAGE_JPEG, MIME_IMAGE_PNG));
        assertNull(resolved.getConvertImageTo());
        assertFalse(resolved.isStripMetadata());
    }

    @Test
    public void testUnmatchedNonJpegPhotoGetsDisabledPolicy() {
        EffectiveFileUploadPolicy resolved =
                FileUploadConfigurationResolver.resolve(DESCRIPTION_PATH, List.of());

        assertFalse(resolved.isContentTypeCheckEnabled());
        assertTrue(resolved.getAllowedContentTypes().isEmpty());
        assertNull(resolved.getConvertImageTo());
        assertFalse(resolved.isStripMetadata());
    }

    @Test
    public void testNullItemPathIsHandledSafely() {
        EffectiveFileUploadPolicy resolved =
                FileUploadConfigurationResolver.resolve(null, List.of());

        assertNull(resolved.getPath());
        assertFalse(resolved.isContentTypeCheckEnabled());
    }

    @Test
    public void testNullConfiguredPolicyListIsHandledSafely() {
        EffectiveFileUploadPolicy resolved =
                FileUploadConfigurationResolver.resolve(DESCRIPTION_PATH, null);

        assertEquals(resolved.getPath(), DESCRIPTION_PATH);
        assertFalse(resolved.isContentTypeCheckEnabled());
    }

    @Test
    public void testLaterGuiConfigurationReplacesEarlierRuleWithEquivalentPath() throws Exception {
        CompiledGuiProfile composite = new CompiledGuiProfile();

        mergeFileUploadConfiguration(composite,
                configuration(rule(JPEG_PHOTO_PATH, MIME_IMAGE_JPEG).convertImageTo(ImageFormatType.JPG)));
        mergeFileUploadConfiguration(composite,
                configuration(rule(ItemPath.create(FocusType.F_JPEG_PHOTO), MIME_IMAGE_PNG).convertImageTo(ImageFormatType.PNG)));

        assertEquals(composite.getFileUploadPolicies().size(), 1);
        EffectiveFileUploadPolicy resolved = composite.getFileUploadPolicy(JPEG_PHOTO_PATH);
        assertEquals(resolved.getAllowedContentTypes(), List.of(MIME_IMAGE_PNG));
        assertEquals(resolved.getConvertImageTo(), ImageFormatType.PNG);
    }

    @Test
    public void testUnrelatedRulesFromEarlierConfigurationsRemain() throws Exception {
        CompiledGuiProfile composite = new CompiledGuiProfile();

        mergeFileUploadConfiguration(composite,
                configuration(rule(JPEG_PHOTO_PATH, MIME_IMAGE_JPEG)));
        mergeFileUploadConfiguration(composite,
                configuration(rule(DESCRIPTION_PATH, MIME_IMAGE_PNG)));

        assertEquals(composite.getFileUploadPolicies().size(), 2);
        assertEquals(composite.getFileUploadPolicy(JPEG_PHOTO_PATH).getAllowedContentTypes(), List.of(MIME_IMAGE_JPEG));
        assertEquals(composite.getFileUploadPolicy(DESCRIPTION_PATH).getAllowedContentTypes(), List.of(MIME_IMAGE_PNG));
    }

    private EffectiveFileUploadPolicy singlePolicy(FileUploadConfigurationType configuration) throws Exception {
        List<EffectiveFileUploadPolicy> policies =
                FileUploadConfigurationResolver.compileConfiguredPolicies(configuration);

        assertEquals(policies.size(), 1);
        return policies.get(0);
    }

    private FileUploadConfigurationType configuration(FileUploadItemConfigurationType... rules) {
        FileUploadConfigurationType configuration = new FileUploadConfigurationType();
        for (FileUploadItemConfigurationType rule : rules) {
            configuration.item(rule);
        }
        return configuration;
    }

    private FileUploadItemConfigurationType rule(ItemPath path, String... allowedContentTypes) {
        FileUploadItemConfigurationType rule = new FileUploadItemConfigurationType()
                .path(new ItemPathType(path));
        for (String allowedContentType : allowedContentTypes) {
            rule.allowedContentType(allowedContentType);
        }
        return rule;
    }

    private ItemPath assignmentExtensionPath() {
        return ItemPath.create(
                AssignmentHolderType.F_ASSIGNMENT,
                AssignmentType.F_EXTENSION,
                EXTENSION_TEST);
    }

    private ItemPath assignmentExtensionPath(long valueId) {
        return ItemPath.create(
                AssignmentHolderType.F_ASSIGNMENT,
                valueId,
                AssignmentType.F_EXTENSION,
                EXTENSION_TEST);
    }

    /**
     * Invokes the focused merge method directly to avoid compiling a complete GUI
     * profile with unrelated dependencies.
     */
    private static void mergeFileUploadConfiguration(
            CompiledGuiProfile composite, FileUploadConfigurationType configuration) throws Exception {
        Method mergeMethod = GuiProfileCompiler.class.getDeclaredMethod(
                "mergeFileUploadConfiguration", CompiledGuiProfile.class, FileUploadConfigurationType.class);
        mergeMethod.setAccessible(true);
        mergeMethod.invoke(new GuiProfileCompiler(), composite, configuration);
    }
}
