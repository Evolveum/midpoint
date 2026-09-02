package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeSelectionType;

public interface ArchetypeSelectionModuleAuthentication extends IdentificationModuleAuthentication{

    boolean isAllowUndefined();

    ArchetypeSelectionType getArchetypeSelection();
}
