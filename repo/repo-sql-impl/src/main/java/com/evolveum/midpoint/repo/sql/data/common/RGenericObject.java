/*
 * Copyright (c) 2010-2021 Evolveum and contributors
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_generic_object")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_generic_object_name", columnNames = {"name_norm"}),
        indexes = {
                @Index(name = "iGenericObjectNameOrig", columnList = "name_orig"),
        }
)
@Persister(impl = MidPointJoinedPersister.class)
public class RGenericObject extends RFocus {

    private RPolyString nameCopy;
    private String objectType;

    public String getObjectType() {
        return objectType;
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

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    // dynamically called
    public static void copyFromJAXB(GenericObjectType jaxb, RGenericObject repo, RepositoryContext repositoryContext,
                                    IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyFocusInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setObjectType(jaxb.getObjectType());
    }
}
