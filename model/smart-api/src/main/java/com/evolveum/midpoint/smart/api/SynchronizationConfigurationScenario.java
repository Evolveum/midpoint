/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.api;

/**
 * Direction classification for a resource or object type, used to drive
 * predefined synchronization configurations.
 */
public enum SynchronizationConfigurationScenario {
    SOURCE,
    SOURCE_WITH_FEEDBACK,
    TARGET,
    TARGET_WITH_FEEDBACK
}
