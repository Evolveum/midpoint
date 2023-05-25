/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import jakarta.persistence.*;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_report_name", columnNames = { "name_norm" }),
        indexes = {
                @jakarta.persistence.Index(name = "iReportNameOrig", columnList = "name_orig"),
        }
)
@ForeignKey(name = "fk_report")
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public class RReport extends RObject {

    private RPolyString nameCopy;
    private Boolean parent;
    private Boolean useHibernateSession;

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

    @Index(name = "iReportParent")
    public Boolean getParent() {
        return parent;
    }

    public Boolean getUseHibernateSession() {
        return useHibernateSession;
    }

    public void setParent(Boolean parent) {
        this.parent = parent;
    }

    public void setUseHibernateSession(Boolean useHibernateSession) {
        this.useHibernateSession = useHibernateSession;
    }

    // dynamically called
    public static void copyFromJAXB(ReportType jaxb, RReport repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);
        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
    }
}
