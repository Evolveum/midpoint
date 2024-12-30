/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import jakarta.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

@Entity
@ForeignKey(name = "fk_report_output")
@Persister(impl = MidPointJoinedPersister.class)
@Table(name = RReportData.TABLE_NAME, indexes = {
        @Index(name = "iReportOutputNameOrig", columnList = "name_orig"),
        @Index(name = "iReportOutputNameNorm", columnList = "name_norm") })
@DynamicUpdate
public class RReportData extends RObject {

    public static final String TABLE_NAME = "m_report_output";

    private RPolyString nameCopy;
    private RSimpleEmbeddedReference reportRef;

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

    @Embedded
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public RSimpleEmbeddedReference getReportRef() {
        return reportRef;
    }

    public void setReportRef(RSimpleEmbeddedReference reportRef) {
        this.reportRef = reportRef;
    }

    // dynamically called
    public static void copyFromJAXB(ReportDataType jaxb, RReportData repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setReportRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getReportRef(), repositoryContext.relationRegistry));
    }
}
