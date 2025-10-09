/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

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
