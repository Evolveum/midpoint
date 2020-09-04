/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.impl.Channel;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 *  TODO - get rid of XMLGregorianCalendar - Date conversions
 *
 * @author lazyman
 */
public class AuditSearchDto implements Serializable {

    public static final String F_FROM = "from";
    public static final String F_TO = "to";
    public static final String F_INITIATOR_NAME = "initiatorName";
    public static final String F_CHANNEL = "channel";
    public static final String F_HOST_IDENTIFIER = "hostIdentifier";
    public static final String F_REQUEST_IDENTIFIER = "requestIdentifier";
    public static final String F_TARGET_NAME = "targetName";
    public static final String F_TARGET_NAMES_OBJECTS = "targetNamesObjects";
    public static final String F_TARGET_OWNER_NAME = "targetOwnerName";
    public static final String F_EVENT_TYPE = "eventType";
    public static final String F_EVENT_STAGE = "eventStage";
    public static final String F_OUTCOME = "outcome";
    public static final String F_CHANGED_ITEM = "changedItem";
    public static final String F_VALUE_REF_TARGET_NAME = "valueRefTargetNames";
    public static final String F_COLLECTION_REF = "collectionRef";
    public static final String F_RESOURCE_OID = "resourceOid";

    private XMLGregorianCalendar from;
    private XMLGregorianCalendar to;
    private ObjectReferenceType initiatorName;
    private Channel channel;
    private String hostIdentifier;
    private String requestIdentifier;
    private List<ObjectReferenceType> targetNames = new ArrayList<>();
    private List<ObjectType> targetNamesObjects = new ArrayList<>();
    private ObjectReferenceType targetOwnerName;
    private AuditEventTypeType eventType;
    private AuditEventStageType eventStage;
    private OperationResultStatusType outcome;
    private ItemPathDto changedItem;
    private List<ObjectType> valueRefTargetNames;
    private CollectionRefSpecificationType collectionRef;
    private String resourceOid;

    private ItemPathPanel.ItemPathPanelMode changedItemPanelMode = ItemPathPanel.ItemPathPanelMode.NAMESPACE_MODE;

    public AuditSearchDto() {
    }

    public XMLGregorianCalendar getFrom() {
        return from;
    }

    public void setFrom(XMLGregorianCalendar from) {
        this.from = from;
    }

    public XMLGregorianCalendar getTo() {
        return to;
    }

    public void setTo(XMLGregorianCalendar to) {
        this.to = to;
    }

    public ObjectReferenceType getInitiatorName() {
        return initiatorName;
    }

    public void setInitiatorName(ObjectReferenceType initiatorName) {
        this.initiatorName = initiatorName;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getHostIdentifier() {
        return hostIdentifier;
    }

    public void setHostIdentifier(String hostIdentifier) {
        this.hostIdentifier = hostIdentifier;
    }

    public String getRequestIdentifier() {
        return requestIdentifier;
    }

    public void setRequestIdentifier(String requestIdentifier) {
        this.requestIdentifier = requestIdentifier;
    }

    public List<ObjectReferenceType> getTargetNames() {
        return targetNames;
    }

    public void setTargetNames(List<ObjectReferenceType> targetNameList) {
        this.targetNames = targetNameList;
    }

    public ObjectReferenceType getTargetOwnerName() {
        return targetOwnerName;
    }

    public void setTargetOwnerName(ObjectReferenceType targetOwnerName) {
        this.targetOwnerName = targetOwnerName;
    }

    public AuditEventTypeType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventTypeType eventType) {
        this.eventType = eventType;
    }

    public AuditEventStageType getEventStage() {
        return eventStage;
    }

    public void setEventStage(AuditEventStageType eventStage) {
        this.eventStage = eventStage;
    }

    public OperationResultStatusType getOutcome() {
        return outcome;
    }

    public void setOutcome(OperationResultStatusType outcome) {
        this.outcome = outcome;
    }

    public ItemPathDto getChangedItem() {
        if (changedItem == null) {
            changedItem = new ItemPathDto();
//            changedItem.setObjectType(UserType.COMPLEX_TYPE);
        }
        return changedItem;
    }

    public void setChangedItem(ItemPathDto changedItem) {
        this.changedItem = changedItem;
    }

    public List<ObjectType> getvalueRefTargetNames() {
        return valueRefTargetNames;
    }

    public void setvalueRefTargetNames(List<ObjectType> valueRefTargetNames) {
        this.valueRefTargetNames = valueRefTargetNames;
    }

    public List<ObjectType> getTargetNamesObjects() {
        return targetNamesObjects;
    }

    public void setTargetNamesObjects(List<ObjectType> targetNamesObjects) {
        this.targetNamesObjects = targetNamesObjects;
    }

    public CollectionRefSpecificationType getCollectionRef() {
        return collectionRef;
    }

    public void setCollectionRef(CollectionRefSpecificationType collectionRef) {
        this.collectionRef = collectionRef;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public ItemPathPanel.ItemPathPanelMode getChangedItemPanelMode() {
        return changedItemPanelMode;
    }

    public void setChangedItemPanelMode(ItemPathPanel.ItemPathPanelMode changedItemPanelMode) {
        this.changedItemPanelMode = changedItemPanelMode;
    }
}
