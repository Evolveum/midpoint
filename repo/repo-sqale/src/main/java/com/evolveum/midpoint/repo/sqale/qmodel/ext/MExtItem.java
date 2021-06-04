/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ext;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;

/**
 * Querydsl "row bean" type related to {@link QUri}.
 */
public class MExtItem {

    public Integer id;
    public Integer itemNameId;
    public Integer valueTypeId; // references use ObjectReferenceType#COMPLEX_TYPE
    public MExtItemHolderType holderType;
    public MExtItemCardinality cardinality;

    @Override
    public String toString() {
        return "MExtItem{" +
                "id=" + id +
                ", itemNameId=" + itemNameId +
                ", valueTypeId=" + valueTypeId +
                ", holderType=" + holderType +
                ", cardinality=" + cardinality +
                '}';
    }
}
