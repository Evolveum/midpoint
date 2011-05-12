package com.gridpulse.xandria.translator;

import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.ObjectFactory;

import java.util.Map;
import java.util.HashMap;

/**
 * User: BogdanCo
 * Date: Aug 21, 2009
 * Time: 6:38:01 PM
 */
public class MockSessionScope implements Scope {
    private Map<String, Object> sessionScopeMap = new HashMap<String, Object>();

    public Object get(String bean, ObjectFactory factory) {
        Object objectFromScope = sessionScopeMap.get(bean);

        if (objectFromScope == null) {
            objectFromScope = factory.getObject();
            sessionScopeMap.put(bean, objectFromScope);
        }

        return objectFromScope;
    }

    public String getConversationId() {
        throw new RuntimeException("Not implimented");
    }

    public void registerDestructionCallback(String arg0, Runnable arg1) {
    }

    public Object remove(String bean) {
        return sessionScopeMap.remove(bean);
    }

    @Override
    public Object resolveContextualObject(String arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

