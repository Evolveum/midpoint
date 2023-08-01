package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeSelectionType;

public interface ArchetypeSelectionModuleAuthentication extends ModuleAuthentication{

    boolean isAllowUndefined();

    ArchetypeSelectionType getArchetypeSelection();
}
