/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import com.evolveum.midpoint.model.impl.correlator.idmatch.data.ListResponse;

import java.io.IOException;
import java.util.List;

public interface ApacheApiRequest {

    void doRequest(String urlPrefix, String urlSuffix, String jsonString) throws IOException;

    List<ListResponse> listResponse();
}
