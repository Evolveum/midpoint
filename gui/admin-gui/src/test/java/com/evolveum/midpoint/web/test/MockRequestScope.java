package com.evolveum.midpoint.web.test;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * 
 * @author lazyman
 * 
 */
public class MockRequestScope implements Scope {

	@Override
	public Object get(String name, ObjectFactory<?> factory) {
		return factory.getObject();
	}

	@Override
	public String getConversationId() {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
	}

	@Override
	public Object remove(String name) {
		return null;
	}

	@Override
	public Object resolveContextualObject(String key) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
