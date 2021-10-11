/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * The same as LookupTableTest but with "executeIfNoChanges" (a.k.a. "reindex") option set.
 * Although this flag causes no special behavior for lookup table rows, it's better to check.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LookupTableTestReindex extends LookupTableTest {

    protected RepoModifyOptions getModifyOptions() {
        return RepoModifyOptions.createExecuteIfNoChanges();
    }

}
