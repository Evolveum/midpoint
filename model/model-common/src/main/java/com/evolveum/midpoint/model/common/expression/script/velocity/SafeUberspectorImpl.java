/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.velocity;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.Introspector;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.slf4j.Logger;

/**
 * A Velocity uberspector that uses our own safe {@link Introspector} to control access to Java methods.
 *
 * - Uberspector is responsible for looking up iterators, methods, and properties for referenced Java classes.
 * - Introspector is responsible for looking up Java methods and fields, as directed e.g. by the Uberspector.
 *
 * It is appropriate to implement the access control at the level of Introspector.
 * We override just {@link #getIterator(Object, Info)}, see the implementation.
 */
@SuppressWarnings("WeakerAccess") // instantiated by reflection from Velocity engine, so it must be public
public class SafeUberspectorImpl extends UberspectImpl {

    @Override
    public void init() {
        introspector = new SafeIntrospectorImpl(log);
    }

    @Override
    public Iterator<?> getIterator(Object obj, Info i) {

        if (AbstractVelocityScriptExecutor.isFullExecutionMode()) {
            return super.getIterator(obj, i);
        }

        if (obj.getClass().isArray()
                || obj instanceof Iterable
                || obj instanceof Map
                || obj instanceof Iterator
                || obj instanceof Enumeration) {
            // These cases are iterable in a safe way by the superclass.
            return super.getIterator(obj, i);
        } else {
            // We don't want to call "iterator" method by name. Even that shouldn't be a problem, because in our Velocity
            // context all objects should be safe in the sense thay don't have adangerous "iterator" method. But let's play
            // it safe.
            getLog().error("Forbidden attempt to iterate over object of class {} in Velocity script", obj.getClass().getName());
            return null;
        }
    }

    private Logger getLog() {
        return ((SafeIntrospectorImpl) introspector).getLog();
    }
}

