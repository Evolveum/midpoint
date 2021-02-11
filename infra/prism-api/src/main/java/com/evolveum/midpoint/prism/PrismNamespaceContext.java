/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public abstract class PrismNamespaceContext implements Serializable {

    public static final PrismNamespaceContext EMPTY = new Empty();
    public static final String DEFAULT_PREFIX = "";
    public static final PrefixPreference DEFAULT_PREFERENCE = PrefixPreference.LOCAL_FIRST;


    public static final PrismNamespaceContext PRISM_API = from(ImmutableMap.<String, String>builder()
            .put(PrismConstants.PREFIX_NS_TYPES, PrismConstants.NS_TYPES)
            .put(PrismConstants.PREFIX_NS_QUERY, PrismConstants.NS_QUERY)
            .build());

    /**
     * Returns parent namespace context
     * @return parent namespace context
     */
    public abstract Optional<PrismNamespaceContext> parent();

    /**
     * Returns mapping of locally defined prefixes to namespaces
     *
     * Returns mapping of prefixes to namespaces defined in this namespace context.
     *
     * Note that mappings of parent namespaces context also apply, unless the prefix
     * is overriden at current level.
     *
     * @return mapping of locally defined prefixes to namespaces
     */
    public abstract Map<String, String> localPrefixes();

    public abstract Map<String, String> allPrefixes();

    /**
     * Returns true if context is only inherited and not explicitly defined.
     *
     * @return True if context is inherited, false if context is explicitly defined.
     */
    public abstract boolean isInherited();

    /**
     * Return true, if context is empty on local level
     * (no mappings defined, or context is inherited)
     *
     * Note: This is useful for context serialization - if context is empty,
     * it should not be serialized
     *
     * @return True if context does not define local mappings.
     */
    public abstract boolean isLocalEmpty();

    public abstract boolean isEmpty();


    public abstract boolean isDefaultNamespaceOnly();

    /**
     * Returns namespace for specified prefix.
     *
     * If prefix is not defined at current context, parent contexts are lookup up for prefix.
     *
     * @param prefix
     * @return Empty, if no namespace was found, otherwise namespace assigned to supplied prefix.
     */
    public abstract Optional<String> namespaceFor(String prefix);

    public Optional<String> defaultNamespace() {
        return namespaceFor(DEFAULT_PREFIX);
    }

    /**
     * Look up suitable prefix for namespace using default prefix search preference-
     *
     * @param namespace Namespace for which prefix should be returned.
     * @return Prefix which is mapped to namespace.
     */
    public final Optional<String> prefixFor(String namespace) {
        return prefixFor(namespace, DEFAULT_PREFERENCE);
    }

    /**
     * Look up suitable prefix for namespace using provided preference.
     *
     * @param namespace Namespace for which prefix should be returned.
     * @param preference Preference (top-most or closest prefix) which should be returned.
     * @return Prefix which is mapped to namespace.
     */
    public abstract Optional<String> prefixFor(String namespace, PrefixPreference preference);

    /**
     * Creates child namespace context with supplied local mapping
     *
     * @param local Local definition of prefixes
     * @return Child context with local definitions
     */
    public abstract PrismNamespaceContext childContext(Map<String, String> local);

    /**
     * Returns child namespace context with no local mappings.
     *
     * Implementation Note: Implementation ensures that instances of inherited context
     * are reused (eg. <code>context.inherited() == context.inherited()</code>
     * and <code>context.inherited() == context.inherited().inherited()</code> )
     *
     * @return child namespace context with no local mappings.
     */
    public abstract PrismNamespaceContext inherited();

    public static PrismNamespaceContext from(Map<String, String> prefixToNs) {
        if(prefixToNs.isEmpty()) {
            return EMPTY;
        }
        return new Impl(null, prefixToNs);
    }

    private static class Impl extends PrismNamespaceContext {

        private static final long serialVersionUID = 1L;
        private final Impl parent;
        private final Map<String, String> prefixToNs;
        private transient Multimap<String,String> nsToPrefix;

        private final PrismNamespaceContext inherited = new Inherited(this);



        public Impl(Impl parent, Map<String, String> local) {
            super();
            this.parent = parent;
            this.prefixToNs = ImmutableMap.copyOf(local);
        }

        @Override
        public Optional<PrismNamespaceContext> parent() {
            return Optional.ofNullable(parent);
        }

        @Override
        public Map<String, String> localPrefixes() {
            return prefixToNs;
        }

        @Override
        public boolean isInherited() {
            return false;
        }

        @Override
        public boolean isLocalEmpty() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public PrismNamespaceContext inherited() {
            return inherited;
        }

        @Override
        public PrismNamespaceContext childContext(Map<String, String> local) {
            if(local.isEmpty()) {
                return inherited;
            }
            return new Impl(this, local);
        }

        @Override
        public Optional<String> namespaceFor(String prefix) {
            if(prefix == null) {
                prefix = DEFAULT_PREFIX;
            }
            String value = localPrefixes().get(prefix);
            if(value == null && parent != null) {
                return parent.namespaceFor(prefix);
            }
            return Optional.ofNullable(value);
        }

        @Override
        public Optional<String> prefixFor(String ns, PrefixPreference preference) {
            // FIXME: Add caching of result? May speed up serialization
            Preconditions.checkNotNull(ns, "namespace must not be null");
            Preconditions.checkNotNull(preference, "PrefixPreference must not be null");

            Collection<String> candidates = preference.apply(this, ns);
            if(candidates.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(candidates.iterator().next());
        }

        /**
         *
         * Returns all usable prefixes from parent node
         *
         * We need to return all prefixes instead of first one, since they can be overriden
         * on nested levels.
         *
         * @param ns Namespace
         * @param preference Prefix preference (prefer top-most prefix vs closest prefix)
         * @return Mutable list of available prefixes (prefixes which are not overriden in this or parent contexts).
         * @see PrismNamespaceContext.PrefixPreference
         */
        protected List<String> parentPrefixesFor(String ns, PrefixPreference preference) {
            if(parent == null) {
                // We explicitly return mutable list, so it can be modified by caller
                return new ArrayList<>();
            }
            List<String> result = preference.apply(parent, ns);
            // Now we remove conflicts
            Iterator<String> it = result.iterator();
            while(it.hasNext()) {
                String prefix = it.next();
                String overrideNs = prefixToNs.get(prefix);

                if(overrideNs != null && !ns.equals(overrideNs)) {
                    // namespace for prefix is different, we can not use it
                    it.remove();
                }
            }
            return result;
        }

        @Override
        public Map<String, String> allPrefixes() {
            Map<String, String> prefixes = new HashMap<>();
            Impl current = this;
            while(current != null) {
                for (Entry<String, String> mapping : current.localPrefixes().entrySet()) {
                    prefixes.putIfAbsent(mapping.getKey(), mapping.getValue());
                }
                current = current.parent;

            }
            return prefixes;
        }

        @Override
        public boolean isDefaultNamespaceOnly() {
            return prefixToNs.size() == 1 && prefixToNs.containsKey(DEFAULT_PREFIX);
        }

        @Override
        public PrismNamespaceContext rebasedOn(PrismNamespaceContext current) {
            if(isParent(current)) {
                // We do not need to rebase to self
                return this;
            }
            ImmutableMap.Builder<String,String> prefixesToRebase = ImmutableMap.builder();
            for(Entry<String, String> prefix : allPrefixes().entrySet()) {
                Optional<String> other = current.namespaceFor(prefix.getKey());
                if(other.isEmpty() || !other.get().equals(prefix.getValue()) ) {
                    prefixesToRebase.put(prefix);
                }
            }
            return current.childContext(prefixesToRebase.build());
        }

        private boolean isParent(PrismNamespaceContext current) {
            if(parent == current) {
                return true;
            }
            if(current instanceof Inherited) {
                return parent == ((Inherited) current).parent;
            }
            return false;
        }

        private Multimap<String, String> nsToPrefix() {
            if(nsToPrefix != null) {
                return nsToPrefix;
            }
            // race is not problem since source is immutable
            // and result is same.
            ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
            for(Entry<String, String> e : prefixToNs.entrySet()) {
                builder.put(e.getValue(), e.getKey());
            }
            this.nsToPrefix = builder.build();
            return nsToPrefix;
        }

    }

    private static class Inherited extends PrismNamespaceContext {

        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final Impl parent;

        public Inherited(Impl parent) {
            this.parent = parent;
        }

        @Override
        public Optional<PrismNamespaceContext> parent() {
            return parent.parent();
        }

        @Override
        public Map<String, String> localPrefixes() {
            return Collections.emptyMap();
        }

        @Override
        public boolean isInherited() {
            return true;
        }

        @Override
        public boolean isLocalEmpty() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean isDefaultNamespaceOnly() {
            return false;
        }

        @Override
        public PrismNamespaceContext inherited() {
            return this;
        }

        @Override
        public PrismNamespaceContext childContext(Map<String, String> local) {
            return parent.childContext(local);
        }

        @Override
        public Optional<String> namespaceFor(String prefix) {
            return parent.namespaceFor(prefix);
        }

        @Override
        public Optional<String> prefixFor(String nsQuery, PrefixPreference pref) {
            return parent.prefixFor(nsQuery, pref);
        }

        @Override
        public Map<String, String> allPrefixes() {
            return parent.allPrefixes();
        }

        @Override
        public PrismNamespaceContext rebasedOn(PrismNamespaceContext current) {
            return parent.rebasedOn(current);
        }
    }

    private static class Empty extends PrismNamespaceContext {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        public Optional<PrismNamespaceContext> parent() {
            return Optional.empty();
        }

        @Override
        public Map<String, String> localPrefixes() {
            return Collections.emptyMap();
        }

        @Override
        public boolean isInherited() {
            return false;
        }

        @Override
        public boolean isLocalEmpty() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean isDefaultNamespaceOnly() {
            return false;
        }

        @Override
        public Optional<String> namespaceFor(String prefix) {
            return Optional.empty();
        }

        @Override
        public Optional<String> prefixFor(String namespace, PrefixPreference preference) {
            return Optional.empty();
        }

        @Override
        public PrismNamespaceContext childContext(Map<String, String> local) {
            return from(local);
        }

        @Override
        public PrismNamespaceContext inherited() {
            return this;
        }

        @Override
        public Map<String, String> allPrefixes() {
            return Collections.emptyMap();
        }

        @Override
        public PrismNamespaceContext rebasedOn(PrismNamespaceContext current) {
            return current.inherited();
        }

        @Override
        public @NotNull PrismNamespaceContext withoutDefault() {
            return super.withoutDefault();
        }

    }

    public enum PrefixPreference {

        /**
         * Returns first found topmost (closest to namespace root) prefix
         */
        GLOBAL_FIRST {

            @Override
            List<String> apply(Impl context, String namespace) {
                final List<String> result = context.parentPrefixesFor(namespace, this);
                result.addAll(context.nsToPrefix().get(namespace));
                return result;
            }
        },
        /**
         * Returns first found topmost (closest to namespace root) prefix which is not default
         */
        GLOBAL_FIRST_SKIP_DEFAULTS {
            @Override
            List<String> apply(Impl context, String namespace) {
                final List<String> result = context.parentPrefixesFor(namespace, this);
                Collection<String> local = context.nsToPrefix().get(namespace);
                for(String prefix : local) {
                    if(!DEFAULT_PREFIX.equals(prefix)) {
                        result.add(prefix);
                    }
                }
                return result;
            }
        },
        LOCAL_FIRST {
            @Override
            List<String> apply(Impl context, String namespace) {
                Collection<String> candidates = context.nsToPrefix().get(namespace);
                if(!candidates.isEmpty()) {
                    return new ArrayList<>(candidates);
                }
                return context.parentPrefixesFor(namespace, this);
            }
        };

        /**
         * Lookup all usable prefixes for specified namespace
         *
         * @param context Namespace context on which perform prefix lookup
         * @param namespace Namespace for which prefix should be looked-up
         * @return Mutable list of usable prefixes
         */
        abstract List<String> apply(Impl context, String namespace);
    }

    public static PrismNamespaceContext of(String ns) {
        return from(ImmutableMap.of(DEFAULT_PREFIX, ns));
    }

    public PrismNamespaceContext childDefaultNamespace(String namespace) {
        return childContext(ImmutableMap.of(DEFAULT_PREFIX, namespace));
    }

    public abstract PrismNamespaceContext rebasedOn(PrismNamespaceContext current);

    public @NotNull PrismNamespaceContext withoutDefault() {
        return childDefaultNamespace("");
    }

    @Override
    public String toString() {
        return new StringBuilder(this.getClass().getSimpleName()).append(allPrefixes().toString()).toString();
    }

}
