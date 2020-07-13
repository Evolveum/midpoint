/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.container;

import java.util.Objects;
import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.annotations.GenericGenerator;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.query.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerIdGetter;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

@JaxbType(type = TriggerType.class)
@Entity
@IdClass(RContainerId.class)
@Table(indexes = { @Index(name = "iTriggerTimestamp", columnList = RTrigger.C_TIMESTAMP) })
public class RTrigger implements Container<RObject> {

    public static final String F_OWNER = "owner";
    public static final String C_TIMESTAMP = "timestampValue";

    private Boolean trans;

    //identificator
    private RObject owner;
    private String ownerOid;
    private Integer id;
    //trigger fields
    private String handlerUri;
    private XMLGregorianCalendar timestamp;

    public RTrigger() {
        this(null);
    }

    public RTrigger(RObject owner) {
        setOwner(owner);
    }

    @Id
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_trigger_owner"))
    @NotQueryable
    public RObject getOwner() {
        return owner;
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
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
    @IdQueryProperty
    public Integer getId() {
        return id;
    }

    public String getHandlerUri() {
        return handlerUri;
    }

    @Column(name = C_TIMESTAMP)
    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTimestamp(XMLGregorianCalendar timestamp) {
        this.timestamp = timestamp;
    }

    public void setHandlerUri(String handlerUri) {
        this.handlerUri = handlerUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RTrigger)) {
            return false;
        }

        RTrigger that = (RTrigger) o;
        return Objects.equals(ownerOid, that.ownerOid)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOid, id);
    }

    public static void copyToJAXB(RTrigger repo, TriggerType jaxb) {
        Objects.requireNonNull(repo, "Repo object must not be null.");
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");

        jaxb.setId(RUtil.toLong(repo.getId()));

        jaxb.setHandlerUri(repo.getHandlerUri());
        jaxb.setTimestamp(repo.getTimestamp());
    }

    public static void fromJaxb(TriggerType jaxb, RTrigger repo, RObject parent,
            RepositoryContext repositoryContext) throws DtoTranslationException {
        repo.setOwner(parent);
        fromJaxb(jaxb, repo, repositoryContext, null);
    }

    public static void fromJaxb(TriggerType jaxb, RTrigger repo, ObjectType parent,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult) {
        repo.setOwnerOid(parent.getOid());
        fromJaxb(jaxb, repo, repositoryContext, generatorResult);
    }

    private static void fromJaxb(TriggerType jaxb, RTrigger repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult) {

        Objects.requireNonNull(repo, "Repo object must not be null.");
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");

        if (generatorResult != null) {
            repo.setTransient(generatorResult.isTransient(jaxb.asPrismContainerValue()));
        }

        repo.setId(RUtil.toInteger(jaxb.getId()));

        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setTimestamp(jaxb.getTimestamp());
    }

    public TriggerType toJAXB() {
        TriggerType object = new TriggerType();
        RTrigger.copyToJAXB(this, object);
        return object;
    }
}
