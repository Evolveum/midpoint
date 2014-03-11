package com.evolveum.midpoint.repo.sql.data.poc;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class RAO implements Serializable {

    private String oid;
    private RPolyString name;

    @Id
    @GeneratedValue(generator = "ContainerOidGenerator")
    @GenericGenerator(name = "ContainerOidGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerOidGenerator")
    @Column(name = "oid", nullable = false, updatable = false, length = RUtil.COLUMN_LENGTH_OID)
    public String getOid() {
        return oid;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAO ro = (RAO) o;

        if (name != null ? !name.equals(ro.name) : ro.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
