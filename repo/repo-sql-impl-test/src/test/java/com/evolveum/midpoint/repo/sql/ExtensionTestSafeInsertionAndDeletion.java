/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql;

/**
 *
 */
public class ExtensionTestSafeInsertionAndDeletion extends ExtensionTest {

    @Override
    boolean isNoFetchInsertion() {
        return false;
    }

    @Override
    boolean isNoFetchDeletion() {
        return false;
    }

    @Override
    int getExtraSafeInsertionSelects(int insertions) {
        return insertions;
    }
}
