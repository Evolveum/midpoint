/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.Objects;

/**
 *
 * @author mederly
 */
@Entity
@ForeignKey(name = "fk_dashboard")
@Table(uniqueConstraints = @UniqueConstraint(name = "u_dashboard_name", columnNames = {"name_norm"}),
        indexes = {
                @Index(name = "iDashboardNameOrig", columnList = "name_orig"),
        }
)
@Persister(impl = MidPointJoinedPersister.class)
public class RDashboard extends RObject<DashboardType> {

    private static final long serialVersionUID = 1L;

    private RPolyString nameCopy;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RDashboard))
            return false;
        if (!super.equals(o))
            return false;
        RDashboard that = (RDashboard) o;
        return Objects.equals(nameCopy, that.nameCopy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nameCopy);
    }

    // dynamically called
    public static void copyFromJAXB(DashboardType jaxb, RDashboard repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);
        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
    }
}
