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

package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 *  TODO - get rid of XMLGregorianCalendar - Date conversions
 *
 * @author lazyman
 */
public class AuditSearchDto implements Serializable {

    public static final String F_FROM_GREG = "fromGreg";
    public static final String F_TO_GREG = "toGreg";
    public static final String F_FROM = "from";
    public static final String F_TO = "to";
    public static final String F_INITIATOR_NAME = "initiatorName";
    public static final String F_CHANNEL = "channel";
    public static final String F_HOST_IDENTIFIER = "hostIdentifier";
    public static final String F_TARGET_NAME = "targetName";
    public static final String F_TARGET_OWNER_NAME = "targetOwnerName";    
    public static final String F_EVENT_TYPE = "eventType";
    public static final String F_EVENT_STAGE = "eventStage";
    public static final String F_OUTCOME = "outcome";

    private XMLGregorianCalendar fromGreg;
    private XMLGregorianCalendar toGreg;
    private Date from;
    private Date to;
    private String initiatorName;
    private String channel;
    private String hostIdentifier;
    private String targetName;
    private String targetOwnerName;
    private String eventType;
    private String eventStage;
    private String outcome;
    
    public XMLGregorianCalendar getFromGreg() {
        return MiscUtil.asXMLGregorianCalendar(from);
    }

    public void setFromGreg(XMLGregorianCalendar fromGreg) {
        this.from = MiscUtil.asDate(fromGreg);
        this.fromGreg = fromGreg;
    }

    public XMLGregorianCalendar getToGreg() {
        return MiscUtil.asXMLGregorianCalendar(to);
    }

    public void setToGreg(XMLGregorianCalendar toGreg) {
        this.to = MiscUtil.asDate(toGreg);
        this.toGreg = toGreg;
    }
    
    public Date getFrom() {
        if (from == null) {
            from = new Date();
        }
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        if (to == null) {
            to = new Date();
        }
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public Timestamp getDateFrom() {
        return new Timestamp(getFrom().getTime());
    }

    public Timestamp getDateTo() {
        return new Timestamp(getTo().getTime());
    }

	public String getInitiatorName() {
		return initiatorName;
	}

	public void setInitiatorName(String initiatorName) {
		this.initiatorName = initiatorName;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getHostIdentifier() {
		return hostIdentifier;
	}

	public void setHostIdentifier(String hostIdentifier) {
		this.hostIdentifier = hostIdentifier;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public String getTargetOwnerName() {
		return targetOwnerName;
	}

	public void setTargetOwnerName(String targetOwnerName) {
		this.targetOwnerName = targetOwnerName;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getEventStage() {
		return eventStage;
	}

	public void setEventStage(String eventStage) {
		this.eventStage = eventStage;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}
	
}
