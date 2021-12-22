/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.authorization;

import com.evolveum.midpoint.util.DebugDumpable;

/**
 * loader for url, initialize all urls with authorizations
 */
public interface DescriptorLoader extends DebugDumpable {

    /**
     * load urls and their authorizations
     */
    void loadData();
}
