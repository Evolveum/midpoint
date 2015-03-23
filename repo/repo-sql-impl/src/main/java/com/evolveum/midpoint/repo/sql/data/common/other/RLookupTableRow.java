package com.evolveum.midpoint.repo.sql.data.common.other;

import com.evolveum.midpoint.repo.sql.data.common.RLookupTable;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableTableType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @author Viliam Repan (lazyman)
 */
@Entity
@Table(indexes = {
//todo create indexes after lookup api is created (when we know how we will search through lookup table [lazyman]
//        @Index(name = "i_row_key", columnList = "key"),
//        @Index(name = "i_row_label_orig", columnList = "label.orig"),
//        @Index(name = "i_row_label_norm", columnList = "label.norm")
})
@IdClass(RContainerId.class)
public class RLookupTableRow implements Container<RLookupTable> {

    //todo move to super class Container (change container to abstract class)
    private Boolean trans;

    private RLookupTable owner;
    private String ownerOid;
    private Short id;

    private String key;
    private String value;
    private RPolyString label;
    private XMLGregorianCalendar lastChangeTimestamp;

    @Id
    @ForeignKey(name = "fk_lookup_table_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @Override
    public RLookupTable getOwner() {
        return owner;
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    public Short getId() {
        return id;
    }

    @Id
    @Column(name = "row_key", unique = true)
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
    }

    @Override
    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public void setId(Short id) {
        this.id = id;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RLookupTableRow that = (RLookupTableRow) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        if (lastChangeTimestamp != null ? !lastChangeTimestamp.equals(that.lastChangeTimestamp) : that.lastChangeTimestamp != null)
            return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (lastChangeTimestamp != null ? lastChangeTimestamp.hashCode() : 0);
        return result;
    }

    public LookupTableTableType toJAXB() {
        LookupTableTableType row = new LookupTableTableType();
        row.setId(Long.valueOf(id));
        row.setKey(key);
        row.setLastChangeTimestamp(lastChangeTimestamp);
        row.setValue(value);
        row.setLabel(RPolyString.copyToJAXB(label));

        return row;
    }

    public static RLookupTableRow toRepo(String ownerOid, LookupTableTableType table) {
        RLookupTableRow row = new RLookupTableRow();
        row.setOwnerOid(ownerOid);
        row.setId(RUtil.toShort(table.getId()));
        row.setKey(table.getKey());
        row.setLabel(RPolyString.copyFromJAXB(table.getLabel()));
        row.setLastChangeTimestamp(table.getLastChangeTimestamp());
        if (row.getLastChangeTimestamp() == null) {
            XMLGregorianCalendar cal = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());
            row.setLastChangeTimestamp(cal);
            table.setLastChangeTimestamp(cal);
        }
        row.setValue(table.getValue());

        return row;
    }
}
