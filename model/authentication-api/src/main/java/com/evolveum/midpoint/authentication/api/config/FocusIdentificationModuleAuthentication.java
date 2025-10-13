/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;

import java.util.List;

public interface FocusIdentificationModuleAuthentication extends ModuleAuthentication {

    List<ModuleItemConfigurationType> getModuleConfiguration();
}
