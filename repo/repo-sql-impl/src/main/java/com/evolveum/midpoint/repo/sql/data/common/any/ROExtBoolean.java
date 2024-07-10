/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import java.util.Objects;
import jakarta.persistence.*;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.ROExtBooleanId;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;

import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(ROExtBooleanId.class)
@Table(name = "m_object_ext_boolean", indexes = {
        @Index(name = "iExtensionBoolean", columnList = "booleanValue")
})
public class ROExtBoolean extends ROExtBase<Boolean> {

    private Boolean value;

    public ROExtBoolean() {
    }

    public ROExtBoolean(Boolean value) {
        this.value = value;
    }

    @MapsId("ownerOid")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    @JoinColumn(name = "owner_oid", foreignKey = @ForeignKey(name = "fk_o_ext_boolean_owner"))
    public RObject getOwner() {
        return super.getOwner();
    }

    @Id
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        return super.getOwnerOid();
    }

    @Id
    @JdbcType(IntegerJdbcType.class)
    @Column(name = "ownerType")
    @Enumerated(EnumType.ORDINAL)
    public RObjectExtensionType getOwnerType() {
        return super.getOwnerType();
    }

    @Id
    @Column(name = "item_id")
    public Integer getItemId() {
        return super.getItemId();
    }

    @Id
    @Column(name = "booleanValue")
    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }
        ROExtBoolean that = (ROExtBoolean) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public ROExtBooleanId createId() {
        return ROExtBooleanId.createFromValue(this);
    }
}
