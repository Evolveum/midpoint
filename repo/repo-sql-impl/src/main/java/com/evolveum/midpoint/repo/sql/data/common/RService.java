/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * @author Viliam Repan (lazyman)
 */
@Entity
@ForeignKey(name = "fk_service")
//@Table(uniqueConstraints = @UniqueConstraint(name = "uc_service_name", columnNames = {"name_norm"}))
@Persister(impl = MidPointJoinedPersister.class)
public class RService extends RAbstractRole<ServiceType> {

    private RPolyString nameCopy;
    private Set<String> serviceType;
    private RPolyString locality;
    private Integer displayOrder;

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Embedded
    public RPolyString getLocality() {
        return locality;
    }

    public void setLocality(RPolyString locality) {
        this.locality = locality;
    }

    @ElementCollection
    @ForeignKey(name = "fk_service_type")
    @CollectionTable(name = "m_service_type", joinColumns = {
            @JoinColumn(name = "service_oid", referencedColumnName = "oid")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getServiceType() {
        return serviceType;
    }

    public void setServiceType(Set<String> serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RService rService = (RService) o;

        if (nameCopy != null ? !nameCopy.equals(rService.nameCopy) : rService.nameCopy != null) return false;
        if (serviceType != null ? !serviceType.equals(rService.serviceType) : rService.serviceType != null)
            return false;
        if (locality != null ? !locality.equals(rService.locality) : rService.locality != null) return false;
        return displayOrder != null ? displayOrder.equals(rService.displayOrder) : rService.displayOrder == null;

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{nameCopy, serviceType, locality, displayOrder});
    }

    public static void copyFromJAXB(ServiceType jaxb, RService repo, RepositoryContext repositoryContext,
                                    IdGeneratorResult generatorResult) throws DtoTranslationException {
        RAbstractRole.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setDisplayOrder(jaxb.getDisplayOrder());
        repo.setLocality(RPolyString.copyFromJAXB(jaxb.getLocality()));
        repo.setServiceType(RUtil.listToSet(jaxb.getServiceType()));
        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
    }

    @Override
    public ServiceType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        ServiceType object = new ServiceType();
        RService.copyToJAXB(this, object, prismContext, options);
        RUtil.revive(object, prismContext);
        return object;
    }
}
