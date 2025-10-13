/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
