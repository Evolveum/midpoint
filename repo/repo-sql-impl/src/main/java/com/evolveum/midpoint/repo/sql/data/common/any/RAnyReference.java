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
import com.evolveum.midpoint.repo.sql.data.common.RAnyContainer;
import com.evolveum.midpoint.repo.sql.data.common.other.RContainerType;
import com.evolveum.midpoint.repo.sql.data.common.id.RAnyReferenceId;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.DOMUtil;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
@Entity
@IdClass(RAnyReferenceId.class)
@Table(name = "m_any_reference")
public class RAnyReference implements RAnyValue {

    //owner entity
    private RAnyContainer anyContainer;
    private String ownerOid;
    private Long ownerId;
    private RContainerType ownerType;

    private boolean dynamic;
    private QName name;
    private QName type;
    private RValueType valueType;

    //this is target oid
    private String value;
    private String description;
    private String filter;
    //this is type attribute
    private RContainerType targetType;
    private QName relation;

    public RAnyReference() {
    }

    @ForeignKey(name = "fk_any_reference")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_oid", referencedColumnName = "ownerOid"),
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_id", referencedColumnName = "ownerId"),
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_type", referencedColumnName = "owner_type")
    })
    public RAnyContainer getAnyContainer() {
        return anyContainer;
    }

    @Id
    @Column(name = "anyContainer_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        if (ownerOid == null && anyContainer != null) {
            ownerOid = anyContainer.getOwnerOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "anyContainer_owner_id")
    public Long getOwnerId() {
        if (ownerId == null && anyContainer != null) {
            ownerId = anyContainer.getOwnerId();
        }
        return ownerId;
    }

    @Id
    @Column(name = "anyContainer_owner_type")
    public RContainerType getOwnerType() {
        if (ownerType == null && anyContainer != null) {
            ownerType = anyContainer.getOwnerType();
        }
        return ownerType;
    }

    @Id
    @Columns(columns = {
            @Column(name = "name_namespace"),
            @Column(name = "name_localPart", length = RUtil.COLUMN_LENGTH_LOCALPART)
    })
    public QName getName() {
        return name;
    }

    @Id
    @Columns(columns = {
            @Column(name = "type_namespace"),
            @Column(name = "type_localPart", length = RUtil.COLUMN_LENGTH_LOCALPART)
    })
    public QName getType() {
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

    @Index(name = "iTargetOid")
    @Column(name = "targetoid", length = RUtil.COLUMN_LENGTH_OID)
    public String getValue() {
        return value;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getDescription() {
        return description;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getFilter() {
        return filter;
    }

    @Enumerated(EnumType.ORDINAL)
    public RContainerType getTargetType() {
        return targetType;
    }

    @Columns(columns = {
            @Column(name = "relation_namespace"),
            @Column(name = "relation_localPart", length = RUtil.COLUMN_LENGTH_LOCALPART)
    })
    public QName getRelation() {
        return relation;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValueType(RValueType valueType) {
        this.valueType = valueType;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public void setType(QName type) {
        this.type = type;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public void setAnyContainer(RAnyContainer anyContainer) {
        this.anyContainer = anyContainer;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerType(RContainerType ownerType) {
        this.ownerType = ownerType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setTargetType(RContainerType targetType) {
        this.targetType = targetType;
    }

    public void setRelation(QName relation) {
        this.relation = relation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAnyReference that = (RAnyReference) o;

        if (dynamic != that.dynamic) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
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
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (targetType != null ? targetType.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);

        return result;
    }

    public static PrismReferenceValue createReference(RAnyReference repo) {
        PrismReferenceValue value = new PrismReferenceValue();
        value.setOid(repo.getValue());
        value.setDescription(repo.getDescription());
        value.setFilter(StringUtils.isNotEmpty(repo.getFilter()) ?
                DOMUtil.parseDocument(repo.getFilter()).getDocumentElement() : null);
        value.setRelation(repo.getRelation());
        value.setTargetType(ClassMapper.getQNameForHQLType(repo.getTargetType()));

        return value;
    }

    public static RAnyReference createReference(PrismReferenceValue jaxb) {
        RAnyReference repo = new RAnyReference();

        repo.setDescription(jaxb.getDescription());
        repo.setFilter(jaxb.getFilter() != null ? DOMUtil.printDom(jaxb.getFilter()).toString() : null);
        repo.setValue(jaxb.getOid());
        repo.setRelation(jaxb.getRelation());
        repo.setTargetType(ClassMapper.getHQLTypeForQName(jaxb.getTargetType()));

        return repo;
    }
}
