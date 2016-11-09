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

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

/**
 * DTO representing a particular decision.
 *
 * TODO cleanup a bit
 *
 * @author mederly
 */
public class CertDecisionDto extends CertCaseOrDecisionDto {

    public static final String F_COMMENT = "comment";
    public static final String F_RESPONSE = "response";

    private AccessCertificationDecisionType decision;

    public CertDecisionDto(AccessCertificationCaseType _case, PageBase page) {
        super(_case, page);
        if (_case.getDecision().isEmpty()) {
            decision = new AccessCertificationDecisionType(page.getPrismContext());
        } else if (_case.getDecision().size() == 1) {
            decision = _case.getDecision().get(0);
        } else {
            throw new IllegalStateException("More than one relevant decision entry in a certification case: " + _case);
        }
    }

    public String getComment() {
        return decision.getComment();
    }

    public void setComment(String value) {
        decision.setComment(value);
    }

    public AccessCertificationResponseType getResponse() {
        return decision.getResponse();
    }

}
