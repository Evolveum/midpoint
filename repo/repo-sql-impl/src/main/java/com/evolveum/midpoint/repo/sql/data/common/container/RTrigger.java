package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerIdGetter;
import com.evolveum.midpoint.repo.sql.query2.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

@JaxbType(type = TriggerType.class)
@Entity
@IdClass(RContainerId.class)
@Table(indexes = {@Index(name = "iTriggerTimestamp", columnList = RTrigger.C_TIMESTAMP)})
public class RTrigger implements Container {

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

    @Override
    @Id
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_trigger_owner"))
    @NotQueryable
    public RObject getOwner() {
        return owner;
    }

    @Override
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Override
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

    @Override
    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    @Override
    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RTrigger that = (RTrigger) o;

        if (handlerUri != null ? !handlerUri.equals(that.handlerUri) :
                that.handlerUri != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) :
                that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = handlerUri != null ? handlerUri.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RTrigger repo, TriggerType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setId(RUtil.toLong(repo.getId()));

        jaxb.setHandlerUri(repo.getHandlerUri());
        jaxb.setTimestamp(repo.getTimestamp());

    }

    public static void copyFromJAXB(TriggerType jaxb, RTrigger repo, RObject parent,
                                    RepositoryContext repositoryContext) throws DtoTranslationException {

        repo.setOwner(parent);
        copyFromJAXB(jaxb, repo, repositoryContext, null);
    }

    public static void copyFromJAXB(TriggerType jaxb, RTrigger repo, ObjectType parent,
                                    RepositoryContext repositoryContext, IdGeneratorResult generatorResult)
            throws DtoTranslationException {

        repo.setOwnerOid(parent.getOid());
        copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);
    }

    private static void copyFromJAXB(TriggerType jaxb, RTrigger repo, RepositoryContext repositoryContext,
                                     IdGeneratorResult generatorResult) throws DtoTranslationException {

        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        if (generatorResult != null) {
            repo.setTransient(generatorResult.isTransient(jaxb.asPrismContainerValue()));
        }

        repo.setId(RUtil.toInteger(jaxb.getId()));

        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setTimestamp(jaxb.getTimestamp());
    }

    public TriggerType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        TriggerType object = new TriggerType();
        RTrigger.copyToJAXB(this, object, prismContext);
        return object;
    }
}
