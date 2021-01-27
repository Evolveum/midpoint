/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

public abstract class XNodeNamespaceContext {



    public static final XNodeNamespaceContext EMPTY = new XNodeNamespaceContext() {

        @Override
        public Optional<XNodeNamespaceContext> parent() {
            return Optional.empty();
        }

        @Override
        public Map<String, String> local() {
            return Collections.emptyMap();
        }

        @Override
        public boolean isInherited() {
            return false;
        }

        @Override
        public XNodeNamespaceContext childContext(Map<String, String> local) {
            return from(local);
        }
    };

    public abstract Optional<XNodeNamespaceContext> parent();

    protected static XNodeNamespaceContext from(Map<String, String> local) {
        if(local.isEmpty()) {
            return EMPTY;
        }
        return new Impl(null, local);
    }

    public abstract Map<String, String> local();

    public abstract boolean isInherited();

    public final String get(String key) {
        String value = local().get(key);
        if(value == null && parent().isPresent()) {
            return parent().get().get(value);
        }
        return value;
    }

    public abstract XNodeNamespaceContext childContext(Map<String, String> local);

    private static class Impl extends XNodeNamespaceContext {

        private final XNodeNamespaceContext parent;
        private final Map<String, String> local;

        private final XNodeNamespaceContext inherited = new Inherited(this);



        public Impl(XNodeNamespaceContext parent, Map<String, String> local) {
            super();
            this.parent = parent;
            this.local = ImmutableMap.copyOf(local);
        }

        @Override
        public Optional<XNodeNamespaceContext> parent() {
            return Optional.ofNullable(parent);
        }

        @Override
        public Map<String, String> local() {
            return local;
        }

        @Override
        public boolean isInherited() {
            return false;
        }

        @Override
        public XNodeNamespaceContext childContext(Map<String, String> local) {
            if(local.isEmpty()) {
                return inherited;
            }
            return new Impl(this, local);
        }
    }

    private static class Inherited extends XNodeNamespaceContext {

        private final Impl parent;

        public Inherited(Impl parent) {
            this.parent = parent;
        }

        @Override
        public Optional<XNodeNamespaceContext> parent() {
            return parent.parent();
        }

        @Override
        public Map<String, String> local() {
            return parent.local();
        }

        @Override
        public boolean isInherited() {
            return true;
        }

        @Override
        public XNodeNamespaceContext childContext(Map<String, String> local) {
            return parent.childContext(local);
        }

    }

}
