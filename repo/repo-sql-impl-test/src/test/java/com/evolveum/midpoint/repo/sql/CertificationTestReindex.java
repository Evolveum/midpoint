/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.repo.api.RepoModifyOptions;

/**
 * The same as CertificationTest but with {@link RepoModifyOptions#forceReindex forceReindex} option set.
 * Certification cases have very special treatment in this respect.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CertificationTestReindex extends CertificationTest {

    protected RepoModifyOptions getModifyOptions() {
        return RepoModifyOptions.createForceReindex();
    }
}
