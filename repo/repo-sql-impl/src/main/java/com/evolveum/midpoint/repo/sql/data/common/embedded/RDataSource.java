/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.DataSourceType;
import org.apache.commons.lang.Validate;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Embeddable
@JaxbType(type = DataSourceType.class)
public class RDataSource implements Serializable {

    private String providerClass;
    private boolean springBean;

    public String getProviderClass() {
        return providerClass;
    }

    public void setProviderClass(String providerClass) {
        this.providerClass = providerClass;
    }

    public boolean isSpringBean() {
        return springBean;
    }

    public void setSpringBean(boolean springBean) {
        this.springBean = springBean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RDataSource that = (RDataSource) o;

        if (springBean != that.springBean) return false;
        if (providerClass != null ? !providerClass.equals(that.providerClass) : that.providerClass != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = providerClass != null ? providerClass.hashCode() : 0;
        result = 31 * result + (springBean ? 1 : 0);
        return result;
    }

    public static void copyFromJAXB(DataSourceType jaxb, RDataSource repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setProviderClass(jaxb.getProviderClass());
        repo.setSpringBean(jaxb.isSpringBean());
    }

    public static void copyToJAXB(RDataSource repo, DataSourceType jaxb, PrismContext prismContext) {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        jaxb.setProviderClass(repo.getProviderClass());
        jaxb.setSpringBean(repo.isSpringBean());
    }

    public DataSourceType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        DataSourceType source = new DataSourceType();
        RDataSource.copyToJAXB(this, source, prismContext);
        return source;
    }
}
