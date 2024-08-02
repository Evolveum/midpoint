/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import java.sql.Timestamp;
import java.util.Objects;
import jakarta.persistence.*;

import org.hibernate.annotations.ForeignKey;

import com.evolveum.midpoint.repo.sql.data.common.id.RAExtDateId;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(RAExtDateId.class)
@Table(name = "m_assignment_ext_date", indexes = {
        @Index(name = "iAExtensionDate", columnList = "dateValue")
})
public class RAExtDate extends RAExtBase<Timestamp> implements RAExtValue<Timestamp> {

    private Timestamp value;

    public RAExtDate() {
    }

    public RAExtDate(Timestamp value) {
        this.value = value;
    }

    @ForeignKey(name = "fk_a_ext_date_owner")
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

    @Column(name = "dateValue")
    public Timestamp getValue() {
        return value;
    }

    public void setValue(Timestamp value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }
        RAExtDate raExtDate = (RAExtDate) o;
        return Objects.equals(value, raExtDate.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public RAExtDateId createId() {
        return RAExtDateId.createFromValue(this);
    }
}
