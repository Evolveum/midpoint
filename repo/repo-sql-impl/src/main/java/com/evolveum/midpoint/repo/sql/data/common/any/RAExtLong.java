/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import java.util.Objects;
import jakarta.persistence.*;

import org.hibernate.annotations.ForeignKey;

import com.evolveum.midpoint.repo.sql.data.common.id.RAExtLongId;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(RAExtLongId.class)
@Table(name = "m_assignment_ext_long", indexes = {
        @Index(name = "iAExtensionLong", columnList = "longValue")
})
public class RAExtLong extends RAExtBase<Long> implements RAExtValue<Long> {

    private Long value;

    public RAExtLong() {
    }

    public RAExtLong(Long value) {
        this.value = value;
    }

    @ForeignKey(name = "fk_a_ext_long_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_owner_oid", referencedColumnName = "owner_owner_oid"),
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_id", referencedColumnName = "owner_id")
    })
    @NotQueryable
    public RAssignmentExtension getAnyContainer() {
        return super.getAnyContainer();
    }

    @Id
    @Column(name = "anyContainer_owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        return super.getOwnerOid();
    }

    @Id
    @Column(name = "anyContainer_owner_id")
    @NotQueryable
    public Integer getOwnerId() {
        return super.getOwnerId();
    }

    @Id
    @Column(name = "item_id")
    public Integer getItemId() {
        return super.getItemId();
    }

    @Id
    @Column(name = "longValue")
    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }
        RAExtLong raExtLong = (RAExtLong) o;
        return Objects.equals(value, raExtLong.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public RAExtLongId createId() {
        return RAExtLongId.createFromValue(this);
    }
}
