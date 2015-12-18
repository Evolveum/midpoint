/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class VirtualCollectionSpecification extends CollectionSpecification {

    private VirtualQueryParam[] additionalParams;

    public VirtualCollectionSpecification(VirtualQueryParam[] additionalParams) {
        this.additionalParams = additionalParams;
    }

    public VirtualQueryParam[] getAdditionalParams() {
        return additionalParams;
    }

    void setAdditionalParams(VirtualQueryParam[] additionalParams) {
        this.additionalParams = additionalParams;
    }

    @Override
    public String toString() {
        return "VirtualCol{additionalParams=" + getAdditionalParamNames() + "}";
    }

    @Override
    public String getShortInfo() {
        return "[params=" + getAdditionalParamNames() + "]";
    }

    private List<String> getAdditionalParamNames() {
        List<String> rv = new ArrayList<>();
        for (VirtualQueryParam p : additionalParams) {
            rv.add(p.name());
        }
        return rv;
    }
}
