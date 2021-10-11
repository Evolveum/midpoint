/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author mederly
 */
@Entity
@ForeignKey(name = "fk_case")
@Table(indexes = {
        @Index(name = "iCaseNameOrig", columnList = "name_orig"),
        @Index(name = "iCaseTypeObjectRefTargetOid", columnList = "objectRef_targetOid"),
        @Index(name = "iCaseTypeTargetRefTargetOid", columnList = "targetRef_targetOid"),
        @Index(name = "iCaseTypeParentRefTargetOid", columnList = "parentRef_targetOid"),
        @Index(name = "iCaseTypeRequestorRefTargetOid", columnList = "requestorRef_targetOid"),
        @Index(name = "iCaseTypeCloseTimestamp", columnList = "closeTimestamp")
}
)
@Persister(impl = MidPointJoinedPersister.class)
public class RCase extends RObject<CaseType> {

    private RPolyString nameCopy;

    private String state;
    private REmbeddedReference objectRef;
    private REmbeddedReference targetRef;
    private REmbeddedReference parentRef;
    private REmbeddedReference requestorRef;

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
    public REmbeddedReference getObjectRef() {
        return objectRef;
    }

    public void setObjectRef(REmbeddedReference objectRef) {
        this.objectRef = objectRef;
    }

    @Embedded
    public REmbeddedReference getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(REmbeddedReference targetRef) {
        this.targetRef = targetRef;
    }

    @Embedded
    public REmbeddedReference getParentRef() {
        return parentRef;
    }

    public void setParentRef(REmbeddedReference value) {
        this.parentRef = value;
    }

    public REmbeddedReference getRequestorRef() {
        return requestorRef;
    }

    public void setRequestorRef(REmbeddedReference requestorRef) {
        this.requestorRef = requestorRef;
    }

    public XMLGregorianCalendar getCloseTimestamp() {
        return closeTimestamp;
    }

    public void setCloseTimestamp(XMLGregorianCalendar closeTimestamp) {
        this.closeTimestamp = closeTimestamp;
    }

    @JaxbName(localPart = "workItem")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @org.hibernate.annotations.ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RCaseWorkItem> getWorkItems() {
        return workItems;
    }

    public void setWorkItems(Set<RCaseWorkItem> workItems) {
        this.workItems = workItems != null ? workItems : new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RCase))
            return false;
        if (!super.equals(o))
            return false;
        RCase rCase = (RCase) o;
        return Objects.equals(nameCopy, rCase.nameCopy) &&
                Objects.equals(objectRef, rCase.objectRef) &&
                Objects.equals(targetRef, rCase.targetRef) &&
                Objects.equals(parentRef, rCase.parentRef) &&
                Objects.equals(requestorRef, rCase.requestorRef) &&
                Objects.equals(closeTimestamp, rCase.closeTimestamp) &&
                Objects.equals(workItems, rCase.workItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nameCopy, objectRef, targetRef, parentRef, requestorRef,
                closeTimestamp, workItems);
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
