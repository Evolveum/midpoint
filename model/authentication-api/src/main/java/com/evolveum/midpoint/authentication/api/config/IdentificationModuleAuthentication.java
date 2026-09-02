/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.config;

/**
 * Modules that establish the identity used for resolving the security policy of the authentication flow.
 *
 * Examples are the {@link FocusIdentificationModuleAuthentication} and the {@link ArchetypeSelectionModuleAuthentication} modules.
 */

public interface IdentificationModuleAuthentication extends ModuleAuthentication {}
