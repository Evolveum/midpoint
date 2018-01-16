/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import org.hibernate.Metamodel;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.metamodel.EntityType;
import java.util.HashMap;
import java.util.Map;

/**
 * // todo documentation, probably rename
 *
 * @author Viliam Repan (lazyman)
 */

@Service
public class EntityModificationRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    private Metamodel metamodel;

    private Map<Class, EntityType> jaxbMappings = new HashMap<>();


    // todo handle RObjectTextInfo
    // todo handle RAssignmentExtension

    @PostConstruct
    public void init() {
        // todo implement

        metamodel = sessionFactory.getMetamodel();
        for (EntityType entity : metamodel.getEntities()) {
            Class javaType = entity.getJavaType();
            Ignore ignore = (Ignore) javaType.getAnnotation(Ignore.class);
            if (ignore != null) {
                continue;
            }

            Class jaxb;
            if (RObject.class.isAssignableFrom(javaType)) {
                jaxb = RObjectType.getType(javaType).getJaxbClass();
            } else {
                JaxbType jaxbType = (JaxbType) javaType.getAnnotation(JaxbType.class);
                if (jaxbType == null) {
                    throw new IllegalStateException("Unknown jaxb type for " + javaType.getName());
                }
                jaxb = jaxbType.type();
            }

            jaxbMappings.put(jaxb, entity);
        }

        System.out.println("asdf");
    }

    public EntityType getJaxbMapping(Class type) {
        return jaxbMappings.get(type);
    }

    public EntityType getMapping(Class type) {
        return metamodel.entity(type);
    }
}
