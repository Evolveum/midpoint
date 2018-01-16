/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.Collection;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_generic_object")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_generic_object_name", columnNames = {"name_norm"}))
@Persister(impl = MidPointJoinedPersister.class)
public class RGenericObject extends RObject<GenericObjectType> {

    private RPolyString nameCopy;
    private String objectType;

    public String getObjectType() {
        return objectType;
    }

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

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RGenericObject that = (RGenericObject) o;

        if (nameCopy != null ? !nameCopy.equals(that.nameCopy) : that.nameCopy != null) return false;
        if (objectType != null ? !objectType.equals(that.objectType) : that.objectType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nameCopy != null ? nameCopy.hashCode() : 0);
        result = 31 * result + (objectType != null ? objectType.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(GenericObjectType jaxb, RGenericObject repo, RepositoryContext repositoryContext,
                                    IdGeneratorResult generatorResult) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setObjectType(jaxb.getObjectType());
    }

    @Override
    public GenericObjectType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {

        GenericObjectType object = new GenericObjectType();
        RUtil.revive(object, prismContext);
        RGenericObject.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}
