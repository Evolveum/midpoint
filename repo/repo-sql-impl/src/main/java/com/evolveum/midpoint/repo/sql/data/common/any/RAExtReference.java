/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.repo.sql.data.common.id.RAExtReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(RAExtReferenceId.class)
@Table(name = "m_assignment_ext_reference")
@org.hibernate.annotations.Table(appliesTo = "m_assignment_ext_reference",
        indexes = {@Index(name = "iAExtensionReference", columnNames = {"extensionType", "eName", "targetoid"})})
public class RAExtReference implements RAExtValue {

    //owner entity
    private RAssignmentExtension anyContainer;
    private String ownerOid;
    private Integer ownerId;

    private RAssignmentExtensionType extensionType;

    private boolean dynamic;
    private String name;
    private String type;
    private RValueType valueType;

    //this is target oid
    private String value;
    //this is type attribute
    private RObjectType targetType;
    private String relation;

    public RAExtReference() {
    }

    @ForeignKey(name = "fk_assignment_ext_reference")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_owner_oid", referencedColumnName = "ownerOid"),
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_id", referencedColumnName = "owner_type")
    })
    @NotQueryable
    public RAssignmentExtension getAnyContainer() {
        return anyContainer;
    }

    @Id
    @Column(name = "anyContainer_owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        if (ownerOid == null && anyContainer != null) {
            ownerOid = anyContainer.getOwnerOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "anyContainer_owner_id")
    @NotQueryable
    public Integer getOwnerId() {
        if (ownerId == null && anyContainer != null) {
            ownerId = anyContainer.getOwnerId();
        }
        return ownerId;
    }

    @Id
    @Enumerated(EnumType.ORDINAL)
    public RAssignmentExtensionType getExtensionType() {
//        return extensionType;
        return RAssignmentExtensionType.EXTENSION;
    }

    @Id
    @Column(name = "eName", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getName() {
        return name;
    }

    @Column(name = "eType", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getType() {
        return type;
    }

    @Enumerated(EnumType.ORDINAL)
    public RValueType getValueType() {
        return valueType;
    }

    /**
     * @return true if this property has dynamic definition
     */
    @Column(name = "dynamicDef")
    public boolean isDynamic() {
        return dynamic;
    }

    @Column(name = "targetoid", length = RUtil.COLUMN_LENGTH_OID)
    public String getValue() {
        return value;
    }

    @Enumerated(EnumType.ORDINAL)
    public RObjectType getTargetType() {
        return targetType;
    }

    @Column(name = "relation", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getRelation() {
        return relation;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValueType(RValueType valueType) {
        this.valueType = valueType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public void setAnyContainer(RAssignmentExtension anyContainer) {
        this.anyContainer = anyContainer;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setTargetType(RObjectType targetType) {
        this.targetType = targetType;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public void setExtensionType(RAssignmentExtensionType extensionType) {
        this.extensionType = extensionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAExtReference that = (RAExtReference) o;

        if (dynamic != that.dynamic) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (relation != null ? !relation.equals(that.relation) : that.relation != null) return false;
        if (targetType != that.targetType) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (valueType != that.valueType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (dynamic ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (valueType != null ? valueType.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (targetType != null ? targetType.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);

        return result;
    }

    public static PrismReferenceValue createReference(RAExtReference repo) {
        PrismReferenceValue value = new PrismReferenceValue();
        value.setOid(repo.getValue());
        value.setRelation(RUtil.stringToQName(repo.getRelation()));
        value.setTargetType(ClassMapper.getQNameForHQLType(repo.getTargetType()));

        return value;
    }

    public static RAExtReference createReference(PrismReferenceValue jaxb) {
        RAExtReference repo = new RAExtReference();

        repo.setValue(jaxb.getOid());
        repo.setRelation(RUtil.qnameToString(jaxb.getRelation()));
        repo.setTargetType(ClassMapper.getHQLTypeForQName(jaxb.getTargetType()));

        return repo;
    }
}
