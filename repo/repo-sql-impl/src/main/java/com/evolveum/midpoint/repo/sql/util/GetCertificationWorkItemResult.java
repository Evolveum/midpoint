/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.transform.ResultTransformer;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GetCertificationWorkItemResult implements Serializable {

    public static final ResultStyle RESULT_STYLE = new ResultStyle() {
        @Override
        public ResultTransformer getResultTransformer() {
            return new ResultTransformer() {
                @Override
                public Object transformTuple(Object[] tuple, String[] aliases) {
                    return new GetCertificationWorkItemResult((String) tuple[0], (Integer) tuple[1], (Integer) tuple[2]);
                }
            };
        }

        @Override
        public List<String> getIdentifiers(String rootAlias) {
            return Arrays.asList(rootAlias + ".ownerOwnerOid", rootAlias + ".ownerId", rootAlias + ".id");
        }

        @Override
        public String getCountString(String basePath) {
            return "*";
        }

        @Override
        public List<String> getContentAttributes(String basePath) {
            return Collections.emptyList();
        }
    };

    @NotNull private final String campaignOid;
    @NotNull private final Integer caseId;
    @NotNull private final Integer id;

    public GetCertificationWorkItemResult(@NotNull String campaignOid, @NotNull Integer caseId, @NotNull Integer id) {
        this.campaignOid = campaignOid;
        this.caseId = caseId;
        this.id = id;
    }

    @NotNull
    public String getCampaignOid() {
        return campaignOid;
    }

    @NotNull
    public Integer getCaseId() {
        return caseId;
    }

    @NotNull
    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "GetCertificationWorkItemResult{" +
                "campaignOid='" + campaignOid + '\'' +
                ", caseId=" + caseId +
                ", id=" + id +
                '}';
    }
}
