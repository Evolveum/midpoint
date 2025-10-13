/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.prism.path.ItemPath;

import java.util.List;

public interface AttributeVerificationModuleAuthentication extends ModuleAuthentication {

    List<ItemPath> getPathsToVerify();
}
