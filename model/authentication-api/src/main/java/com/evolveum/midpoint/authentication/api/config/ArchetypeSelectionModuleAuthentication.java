/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeSelectionType;

public interface ArchetypeSelectionModuleAuthentication extends IdentificationModuleAuthentication{

    boolean isAllowUndefined();

    ArchetypeSelectionType getArchetypeSelection();
}
