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
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.w3c.dom.Element;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Embeddable
public class REmbeddedReference implements Serializable {

    //target
    private String targetOid;
    //other fields
    private String description;
    private String filter;
    private RContainerType type;
//    private QName relation;

//    @Columns(columns = {
//            @Column(name = "relation_namespace"),
//            @Column(name = "relation_localPart")
//    })
//    public QName getRelation() {
//        return relation;
//    }

    @Column(length = 36, insertable = true, updatable = true, nullable = true)
    public String getTargetOid() {
        return targetOid;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getDescription() {
        return description;
    }

    @Enumerated(EnumType.ORDINAL)
    public RContainerType getType() {
        return type;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getFilter() {
        return filter;
    }

//    public void setRelation(QName relation) {
//        this.relation = relation;
//    }

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
//        if (relation != null ? !relation.equals(that.relation) : that.relation != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = description != null ? description.hashCode() : 0;
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
//        result = 31 * result + (relation != null ? relation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyToJAXB(REmbeddedReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setDescription(repo.getDescription());
        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
//        jaxb.setRelation(repo.getRelation());
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

        repo.setDescription(jaxb.getDescription());
        repo.setType(ClassMapper.getHQLTypeForQName(jaxb.getType()));
//        repo.setRelation(jaxb.getRelation());

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
