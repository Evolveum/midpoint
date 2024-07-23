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
import com.evolveum.midpoint.repo.sql.data.common.id.ROExtStringId;
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
@IdClass(ROExtStringId.class)
@Table(name = "m_object_ext_string", indexes = {
        @Index(name = "iExtensionString", columnList = "stringValue")
})
public class ROExtString extends ROExtBase<String> {

    private String value;

    public ROExtString() {
    }

    public ROExtString(String value) {
        this.value = value;
    }

    @MapsId("ownerOid")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    @JoinColumn(name = "owner_oid", foreignKey = @ForeignKey(name = "fk_object_ext_string_owner"))
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

    @Column(name = "stringValue")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof ROExtString)) {return false;}
        if (!super.equals(o)) {return false;}
        ROExtString that = (ROExtString) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public ROExtStringId createId() {
        return ROExtStringId.createFromValue(this);
    }

    @Override
    public String toString() {
        return "ROExtString{" +
                "value='" + value + '\'' +
                '}';
    }
}
