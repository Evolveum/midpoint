/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.rest;

import java.io.File;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;

public class TestRestServiceYaml extends TestAbstractRestService {

    @Override
    protected String getAcceptHeader() {
        return "application/yaml";
    }

    @Override
    protected String getContentType() {
        return "application/yaml";
    }


    @Override
    protected File getRepoFile(String fileBaseName) {
        return new File(BASE_REPO_DIR + "/yaml", fileBaseName + ".yml");
    }

    @Override
    protected File getRequestFile(String fileBaseName) {
        return new File(BASE_REQ_DIR + "/yaml", fileBaseName + ".yml");
    }

    @Override
    protected MidpointAbstractProvider getProvider() {
        return yamlProvider;
    }

}
