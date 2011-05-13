package com.evolveum.midpoint.web.test;

import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.ObjectFactory;

import java.util.Map;
import java.util.HashMap;

public class MockSessionScope implements Scope {

	private Map<String, Object> scope = new HashMap<String, Object>();

	public Object get(String bean, ObjectFactory<?> factory) {
		Object objectFromScope = scope.get(bean);

		if (objectFromScope == null) {
			objectFromScope = factory.getObject();
			scope.put(bean, objectFromScope);
		}

		return objectFromScope;
	}

	public String getConversationId() {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public void registerDestructionCallback(String name, Runnable callback) {
	}

	public Object remove(String name) {
		return scope.remove(name);
	}

	@Override
	public Object resolveContextualObject(String key) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
