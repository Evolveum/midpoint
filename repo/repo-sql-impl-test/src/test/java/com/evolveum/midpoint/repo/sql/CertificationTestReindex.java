/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import org.springframework.test.context.ContextConfiguration;

/**
 * The same as CertificationTest but with "executeIfNoChanges" (a.k.a. "reindex") option set.
 * Certification cases have very special treatment in this respect.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
public class CertificationTestReindex extends CertificationTest {

    protected RepoModifyOptions getModifyOptions() {
        return RepoModifyOptions.createExecuteIfNoChanges();
    }
}
