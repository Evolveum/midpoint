/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql;

/**
 *
 */
public class ExtensionTestNoFetchDeletion extends ExtensionTest {

    @Override
    boolean isNoFetchDeletion() {
        return true;
    }
}
