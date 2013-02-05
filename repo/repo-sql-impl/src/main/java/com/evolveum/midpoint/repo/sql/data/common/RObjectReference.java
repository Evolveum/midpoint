/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.id.RObjectReferenceId;
import com.evolveum.midpoint.repo.sql.type.QNameType;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.w3c.dom.Element;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@IdClass(RObjectReferenceId.class)
@Table(name = "m_reference")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = RObjectReference.REFERENCE_TYPE, discriminatorType = DiscriminatorType.INTEGER)
public class RObjectReference implements ObjectReference {

    public static final String REFERENCE_TYPE = "reference_type";

    public static final String F_OWNER = "owner";

    //owner
    private RContainer owner;
    private String ownerOid;
    private Long ownerId;
    //other primary key fields
    private String targetOid;
    private String relationNamespace;
    private String relationLocalPart;

    //other fields
    private String description;
    private String filter;
    private RContainerType type;

    public RObjectReference() {
    }

    @ForeignKey(name = "fk_reference_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "owner_oid", referencedColumnName = "oid"),
            @PrimaryKeyJoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public RContainer getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_oid", length = 36)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "owner_id")
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Id
    @Column(name = "targetOid", length = 36)
    @Override
    public String getTargetOid() {
        return targetOid;
    }

    @Id
    @Column(name = "relNamespace")
    @Override
    public String getRelationNamespace() {
        if (relationNamespace == null) {
            relationNamespace = QNameType.EMPTY_QNAME_COLUMN_VALUE;
        }
        return relationNamespace;
    }

    @Id
    @Column(name = "relLocalPart", length = RUtil.COLUMN_LENGTH_LOCALPART)
    @Override
    public String getRelationLocalPart() {
        if (relationLocalPart == null) {
            relationLocalPart = QNameType.EMPTY_QNAME_COLUMN_VALUE;
        }
        return relationLocalPart;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    @Override
    public String getDescription() {
        return description;
    }


    /**
     * Represents {@link javax.xml.namespace.QName} type attribute in reference e.g.
     * {@link com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType} represented
     * as enum {@link RContainerType#USER}
     *
     * @return null if not defined, otherwise value from {@link RContainerType} enum
     */
    @Column(name = "containerType")
    @Enumerated(EnumType.ORDINAL)
    @Override
    public RContainerType getType() {
        return type;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    @Override
    public String getFilter() {
        return filter;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setOwner(RContainer owner) {
        this.owner = owner;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setRelationLocalPart(String relationLocalPart) {
        this.relationLocalPart = relationLocalPart;
    }

    public void setRelationNamespace(String relationNamespace) {
        this.relationNamespace = relationNamespace;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public void setType(RContainerType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RObjectReference ref = (RObjectReference) o;

        if (description != null ? !description.equals(ref.description) : ref.description != null)
            return false;
        if (filter != null ? !filter.equals(ref.filter) : ref.filter != null) return false;
        if (getRelationNamespace() != null ? !getRelationNamespace().equals(ref.getRelationNamespace()) :
                ref.getRelationNamespace() != null) return false;
        if (getRelationLocalPart() != null ? !getRelationLocalPart().equals(ref.getRelationLocalPart()) :
                ref.getRelationLocalPart() != null) return false;
        if (targetOid != null ? !targetOid.equals(ref.targetOid) : ref.targetOid != null) return false;
        if (type != ref.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = targetOid != null ? targetOid.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (getRelationNamespace() != null ? getRelationNamespace().hashCode() : 0);
        result = 31 * result + (getRelationLocalPart() != null ? getRelationLocalPart().hashCode() : 0);

        return result;
    }

    public static void copyToJAXB(RObjectReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        if (StringUtils.isNotEmpty(repo.getDescription())) {
            jaxb.setDescription(repo.getDescription());
        }
        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
        jaxb.setOid(repo.getTargetOid());
        jaxb.setRelation(QNameType.assembleQName(repo.getRelationNamespace(), repo.getRelationLocalPart()));

        String filter = repo.getFilter();
        if (StringUtils.isNotEmpty(filter)) {
            Element element = DOMUtil.parseDocument(filter).getDocumentElement();
            ObjectReferenceType.Filter jaxbFilter = new ObjectReferenceType.Filter();
            jaxbFilter.setFilter(element);
            jaxb.setFilter(jaxbFilter);
        }
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, RObjectReference repo, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notEmpty(jaxb.getOid(), "Target oid must not be null.");

        if (jaxb.getDescription() != null) {
            repo.setDescription(jaxb.getDescription());
        }
        repo.setType(ClassMapper.getHQLTypeForQName(jaxb.getType()));

        String[] relation = QNameType.disassembleQName(jaxb.getRelation());
        repo.setRelationNamespace(relation[0]);
        repo.setRelationLocalPart(relation[1]);

        repo.setTargetOid(jaxb.getOid());

        if (jaxb.getFilter() != null && jaxb.getFilter().getFilter() != null) {
            ObjectReferenceType.Filter filter = jaxb.getFilter();
            repo.setFilter(DOMUtil.printDom(filter.getFilter()).toString());
        }
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);

        return ref;
    }
}
