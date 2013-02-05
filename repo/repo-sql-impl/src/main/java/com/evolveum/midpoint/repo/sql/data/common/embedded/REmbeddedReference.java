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

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RContainerType;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Type;
import org.w3c.dom.Element;

import javax.persistence.*;
import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
@Embeddable
public class REmbeddedReference implements ObjectReference {

    //target
    private String targetOid;
    //other fields
    private String description = "";
    private String filter = "";
    private RContainerType type;
    //relation qname
    private String relationNamespace = "";
    private String relationLocalPart = "";

    @Column(length = RUtil.COLUMN_LENGTH_LOCALPART)
    @Override
    public String getRelationLocalPart() {
        return relationLocalPart;
    }

    @Override
    public String getRelationNamespace() {
        return relationNamespace;
    }

    @Column(length = 36, insertable = true, updatable = true, nullable = true)
    @Override
    public String getTargetOid() {
        return targetOid;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    @Override
    public String getDescription() {
        return description;
    }

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

    public void setRelationLocalPart(String relationLocalPart) {
        this.relationLocalPart = relationLocalPart;
    }

    public void setRelationNamespace(String relationNamespace) {
        this.relationNamespace = relationNamespace;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFilter(String filter) {
        this.filter = filter;
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

        REmbeddedReference that = (REmbeddedReference) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
        if (targetOid != null ? !targetOid.equals(that.targetOid) : that.targetOid != null)
            return false;
        if (type != that.type) return false;
        if (relationLocalPart != null ? !relationLocalPart.equals(that.relationLocalPart) : that.relationLocalPart != null)
            return false;
        if (relationNamespace != null ? !relationNamespace.equals(that.relationNamespace) : that.relationNamespace != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = description != null ? description.hashCode() : 0;
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (relationLocalPart != null ? relationLocalPart.hashCode() : 0);
        result = 31 * result + (relationNamespace != null ? relationNamespace.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyToJAXB(REmbeddedReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        if (StringUtils.isNotEmpty(repo.getDescription())) {
            jaxb.setDescription(repo.getDescription());
        }
        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
        if (StringUtils.isNotEmpty(repo.getRelationLocalPart()) && StringUtils.isNotEmpty(repo.getRelationNamespace())) {
            jaxb.setRelation(new QName(repo.getRelationNamespace(), repo.getRelationLocalPart()));
        }
        if (StringUtils.isNotEmpty(repo.getTargetOid())) {
            jaxb.setOid(repo.getTargetOid());
        }

        String filter = repo.getFilter();
        if (StringUtils.isNotEmpty(filter)) {
            Element element = DOMUtil.parseDocument(filter).getDocumentElement();
            ObjectReferenceType.Filter jaxbFilter = new ObjectReferenceType.Filter();
            jaxbFilter.setFilter(element);
            jaxb.setFilter(jaxbFilter);
        }
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, REmbeddedReference repo, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notEmpty(jaxb.getOid(), "Target oid must not be null.");

        if (jaxb.getDescription() != null) {
            repo.setDescription(jaxb.getDescription());
        }
        repo.setType(ClassMapper.getHQLTypeForQName(jaxb.getType()));
        if (jaxb.getRelation() != null) {
            repo.setRelationNamespace(jaxb.getRelation().getNamespaceURI());
            repo.setRelationLocalPart(jaxb.getRelation().getLocalPart());
        }

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
