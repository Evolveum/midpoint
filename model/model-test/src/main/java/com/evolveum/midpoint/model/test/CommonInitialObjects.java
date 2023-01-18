/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test;

import java.io.IOException;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.AbstractTestResource;
import com.evolveum.midpoint.test.ClassPathTestResource;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

/**
 * Definition of commonly used initial objects be used in tests on or above the `model` level.
 *
 * TODO Should this class be limited to tags? Or should it cover more initial objects? Is this a good idea, after all?
 */
@Experimental
public interface CommonInitialObjects {

    String INITIAL_OBJECTS = "initial-objects";

    String TAGS = INITIAL_OBJECTS + "/tag";

    String ARCHETYPES = INITIAL_OBJECTS + "/archetype";

    String FUNCTION_LIBRARY = INITIAL_OBJECTS + "/function-library";

    AbstractTestResource<ArchetypeType> STANDARD_FUNCTIONS = new ClassPathTestResource<>(
            FUNCTION_LIBRARY, "005-standard-functions.xml",
            SystemObjectsType.STANDARD_FUNCTIONS.value());

    AbstractTestResource<ArchetypeType> ARCHETYPE_EVENT_TAG = new ClassPathTestResource<>(
            ARCHETYPES, "700-archetype-event-tag.xml",
            SystemObjectsType.ARCHETYPE_EVENT_TAG.value());

    AbstractTestResource<ArchetypeType> ARCHETYPE_POLICY_SITUATION = new ClassPathTestResource<>(
            ARCHETYPES, "701-archetype-policy-situation.xml",
            SystemObjectsType.ARCHETYPE_POLICY_SITUATION.value());

    AbstractTestResource<TagType> TAG_FOCUS_ENABLED = new ClassPathTestResource<>(
            TAGS, "710-tag-focus-enabled.xml",
            SystemObjectsType.TAG_FOCUS_ENABLED.value());

    AbstractTestResource<TagType> TAG_FOCUS_DISABLED = new ClassPathTestResource<>(
            TAGS, "711-tag-focus-disabled.xml",
            SystemObjectsType.TAG_FOCUS_DISABLED.value());

    AbstractTestResource<TagType> TAG_FOCUS_NAME_CHANGED = new ClassPathTestResource<>(
            TAGS, "712-tag-focus-renamed.xml",
            SystemObjectsType.TAG_FOCUS_NAME_CHANGED.value());

    AbstractTestResource<TagType> TAG_FOCUS_ASSIGNMENT_CHANGED = new ClassPathTestResource<>(
            TAGS, "713-tag-focus-assignment-changed.xml",
            SystemObjectsType.TAG_FOCUS_ASSIGNMENT_CHANGED.value());

    AbstractTestResource<TagType> TAG_FOCUS_ARCHETYPE_CHANGED = new ClassPathTestResource<>(
            TAGS, "714-tag-focus-archetype-changed.xml",
            SystemObjectsType.TAG_FOCUS_ARCHETYPE_CHANGED.value());

    AbstractTestResource<TagType> TAG_FOCUS_PARENT_ORG_REFERENCE_CHANGED = new ClassPathTestResource<>(
            TAGS, "715-tag-focus-parent-org-reference-changed.xml",
            SystemObjectsType.TAG_FOCUS_PARENT_ORG_REFERENCE_CHANGED.value());

    AbstractTestResource<TagType> TAG_FOCUS_ROLE_MEMBERSHIP_CHANGED = new ClassPathTestResource<>(
            TAGS, "716-tag-focus-role-membership-changed.xml",
            SystemObjectsType.TAG_FOCUS_ROLE_MEMBERSHIP_CHANGED.value());

    AbstractTestResource<TagType> TAG_PROJECTION_ENABLED = new ClassPathTestResource<>(
            TAGS, "730-tag-projection-enabled.xml",
            SystemObjectsType.TAG_PROJECTION_ENABLED.value());

    AbstractTestResource<TagType> TAG_PROJECTION_DISABLED = new ClassPathTestResource<>(
            TAGS, "731-tag-projection-disabled.xml",
            SystemObjectsType.TAG_PROJECTION_DISABLED.value());

    AbstractTestResource<TagType> TAG_PROJECTION_NAME_CHANGED = new ClassPathTestResource<>(
            TAGS, "732-tag-projection-renamed.xml",
            SystemObjectsType.TAG_PROJECTION_NAME_CHANGED.value());

    AbstractTestResource<TagType> TAG_PROJECTION_IDENTIFIER_CHANGED = new ClassPathTestResource<>(
            TAGS, "733-tag-projection-identifier-changed.xml",
            SystemObjectsType.TAG_PROJECTION_IDENTIFIER_CHANGED.value());

    AbstractTestResource<TagType> TAG_PROJECTION_ENTITLEMENT_CHANGED = new ClassPathTestResource<>(
            TAGS, "734-tag-projection-entitlement-changed.xml",
            SystemObjectsType.TAG_PROJECTION_ENTITLEMENT_CHANGED.value());

    AbstractTestResource<TagType> TAG_PROJECTION_PASSWORD_CHANGED = new ClassPathTestResource<>(
            TAGS, "735-tag-projection-password-changed.xml",
            SystemObjectsType.TAG_PROJECTION_PASSWORD_CHANGED.value());

    /** To be used when needed. */
    static void addTags(AbstractModelIntegrationTest test, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
        if (!test.areTagsSupported()) {
            return;
        }
        test.repoAdd(ARCHETYPE_EVENT_TAG, result);
        test.repoAdd(ARCHETYPE_POLICY_SITUATION, result);
        test.repoAdd(TAG_FOCUS_ENABLED, result);
        test.repoAdd(TAG_FOCUS_DISABLED, result);
        test.repoAdd(TAG_FOCUS_NAME_CHANGED, result);
        test.repoAdd(TAG_FOCUS_ASSIGNMENT_CHANGED, result);
        test.repoAdd(TAG_FOCUS_ARCHETYPE_CHANGED, result);
        test.repoAdd(TAG_FOCUS_PARENT_ORG_REFERENCE_CHANGED, result);
        test.repoAdd(TAG_FOCUS_ROLE_MEMBERSHIP_CHANGED, result);
        test.repoAdd(TAG_PROJECTION_ENABLED, result);
        test.repoAdd(TAG_PROJECTION_DISABLED, result);
        test.repoAdd(TAG_PROJECTION_NAME_CHANGED, result);
        test.repoAdd(TAG_PROJECTION_IDENTIFIER_CHANGED, result);
        test.repoAdd(TAG_PROJECTION_ENTITLEMENT_CHANGED, result);
        test.repoAdd(TAG_PROJECTION_PASSWORD_CHANGED, result);
    }
}
