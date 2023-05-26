/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.*;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

@Entity
@ForeignKey(name = "fk_object_template")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_object_template_name", columnNames = { "name_norm" }),
        indexes = {
                @Index(name = "iObjectTemplateNameOrig", columnList = "name_orig"),
        }
)
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public class RObjectTemplate extends RObject {

    private RPolyString nameCopy;
    private Set<RObjectReference<RObjectTemplate>> includeRef;

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 7")
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RObjectReference<RObjectTemplate>> getIncludeRef() {
        if (includeRef == null) {
            includeRef = new HashSet<>();
        }
        return includeRef;
    }

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

    public void setIncludeRef(Set<RObjectReference<RObjectTemplate>> includeRef) {
        this.includeRef = includeRef;
    }

    // dynamically called
    public static void copyFromJAXB(ObjectTemplateType jaxb, RObjectTemplate repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));

        repo.getIncludeRef().addAll(RUtil.toRObjectReferenceSet(
                jaxb.getIncludeRef(), repo, RReferenceType.INCLUDE, repositoryContext.relationRegistry));
    }
}
