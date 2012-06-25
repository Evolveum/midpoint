/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.repo.sql.data.common.ROperationResultStatusType;
import com.evolveum.midpoint.repo.sql.data.common.RUtil;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "m_audit_event")
public class RAuditEventRecord implements Serializable {

    private long id;
    private Long timestamp;
    private String eventIdentifier;
    private String sessionIdentifier;
    private String taskIdentifier;
    private String taskOID;
    private String hostIdentifier;
    //prism object - user
    private String initiator;
    //prism object
    private String target;
    //prism object - user
    private String targetOwner;
    private RAuditEventType eventType;
    private RAuditEventStage eventStage;
    //collection of object deltas
    private Set<String> deltas;
    private String channel;
    private ROperationResultStatusType outcome;

    public String getChannel() {
        return channel;
    }

    @ElementCollection
    @ForeignKey(name = "fk_audit_delta")
    @CollectionTable(name = "m_audit_delta")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @Type(type = "org.hibernate.type.TextType")
    public Set<String> getDeltas() {
        return deltas;
    }

    public String getEventIdentifier() {
        return eventIdentifier;
    }

    @Enumerated(EnumType.ORDINAL)
    public RAuditEventStage getEventStage() {
        return eventStage;
    }

    @Enumerated(EnumType.ORDINAL)
    public RAuditEventType getEventType() {
        return eventType;
    }

    public String getHostIdentifier() {
        return hostIdentifier;
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getInitiator() {
        return initiator;
    }

    @Enumerated(EnumType.ORDINAL)
    public ROperationResultStatusType getOutcome() {
        return outcome;
    }

    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getTarget() {
        return target;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getTargetOwner() {
        return targetOwner;
    }

    public String getTaskIdentifier() {
        return taskIdentifier;
    }

    public String getTaskOID() {
        return taskOID;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setDeltas(Set<String> deltas) {
        this.deltas = deltas;
    }

    public void setEventIdentifier(String eventIdentifier) {
        this.eventIdentifier = eventIdentifier;
    }

    public void setEventStage(RAuditEventStage eventStage) {
        this.eventStage = eventStage;
    }

    public void setEventType(RAuditEventType eventType) {
        this.eventType = eventType;
    }

    public void setHostIdentifier(String hostIdentifier) {
        this.hostIdentifier = hostIdentifier;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public void setOutcome(ROperationResultStatusType outcome) {
        this.outcome = outcome;
    }

    public void setSessionIdentifier(String sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setTargetOwner(String targetOwner) {
        this.targetOwner = targetOwner;
    }

    public void setTaskIdentifier(String taskIdentifier) {
        this.taskIdentifier = taskIdentifier;
    }

    public void setTaskOID(String taskOID) {
        this.taskOID = taskOID;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAuditEventRecord that = (RAuditEventRecord) o;

        if (channel != null ? !channel.equals(that.channel) : that.channel != null) return false;
        if (deltas != null ? !deltas.equals(that.deltas) : that.deltas != null) return false;
        if (eventIdentifier != null ? !eventIdentifier.equals(that.eventIdentifier) : that.eventIdentifier != null)
            return false;
        if (eventStage != that.eventStage) return false;
        if (eventType != that.eventType) return false;
        if (hostIdentifier != null ? !hostIdentifier.equals(that.hostIdentifier) : that.hostIdentifier != null)
            return false;
        if (initiator != null ? !initiator.equals(that.initiator) : that.initiator != null) return false;
        if (outcome != that.outcome) return false;
        if (sessionIdentifier != null ? !sessionIdentifier.equals(that.sessionIdentifier) : that.sessionIdentifier != null)
            return false;
        if (target != null ? !target.equals(that.target) : that.target != null) return false;
        if (targetOwner != null ? !targetOwner.equals(that.targetOwner) : that.targetOwner != null) return false;
        if (taskIdentifier != null ? !taskIdentifier.equals(that.taskIdentifier) : that.taskIdentifier != null)
            return false;
        if (taskOID != null ? !taskOID.equals(that.taskOID) : that.taskOID != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = timestamp != null ? timestamp.hashCode() : 0;
        result = 31 * result + (eventIdentifier != null ? eventIdentifier.hashCode() : 0);
        result = 31 * result + (sessionIdentifier != null ? sessionIdentifier.hashCode() : 0);
        result = 31 * result + (taskIdentifier != null ? taskIdentifier.hashCode() : 0);
        result = 31 * result + (taskOID != null ? taskOID.hashCode() : 0);
        result = 31 * result + (hostIdentifier != null ? hostIdentifier.hashCode() : 0);
        result = 31 * result + (initiator != null ? initiator.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (targetOwner != null ? targetOwner.hashCode() : 0);
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (eventStage != null ? eventStage.hashCode() : 0);
        result = 31 * result + (deltas != null ? deltas.hashCode() : 0);
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (outcome != null ? outcome.hashCode() : 0);
        return result;
    }

    public static RAuditEventRecord toRepo(AuditEventRecord record, PrismContext prismContext)
            throws DtoTranslationException {

        Validate.notNull(record, "Audit event record must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");

        RAuditEventRecord repo = new RAuditEventRecord();
        repo.setChannel(record.getChannel());
        repo.setTimestamp(record.getTimestamp());
        repo.setEventStage(RAuditEventStage.toRepo(record.getEventStage()));
        repo.setEventType(RAuditEventType.toRepo(record.getEventType()));
        repo.setSessionIdentifier(record.getSessionIdentifier());
        repo.setEventIdentifier(record.getEventIdentifier());
        repo.setHostIdentifier(record.getHostIdentifier());
        if (record.getOutcome() != null) {
            repo.setOutcome(ROperationResultStatusType.toRepoType(record.getOutcome().createStatusType()));
        }
        repo.setTaskIdentifier(record.getTaskIdentifier());
        repo.setTaskOID(record.getTaskOID());

        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
        try {
            String xml;
            if (record.getTarget() != null) {
                xml = domProcessor.serializeObjectToString(record.getTarget());
                repo.setTarget(xml);
            }
            if (record.getTargetOwner() != null) {
                xml = domProcessor.serializeObjectToString(record.getTargetOwner());
                repo.setTargetOwner(xml);
            }
            if (record.getInitiator() != null) {
                xml = domProcessor.serializeObjectToString(record.getInitiator());
                repo.setInitiator(xml);
            }

            if (!record.getDeltas().isEmpty()) {
                repo.setDeltas(new HashSet<String>());
            } else {
                repo.getDeltas().clear();
            }
            for (ObjectDelta<?> delta : record.getDeltas()) {
                ObjectDeltaType xmlDelta = DeltaConvertor.toObjectDeltaType(delta);
                xml = RUtil.toRepo(xmlDelta, prismContext);
                repo.getDeltas().add(xml);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return repo;
    }
}
