/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;

import java.util.List;

public interface FocusIdentificationModuleAuthentication extends ModuleAuthentication {

    List<ModuleItemConfigurationType> getModuleConfiguration();
}
