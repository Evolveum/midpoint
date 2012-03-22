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

import com.evolveum.midpoint.repo.sql.ClassMapper;
import com.evolveum.midpoint.repo.sql.data.common.RContainerType;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public class QueryRegistry {

    private static QueryRegistry registry;
    private Map<QName, EntityDefinition> definitions = new HashMap<QName, EntityDefinition>();

    public static synchronized QueryRegistry getInstance() throws QueryException {
        if (registry == null) {
            registry = new QueryRegistry();
            registry.init();
        }

        return registry;
    }

    public <T extends ObjectType> EntityDefinition findDefinition(Class<T> type) {
        ObjectTypes def = ObjectTypes.getObjectType(type);
        return findDefinition(def.getQName());
    }

    public EntityDefinition findDefinition(QName qname) {
        Validate.notNull(qname, "QName must not be null.");
        return definitions.get(qname);
    }

    /**
     * Method checks mapped classes through reflection and looks for {@link QueryAttribute} and
     * {@link QueryEntity} annotations. From them it builds query tree - paths that can be queried
     * via {@link QueryInterpreter}
     */
    private void init() throws QueryException {
        Collection<RContainerType> types = ClassMapper.getKnownTypes();

        EntityDefinition account = new EntityDefinition();
        account.setName(SchemaConstants.C_ACCOUNT);
        account.setType(AccountShadowType.COMPLEX_TYPE);
        definitions.put(SchemaConstants.C_ACCOUNT, account);

        AttributeDefinition accountType = new AttributeDefinition();
        accountType.setName(AccountShadowType.F_ACCOUNT_TYPE);
        accountType.setType(DOMUtil.XSD_STRING);
        account.addDefinition(AccountShadowType.F_ACCOUNT_TYPE, accountType);

        EntityDefinition extension = new EntityDefinition();
        extension.setAny(true);
        extension.setName(ObjectType.F_EXTENSION);
        extension.setType(new QName(SchemaConstants.NS_COMMON, "ExtensionType"));
        account.addDefinition(ObjectType.F_EXTENSION, extension);

        EntityDefinition attributes = new EntityDefinition();
        attributes.setAny(true);
        attributes.setName(ResourceObjectShadowType.F_ATTRIBUTES);
        attributes.setType(ResourceObjectShadowType.COMPLEX_TYPE);
        account.addDefinition(ResourceObjectShadowType.F_ATTRIBUTES, attributes);

        if (1 == 1) {
            return;
        }
        //todo implement
        for (RContainerType type : types) {
            if (RObject.class.equals(type.getClazz())) {
                //todo abstract class fix later
                continue;
            }

            Class clazz = type.getClazz();
            Field[] fields = clazz.getFields();
        }
    }
}
