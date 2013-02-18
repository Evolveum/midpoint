package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.repo.sql.data.common.RContainerType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.ClassDefinitionParser;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public class QueryDefinitionRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(QueryDefinitionRegistry.class);
    private static final Map<QName, Definition> definitions;

    private static QueryDefinitionRegistry registry;

    static {
        LOGGER.trace("Initializing query definition registry.");
        ClassDefinitionParser classDefinitionParser = new ClassDefinitionParser();

        Map<QName, Definition> map = new HashMap<QName, Definition>();
        Collection<RContainerType> types = ClassMapper.getKnownTypes();
        for (RContainerType type : types) {
            Class clazz = type.getClazz();

            Definition definition = classDefinitionParser.parse(clazz);
            if (definition == null) {
                continue;
            }

            ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(type);
            map.put(objectType.getQName(), definition);
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

    public String dump() {
        StringBuilder builder = new StringBuilder();
        Collection<Definition> defCollection = definitions.values();
        for (Definition definition : defCollection) {
            builder.append(definition).append('\n');
        }

        return builder.toString();
    }
}
