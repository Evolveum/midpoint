/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.other;

import java.util.Date;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.RLookupTable;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerIdGetter;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;

@Ignore
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uc_row_key", columnNames = { "owner_oid", "row_key" })
})
@IdClass(RContainerId.class)
public class RLookupTableRow implements Container<RLookupTable> {

    //todo move to super class Container (change container to abstract class)
    private Boolean trans;

    private RLookupTable owner;
    private String ownerOid;
    private Integer id;

    private String key;
    private String value;
    private RPolyString label;

    private XMLGregorianCalendar lastChangeTimestamp;

    @JoinColumn(name = "owner_oid", referencedColumnName = "oid", foreignKey = @ForeignKey(name = "fk_lookup_table_owner"))
    @MapsId("ownerOid")
    @ManyToOne(fetch = FetchType.LAZY)
    @Override
    @NotQueryable
    public RLookupTable getOwner() {
        return owner;
    }

    /**
     * @see com.evolveum.midpoint.repo.sql.data.generator.ContainerOidGenerator
     */
    @Id
    @Override
    @GeneratedValue(generator = "ContainerOidGenerator")
    @GenericGenerator(name = "ContainerOidGenerator", strategy = "com.evolveum.midpoint.repo.sql.data.generator.ContainerOidGeneratorImpl")
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    public static final String ID_COLUMN_NAME = "id";

    @Override
    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = ID_COLUMN_NAME)
    @IdQueryProperty
    public Integer getId() {
        return id;
    }

    @Column(name = "row_key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public RPolyString getLabel() {
        return label;
    }

    public void setLabel(RPolyString label) {
        this.label = label;
    }

    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getLastChangeTimestamp() {
        return lastChangeTimestamp;
    }

    public void setLastChangeTimestamp(XMLGregorianCalendar lastChangeTimestamp) {
        this.lastChangeTimestamp = lastChangeTimestamp;
    }

    @Column(name = "row_value")
    public String getValue() {
        return value;
    }

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void setOwner(RLookupTable owner) {
        this.owner = owner;
        if (owner != null) {
            setOwnerOid(owner.getOid());
        }
    }

    @Override
    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public LookupTableRowType toJAXB(PrismContext prismContext) {
        LookupTableRowType row = new LookupTableRowType();
        row.setId(Long.valueOf(id));
        row.setKey(key);
        row.setLastChangeTimestamp(lastChangeTimestamp);
        row.setValue(value);
        row.setLabel(RPolyString.copyToJAXB(label, prismContext));

        return row;
    }

    public static RLookupTableRow toRepo(RLookupTable owner, LookupTableRowType row) throws SchemaException {
        RLookupTableRow rRow = toRepo(row);
        rRow.setOwner(owner);
        return rRow;
    }

    public static RLookupTableRow toRepo(String ownerOid, LookupTableRowType row) throws SchemaException {
        RLookupTableRow rRow = toRepo(row);
        rRow.setOwnerOid(ownerOid);
        return rRow;
    }

    private static RLookupTableRow toRepo(LookupTableRowType row) throws SchemaException {
        RLookupTableRow rRow = new RLookupTableRow();
        rRow.setId(RUtil.toInteger(row.getId()));
        if (row.getKey() == null) {
            throw new SchemaException("Attempt to insert a row with no key");
        }
        rRow.setKey(row.getKey());
        rRow.setLabel(RPolyString.copyFromJAXB(row.getLabel()));
        rRow.setLastChangeTimestamp(row.getLastChangeTimestamp());
        if (rRow.getLastChangeTimestamp() == null) {
            XMLGregorianCalendar cal = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());
            rRow.setLastChangeTimestamp(cal);
            row.setLastChangeTimestamp(cal);
        }
        rRow.setValue(row.getValue());

        return rRow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RLookupTableRow)) {
            return false;
        }

        RLookupTableRow that = (RLookupTableRow) o;
        return Objects.equals(getOwnerOid(), that.getOwnerOid())
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerOid(), id);
    }

    @Override
    public String toString() {
        return "RLookupTableRow{" +
                "id=" + id +
                ", owner=" + owner +
                ", ownerOid='" + getOwnerOid() + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", label=" + label +
                '}';
    }
}
