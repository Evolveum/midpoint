/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.io.Serializable;
import java.util.Date;

/**
 * @author mederly
 */
public class Decision implements Serializable {

    private static final long serialVersionUID = -542549699933865819L;

    private String approverName;
    private String approverOid;
    private boolean approved;
    private String comment;
    private Date date;
    private Integer stageNumber;
    private String stageName;
    private String stageDisplayName;
    private String additionalDelta;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getApproverName() {
        return approverName;
    }

    public String getApproverOid() {
        return approverOid;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public void setApproverOid(String approverOid) {
        this.approverOid = approverOid;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

	public Integer getStageNumber() {
		return stageNumber;
	}

	public void setStageNumber(Integer stageNumber) {
		this.stageNumber = stageNumber;
	}

	public String getStageName() {
		return stageName;
	}

	public void setStageName(String stageName) {
		this.stageName = stageName;
	}

	public String getStageDisplayName() {
		return stageDisplayName;
	}

	public void setStageDisplayName(String stageDisplayName) {
		this.stageDisplayName = stageDisplayName;
	}

	@Override
    public String toString() {
        return "Decision: approved=" + isApproved() + ", comment=" + getComment() + ", approver=" + getApproverName()
				+ "/" + getApproverOid() + ", date=" + getDate() + ", stage=" + stageNumber + ":" + stageName
				+ ", additionalDelta = " + additionalDelta;
    }

	public String getAdditionalDelta() {
		return additionalDelta;
	}

	public void setAdditionalDelta(String additionalDelta) {
		this.additionalDelta = additionalDelta;
	}

	public DecisionType toDecisionType(PrismContext prismContext) throws SchemaException {
        DecisionType decisionType = new DecisionType();
        decisionType.setApproved(isApproved());
        decisionType.setComment(getComment());
        decisionType.setDateTime(XmlTypeConverter.createXMLGregorianCalendar(getDate()));
        if (approverOid != null) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid(approverOid);
            ort.setType(UserType.COMPLEX_TYPE);
            ort.setTargetName(new PolyStringType(approverName));
            decisionType.setApproverRef(ort);
        }
        decisionType.setStageNumber(stageNumber);
        decisionType.setStageName(stageName);
        decisionType.setStageDisplayName(stageDisplayName);
        if (additionalDelta != null) {
        	decisionType.setAdditionalDelta(prismContext.parserFor(additionalDelta).parseRealValue(ObjectDeltaType.class));
		}
        return decisionType;
    }
}
