package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RContainerType;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.ClassDefinitionParser;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public class QueryDefinitionRegistry implements Dumpable {

    private static final Trace LOGGER = TraceManager.getTrace(QueryDefinitionRegistry.class);
    private static final Map<QName, EntityDefinition> definitions;

    private static QueryDefinitionRegistry registry;

    static {
        LOGGER.trace("Initializing query definition registry.");
        ClassDefinitionParser classDefinitionParser = new ClassDefinitionParser();

        Map<QName, EntityDefinition> map = new HashMap<QName, EntityDefinition>();
        Collection<RContainerType> types = ClassMapper.getKnownTypes();
        for (RContainerType type : types) {
            Class clazz = type.getClazz();
            if (!RObject.class.isAssignableFrom(clazz)) {
                continue;
            }

            Definition definition = classDefinitionParser.parseObjectTypeClass(clazz);
            if (definition == null) {
                continue;
            }

            ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(type);
            map.put(objectType.getQName(), (EntityDefinition) definition);
        }

        definitions = Collections.unmodifiableMap(map);
    }

    private QueryDefinitionRegistry() {
    }

    public static QueryDefinitionRegistry getInstance() throws QueryException {
        if (registry == null) {
            registry = new QueryDefinitionRegistry();
        }

        return registry;
    }

    @Override
    public String dump() {
        StringBuilder builder = new StringBuilder();
        Collection<EntityDefinition> defCollection = definitions.values();
        for (Definition definition : defCollection) {
            builder.append(definition.debugDump()).append('\n');
        }

        return builder.toString();
    }

    public <T extends ObjectType, D extends Definition> D findDefinition(Class<T> type, ItemPath path, Class<D> definitionType) {
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(definitionType, "Definition type must not be null.");

        EntityDefinition entityDef = definitions.get(ObjectTypes.getObjectType(type).getQName());
        return entityDef.findDefinition(path, definitionType);
    }
}
