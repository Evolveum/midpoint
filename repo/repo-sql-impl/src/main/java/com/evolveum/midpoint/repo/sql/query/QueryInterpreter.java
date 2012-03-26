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

package com.evolveum.midpoint.repo.sql.query;


import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.sql.ClassMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class QueryInterpreter {

    private static final Trace LOGGER = TraceManager.getTrace(QueryInterpreter.class);
    private PrismContext prismContext;
    //query context stuff
    private Class<? extends ObjectType> type;
    private Map<PropertyPath, Criteria> criterions = new HashMap<PropertyPath, Criteria>();
    private Map<PropertyPath, String> aliases = new HashMap<PropertyPath, String>();

    public QueryInterpreter(Session session, Class<? extends ObjectType> type, PrismContext prismContext) {
        this.prismContext = prismContext;
        this.type = type;

        String alias = createAlias(ObjectTypes.getObjectType(type).getQName());
        Criteria criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type), alias);
        setCriteria(null, criteria);
        setAlias(null, alias);
    }

    public Criteria interpret(Element filter) throws QueryException {
        Validate.notNull(filter, "Element filter must not be null.");

        LOGGER.debug("Interpreting query '{}', query on trace level.",
                new Object[]{DOMUtil.getQNameWithoutPrefix(filter)});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Query filter:\n{}", new Object[]{DOMUtil.printDom(filter).toString()});
        }

        try {
            Criterion criterion = interpret(filter, false);

            Criteria criteria = getCriteria(null);
            criteria.add(criterion);

            return criteria;
        } catch (QueryException ex) {
            LOGGER.trace(ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.trace(ex.getMessage(), ex);
            throw new QueryException(ex.getMessage(), ex);
        }
    }

    public Criterion interpret(Element filter, boolean pushNot) throws QueryException {
        //todo fix operation choosing and initialization...
        Op operation = new LogicalOp(this);
        if (operation.canHandle(filter)) {
            return operation.interpret(filter, pushNot);
        }
        operation = new SimpleOp(this);
        if (operation.canHandle(filter)) {
            return operation.interpret(filter, pushNot);
        }

        throw new QueryException("Unsupported query filter '"
                + DOMUtil.getQNameWithoutPrefix(filter) + "'.");
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public ItemDefinition findDefinition(Element path, QName name) {
        SchemaRegistry registry = prismContext.getSchemaRegistry();
        PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(type);

        PropertyPath propertyPath = createPropertyPath(path);
        if (propertyPath == null) {
            propertyPath = PropertyPath.EMPTY_PATH;
        }

        List<PropertyPathSegment> segments = propertyPath.getSegments();
        segments.add(new PropertyPathSegment(name));
        propertyPath = new PropertyPath(segments);

        return objectDef.findItemDefinition(propertyPath);
    }

    public PropertyPath createPropertyPath(Element path) {
        PropertyPath propertyPath = null;
        if (path != null && StringUtils.isNotEmpty(path.getTextContent())) {
            propertyPath = new XPathHolder(path).toPropertyPath();
        }

        return propertyPath;
    }

    public String createAlias(QName qname) {
        String prefix = Character.toString(qname.getLocalPart().charAt(0)).toLowerCase();
        int index = 1;

        String alias = prefix;
        while (hasAlias(alias)) {
            alias = prefix + Integer.toString(index);
            index++;

            if (index > 20) {
                throw new IllegalStateException("Alias index for segment '" + qname
                        + "' is more than 20? Should not happen.");
            }
        }

        return alias;
    }

    public Criteria getCriteria(PropertyPath path) {
        return criterions.get(path);
    }

    public void setCriteria(PropertyPath path, Criteria criteria) {
        Validate.notNull(criteria, "Criteria must not be null.");
        if (criterions.containsKey(path)) {
            throw new IllegalArgumentException("Already has criteria with this path '" + path + "'");
        }

        criterions.put(path, criteria);
    }

    public String getAlias(PropertyPath path) {
        return aliases.get(path);
    }

    public void setAlias(PropertyPath path, String alias) {
        Validate.notNull(alias, "Alias must not be null.");
        if (aliases.containsValue(alias)) {
            throw new IllegalArgumentException("Already has alias '" + alias + "' with this path '" + path + "'.");
        }

        aliases.put(path, alias);
    }

    public boolean hasAlias(String alias) {
        return aliases.containsValue(alias);
    }
}
