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

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.query.definition.Definition;
import com.evolveum.midpoint.repo.sql.query.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;

import javax.xml.namespace.QName;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public class QueryContext {

    private QueryInterpreter interpreter;
    private PrismContext prismContext;
    private Session session;

    private ObjectQuery query;

    private Class<? extends ObjectType> type;

    private final Map<ItemPath, Criteria> criterias = new HashMap<ItemPath, Criteria>();
    private final Map<ItemPath, String> aliases = new HashMap<ItemPath, String>();

    public QueryContext(QueryInterpreter interpreter, Class<? extends ObjectType> type, ObjectQuery query,
                        PrismContext prismContext, Session session) {
        this.interpreter = interpreter;
        this.type = type;
        this.query = query;
        this.prismContext = prismContext;
        this.session = session;

        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();

        String alias = addAlias(null, registry.findDefinition(type, null, EntityDefinition.class));
        addCriteria(null, session.createCriteria(ClassMapper.getHQLTypeClass(type), alias));
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public Session getSession() {
        return session;
    }

    public QueryInterpreter getInterpreter() {
        return interpreter;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public Criteria getCriteria(ItemPath path) {
        return criterias.get(path);
    }

    public void addCriteria(ItemPath path, Criteria criteria) {
        criterias.put(path, criteria);
    }

    public String getAlias(ItemPath path) {
        return aliases.get(path);
    }

    public void addAlias(ItemPath path, String alias) {
        if (aliases.containsKey(path)) {
            if (!StringUtils.equals(alias, aliases.get(path))) {
                throw new IllegalArgumentException("Path '" + path + "' (" + alias
                        + ") is already defined in alias map with alias (" + aliases.get(path) + ").");
            }

            return;
        }

        aliases.put(path, alias);
    }

    public String addAlias(ItemPath path, Definition def) {
        QName qname;
        if (path == null) {
            //get qname from class type
            qname = ObjectTypes.getObjectType(type).getQName();
        } else {
            if (!path.isEmpty()) {
                //get last qname from path
                qname = ItemPath.getName(path.last());
            } else {
                throw new IllegalArgumentException("Item path must not be empty.");
            }
        }

        String alias = createAlias(def, qname);
        aliases.put(path, alias);

        return alias;
    }

    private String createAlias(Definition def, QName qname) {
        String prefix;
        if (def != null) {
            //we want to skip 'R' prefix for entity definition names
            int prefixIndex = (def instanceof EntityDefinition) ? 1 : 0;
            prefix = Character.toString(def.getJpaName().charAt(prefixIndex)).toLowerCase();
        } else {
            prefix = Character.toString(qname.getLocalPart().charAt(0)).toLowerCase();
        }

        int index = 1;
        String alias = prefix;
        while (hasAlias(alias)) {
            alias = prefix + Integer.toString(index);
            index++;

            if (index > 5) {
                throw new IllegalStateException("Alias index for definition '" + def
                        + "' is more than 5? This probably should not happen.");
            }
        }

        return alias;
    }

    private boolean hasAlias(String alias) {
        return aliases.containsValue(alias);
    }

    public boolean hasAlias(ItemPath path) {
        return aliases.get(path) != null;
    }
}
