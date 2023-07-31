/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.api.config;

public interface CorrelationModuleAuthentication extends ModuleAuthentication {

    String getCurrentCorrelatorIdentifier();

}
