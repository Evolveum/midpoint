/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.HashSet;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.Type;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

@Entity
@ForeignKey(name = "fk_case")
@Table(indexes = {
        @Index(name = "iCaseNameOrig", columnList = "name_orig"),
        @Index(name = "iCaseTypeObjectRefTargetOid", columnList = "objectRef_targetOid"),
        @Index(name = "iCaseTypeTargetRefTargetOid", columnList = "targetRef_targetOid"),
        @Index(name = "iCaseTypeParentRefTargetOid", columnList = "parentRef_targetOid"),
        @Index(name = "iCaseTypeRequestorRefTargetOid", columnList = "requestorRef_targetOid"),
        @Index(name = "iCaseTypeCloseTimestamp", columnList = "closeTimestamp")
})
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public class RCase extends RObject {

    private RPolyString nameCopy;

    private String state;
    private RSimpleEmbeddedReference objectRef;
    private RSimpleEmbeddedReference targetRef;
    private RSimpleEmbeddedReference parentRef;
    private RSimpleEmbeddedReference requestorRef;

    private XMLGregorianCalendar closeTimestamp;

    private Set<RCaseWorkItem> workItems = new HashSet<>();

    @JaxbName(localPart = "name")
    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    @NeverNull
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    @Column
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Embedded
    public RSimpleEmbeddedReference getObjectRef() {
        return objectRef;
    }

    public void setObjectRef(RSimpleEmbeddedReference objectRef) {
        this.objectRef = objectRef;
    }

    @Embedded
    public RSimpleEmbeddedReference getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(RSimpleEmbeddedReference targetRef) {
        this.targetRef = targetRef;
    }

    @Embedded
    public RSimpleEmbeddedReference getParentRef() {
        return parentRef;
    }

    public void setParentRef(RSimpleEmbeddedReference value) {
        this.parentRef = value;
    }

    public RSimpleEmbeddedReference getRequestorRef() {
        return requestorRef;
    }

    public void setRequestorRef(RSimpleEmbeddedReference requestorRef) {
        this.requestorRef = requestorRef;
    }

    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getCloseTimestamp() {
        return closeTimestamp;
    }

    public void setCloseTimestamp(XMLGregorianCalendar closeTimestamp) {
        this.closeTimestamp = closeTimestamp;
    }

    @JaxbName(localPart = "workItem")
    @OneToMany(mappedBy = "owner", orphanRemoval = true, cascade = CascadeType.ALL)
    @org.hibernate.annotations.ForeignKey(name = "none")
    public Set<RCaseWorkItem> getWorkItems() {
        return workItems;
    }

    public void setWorkItems(Set<RCaseWorkItem> workItems) {
        this.workItems = workItems != null ? workItems : new HashSet<>();
    }

    @Override
    public String toString() {
        return "RCase{" +
                "name=" + nameCopy +
                ", parentRef=" + parentRef +
                ", objectRef=" + objectRef +
                ", targetRef=" + targetRef +
                '}';
    }

    // dynamically called
    public static void copyFromJAXB(CaseType jaxb, RCase repo, RepositoryContext context,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, context, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));

        repo.setParentRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getParentRef(), context.relationRegistry));
        repo.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getObjectRef(), context.relationRegistry));
        repo.setTargetRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTargetRef(), context.relationRegistry));
        repo.setRequestorRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getRequestorRef(), context.relationRegistry));
        repo.setCloseTimestamp(jaxb.getCloseTimestamp());
        repo.setState(jaxb.getState());
        for (CaseWorkItemType workItem : jaxb.getWorkItem()) {
            repo.getWorkItems().add(RCaseWorkItem.toRepo(repo, workItem, context));
        }
    }
}
