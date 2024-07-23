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
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RNodeOperationalState;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

@Entity
@ForeignKey(name = "fk_node")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_node_name", columnNames = { "name_norm" }),
        indexes = {
                @Index(name = "iNodeNameOrig", columnList = "name_orig"),
        }
)
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public class RNode extends RObject {

    private RPolyString nameCopy;
    private String nodeIdentifier;
    private RNodeOperationalState operationalState;

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    @JaxbName(localPart = "name")
    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm")) })
    @Embedded
    @NeverNull
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RNodeOperationalState getOperationalState() {
        return operationalState;
    }

    public void setOperationalState(RNodeOperationalState operationalState) {
        this.operationalState = operationalState;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    public void setNodeIdentifier(String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    // dynamically called
    public static void copyFromJAXB(NodeType jaxb, RNode repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyObjectInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setNodeIdentifier(jaxb.getNodeIdentifier());
        repo.setOperationalState(RUtil.getRepoEnumValue(jaxb.getOperationalState(), RNodeOperationalState.class));
    }
}
