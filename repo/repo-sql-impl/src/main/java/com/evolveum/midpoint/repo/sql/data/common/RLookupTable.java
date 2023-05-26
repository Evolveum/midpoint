/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.*;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RLookupTableRow;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

@Entity
@ForeignKey(name = "fk_lookup_table")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_lookup_name", columnNames = { "name_norm" }),
        indexes = {
                @Index(name = "iLookupTableNameOrig", columnList = "name_orig"),
        }
)
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public class RLookupTable extends RObject {

    private RPolyString nameCopy;
    private Set<RLookupTableRow> rows;

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

    @Transient
    public Set<RLookupTableRow> getRows() {
        return rows;
    }

    public void setRows(Set<RLookupTableRow> rows) {
        this.rows = rows;
    }

    // dynamically called
    public static void copyFromJAXB(
            LookupTableType jaxb, RLookupTable repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException, SchemaException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));

        List<LookupTableRowType> rows = jaxb.getRow();
        if (!rows.isEmpty()) {
            repo.setRows(new HashSet<>());
            for (LookupTableRowType row : rows) {
                RLookupTableRow rRow = RLookupTableRow.toRepo(repo, row);
                rRow.setTransient(generatorResult.isTransient(row.asPrismContainerValue()));
                repo.getRows().add(rRow);
            }
        }
    }
}
