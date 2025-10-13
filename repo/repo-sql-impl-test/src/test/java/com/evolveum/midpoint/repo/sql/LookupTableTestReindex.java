/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.repo.api.RepoModifyOptions;

/**
 * The same as LookupTableTest but with {@link RepoModifyOptions#forceReindex forceReindex} option set.
 * Although this flag causes no special behavior for lookup table rows, it's better to check.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LookupTableTestReindex extends LookupTableTest {

    protected RepoModifyOptions getModifyOptions() {
        return RepoModifyOptions.createForceReindex();
    }
}
