/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.rest;

import java.io.File;
import jakarta.ws.rs.core.MediaType;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;

public class TestRestServiceXml extends TestAbstractRestService {

    @Override
    protected String getAcceptHeader() {
        return MediaType.APPLICATION_XML;
    }

    @Override
    protected String getContentType() {
        return MediaType.APPLICATION_XML;
    }


    @Override
    protected File getRepoFile(String fileBaseName) {
        return new File(BASE_REPO_DIR + "/xml", fileBaseName + ".xml");
    }

    @Override
    protected File getRequestFile(String fileBaseName) {
        return new File(BASE_REQ_DIR + "/xml", fileBaseName + ".xml");
    }

    @Override
    protected MidpointAbstractProvider getProvider() {
        return xmlProvider;
    }

}
