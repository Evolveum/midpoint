/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import java.io.Serializable;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import com.evolveum.midpoint.repo.sql.data.common.id.ROrgClosureId;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;

@SqlResultSetMapping(name = "OrgClosureBasic",
        columns = {
                @ColumnResult(name = "descendant_oid", type = String.class),
                @ColumnResult(name = "ancestor_oid", type = String.class),
                @ColumnResult(name = "val", type = Integer.class),
        }
)
@SqlResultSetMapping(name = "OrgClosureQuickCheck",
        columns = {
                @ColumnResult(name = "problems", type = Integer.class)
        }
)
@SqlResultSetMapping(name = "OrgClosureCheckCycles",
        columns = {
                @ColumnResult(name = "descendant_oid", type = String.class),
                @ColumnResult(name = "ancestor_oid", type = String.class),
        }
)
@Ignore
@IdClass(ROrgClosureId.class)
@Entity
@Table(name = "m_org_closure",
        indexes = {
                @jakarta.persistence.Index(name = "iAncestor", columnList = "ancestor_oid"),
                @jakarta.persistence.Index(name = "iDescendant", columnList = "descendant_oid"),
                @jakarta.persistence.Index(name = "iDescendantAncestor", columnList = "descendant_oid, ancestor_oid")
        })
@NotQueryable
@DynamicUpdate
public class ROrgClosure implements Serializable {

    private RObject ancestor;
    private String ancestorOid;

    private RObject descendant;
    private String descendantOid;

    private int val;

    public ROrgClosure() {
    }

    public ROrgClosure(String ancestorOid, String descendantOid, int val) {
        if (ancestorOid != null) {
            this.ancestorOid = ancestorOid;
        }
        if (descendantOid != null) {
            this.descendantOid = descendantOid;
        }
        this.val = val;
    }

    public ROrgClosure(RObject ancestor, RObject descendant, int val) {
        this.ancestor = ancestor;
        this.descendant = descendant;
        this.val = val;
    }

    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ancestor_oid", referencedColumnName = "oid", foreignKey = @ForeignKey(name = "fk_ancestor"))
    @NotQueryable
    public RObject getAncestor() {
        return ancestor;
    }

    @Id
    @Column(name = "ancestor_oid", length = RUtil.COLUMN_LENGTH_OID, insertable = false, updatable = false)
    @NotQueryable
    public String getAncestorOid() {
        if (ancestorOid == null && ancestor.getOid() != null) {
            ancestorOid = ancestor.getOid();
        }
        return ancestorOid;
    }

    public void setAncestor(RObject ancestor) {
        this.ancestor = ancestor;
    }

    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descendant_oid", referencedColumnName = "oid", foreignKey = @ForeignKey(name = "fk_descendant"))
    @NotQueryable
    public RObject getDescendant() {
        return descendant;
    }

    @Id
    @Column(name = "descendant_oid", length = RUtil.COLUMN_LENGTH_OID, insertable = false, updatable = false)
    @NotQueryable
    public String getDescendantOid() {
        if (descendantOid == null && descendant.getOid() != null) {
            descendantOid = descendant.getOid();
        }
        return descendantOid;
    }

    public void setDescendant(RObject descendant) {
        this.descendant = descendant;
    }

    public void setAncestorOid(String ancestorOid) {
        this.ancestorOid = ancestorOid;
    }

    public void setDescendantOid(String descendantOid) {
        this.descendantOid = descendantOid;
    }

    @Column(name = "val")
    public int getVal() {
        return val;
    }

    public void setVal(int val) {
        this.val = val;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {return true;}
        if (obj == null || getClass() != obj.getClass()) {return false;}

        ROrgClosure that = (ROrgClosure) obj;

        if (ancestor != null ? !ancestor.equals(that.ancestor) : that.ancestor != null) {return false;}
        if (descendant != null ? !descendant.equals(that.descendant) : that.descendant != null) {return false;}

        return true;
    }

    @Override
    public String toString() {
        return "ROrgClosure{a='" + ancestorOid + "', d='" + descendantOid + "', val=" + val + "}";
    }
}
