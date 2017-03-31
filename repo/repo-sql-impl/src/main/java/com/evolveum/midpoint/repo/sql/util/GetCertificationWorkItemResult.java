/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author mederly
 */
public class GetCertificationWorkItemResult implements Serializable {

    public static final ResultTransformer RESULT_TRANSFORMER = new BasicTransformerAdapter() {
        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            return new GetCertificationWorkItemResult((String) tuple[0], (Integer) tuple[1], (Integer) tuple[2]);
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
}
