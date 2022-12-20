/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

import static com.evolveum.midpoint.test.AbstractIntegrationTest.COMMON_DIR;

/**
 * Definition of commonly used initial objects be used in model integration tests.
 */
@Experimental
public interface CommonInitialObjects {

    TestResource<ArchetypeType> STANDARD_FUNCTIONS = new TestResource<>(
            COMMON_DIR, "005-standard-functions.xml", SystemObjectsType.STANDARD_FUNCTIONS.value());

    TestResource<ArchetypeType> ARCHETYPE_EVENT_TAG = new TestResource<>(
            COMMON_DIR, "700-archetype-event-tag.xml", SystemObjectsType.ARCHETYPE_EVENT_TAG.value());

    TestResource<ArchetypeType> ARCHETYPE_POLICY_SITUATION = new TestResource<>(
            COMMON_DIR, "701-archetype-policy-situation.xml", SystemObjectsType.ARCHETYPE_POLICY_SITUATION.value());

    TestResource<TagType> TAG_FOCUS_ENABLED = new TestResource<>(
            COMMON_DIR, "710-tag-focus-enabled.xml", SystemObjectsType.TAG_FOCUS_ENABLED.value());

    TestResource<TagType> TAG_FOCUS_DISABLED = new TestResource<>(
            COMMON_DIR, "711-tag-focus-disabled.xml", SystemObjectsType.TAG_FOCUS_DISABLED.value());

    TestResource<TagType> TAG_FOCUS_NAME_CHANGED = new TestResource<>(
            COMMON_DIR, "712-tag-focus-name-changed.xml", SystemObjectsType.TAG_FOCUS_NAME_CHANGED.value());

    TestResource<TagType> TAG_FOCUS_ASSIGNMENT_CHANGED = new TestResource<>(
            COMMON_DIR, "713-tag-focus-assignment-changed.xml", SystemObjectsType.TAG_FOCUS_ASSIGNMENT_CHANGED.value());

    TestResource<TagType> TAG_FOCUS_ARCHETYPE_CHANGED = new TestResource<>(
            COMMON_DIR, "714-tag-focus-archetype-changed.xml", SystemObjectsType.TAG_FOCUS_ARCHETYPE_CHANGED.value());

    TestResource<TagType> TAG_FOCUS_PARENT_ORG_REFERENCE_CHANGED = new TestResource<>(
            COMMON_DIR, "715-tag-focus-parent-org-reference-changed.xml",
            SystemObjectsType.TAG_FOCUS_PARENT_ORG_REFERENCE_CHANGED.value());

    TestResource<TagType> TAG_FOCUS_ROLE_MEMBERSHIP_CHANGED = new TestResource<>(
            COMMON_DIR, "716-tag-focus-role-membership-changed.xml",
            SystemObjectsType.TAG_FOCUS_ROLE_MEMBERSHIP_CHANGED.value());

    TestResource<TagType> TAG_PROJECTION_ENABLED = new TestResource<>(
            COMMON_DIR, "730-tag-projection-enabled.xml",
            SystemObjectsType.TAG_PROJECTION_ENABLED.value());

    TestResource<TagType> TAG_PROJECTION_DISABLED = new TestResource<>(
            COMMON_DIR, "731-tag-projection-disabled.xml",
            SystemObjectsType.TAG_PROJECTION_DISABLED.value());
}
