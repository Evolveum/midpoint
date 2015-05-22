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

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.prism.parser.XNodeSerializer;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang3.Validate;
import org.apache.poi.ss.formula.functions.T;

import java.io.Serializable;

/**
 * @author mederly
 */
public class CertDecisionDto extends Selectable implements Serializable{

    public static final String F_SUBJECT_NAME = "subjectName";
    public static final String F_TARGET_NAME = "targetName";
    public static final String F_TARGET_TYPE = "targetType";
    public static final String F_COMMENT = "comment";
    public static final String F_RESPONSE = "response";

    private AccessCertificationCaseType certCase;
    private String subjectName;
    private String targetName;
    private AccessCertificationDecisionType decision;
public CertDecisionDto() {
	// TODO Auto-generated constructor stub
}
    public CertDecisionDto(AccessCertificationCaseType _case) {
        Validate.notNull(_case);

        this.certCase = _case;
        this.subjectName = getName(_case.getSubjectRef());
        this.targetName = getName(_case.getTargetRef());
        if (_case.getDecision().isEmpty()) {
            decision = new AccessCertificationDecisionType();
        } else if (_case.getDecision().size() == 1) {
            decision = _case.getDecision().get(0);
        } else {
            throw new IllegalStateException("More than one relevant decision entry in a certification case: " + _case);
        }
    }

    // ugly hack (for now) - we extract the name from serialization metadata
    private String getName(ObjectReferenceType ref) {
        if (ref == null) {
            return null;
        }
        String name = (String) ref.asReferenceValue().getUserData(XNodeSerializer.USER_DATA_KEY_COMMENT);
        if (name == null) {
            return "(" + ref.getOid() + ")";
        } else {
            return name.trim();
        }
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetType() {
        // TODO improve
        return certCase.getTargetRef().getType().getLocalPart();
    }

    public String getComment() {
        return decision.getComment();
    }

    public void setComment(String value) {
        decision.setComment(value);
    }

    // TODO reimplement the following 2 methods to use buttons

    public String getResponse() {
        return String.valueOf(decision.getResponse());
    }

    public void setResponse(String value) {
        try {
            decision.setResponse(AccessCertificationResponseType.fromValue(value));
        } catch (IllegalArgumentException e) {
            // TODO
        }
    }

    public ObjectReferenceType getCampaignRef() {
        return certCase.getCampaignRef();
    }

    public Long getCaseId() {
        return certCase.asPrismContainerValue().getId();
    }

	
    
    

}
