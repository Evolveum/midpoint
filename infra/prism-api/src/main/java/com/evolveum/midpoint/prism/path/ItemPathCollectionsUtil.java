/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Methods that operate on path collections.
 */
public class ItemPathCollectionsUtil {

    /**
     * Returns true if the collection contains a path equivalent to pathToBeFound.
     */
    public static boolean containsEquivalent(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
        for (ItemPath path : paths) {
            if (path.equivalent(pathToBeFound)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the collection contains a superpath of the given path.
     * I.e. having collection = { A/B, A/C }
     * then the method for this collection and 'path' returns:
     *  - path = A/B -&gt; false
     *  - path = A -&gt; false
     *  - path = A/B/C -&gt; true
     *  - path = X -&gt; false
     */
    public static boolean containsSubpath(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
        for (ItemPath path : paths) {
            if (path.isSubPath(pathToBeFound)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the collection contains a superpath of or equivalent path to the given path.
     * I.e. having collection = { A/B, A/C }
     * then the method for this collection and 'pathToBeFound' returns:
     *  - pathToBeFound = A/B -&gt; true
     *  - pathToBeFound = A -&gt; true
     *  - pathToBeFound = A/B/C -&gt; false
     *  - pathToBeFound = X -&gt; false
     */
    public static boolean containsSuperpathOrEquivalent(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
        for (ItemPath path : paths) {
            if (path.isSuperPathOrEquivalent(pathToBeFound)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the collection contains a superpath of the given path.
     * I.e. having collection = { A/B, A/C }
     * then the method for this collection and 'path' returns:
     *  - path = A/B -&gt; false
     *  - path = A -&gt; true
     *  - path = A/B/C -&gt; false
     *  - path = X -&gt; false
     */
    public static boolean containsSuperpath(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
        for (ItemPath path : paths) {
            if (path.isSuperPath(pathToBeFound)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the collection contains a subpath of or equivalent path to the given path.
     * I.e. having collection = { A/B, A/C }
     * then the method for this collection and 'path' returns:
     *  - path = A/B -&gt; true
     *  - path = A -&gt; false
     *  - path = A/B/C -&gt; true
     *  - path = X -&gt; false
     */
    public static boolean containsSubpathOrEquivalent(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
        for (ItemPath path : paths) {
            if (path.isSubPathOrEquivalent(pathToBeFound)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Strips the prefix from a set of paths.
     *
     * @param alsoEquivalent If true, 'prefix' in paths is processed as well (resulting in empty path). Otherwise, it is skipped.
     */
    public static List<ItemPath> remainder(Collection<? extends ItemPath> paths, ItemPath prefix, boolean alsoEquivalent) {
        List<ItemPath> rv = new ArrayList<>();
        for (ItemPath path : paths) {
            if (alsoEquivalent && path.isSuperPathOrEquivalent(prefix)
                    || !alsoEquivalent && path.isSuperPath(prefix)) {
                rv.add(path.remainder(prefix));
            }
        }
        return rv;
    }

    @NotNull
    public static List<ItemPath> pathListFromStrings(List<String> pathsAsStrings,
            PrismContext prismContext) {
        List<ItemPath> rv = new ArrayList<>();
        if (pathsAsStrings != null) {
            for (String pathAsString : pathsAsStrings) {
                rv.add(prismContext.itemPathParser().asItemPath(pathAsString));
            }
        }
        return rv;
    }

    public static ItemPath[] asPathArray(QName... names) {
        ItemPath[] paths = new ItemPath[names.length];
        int i = 0;
        for (QName name : names) {
            paths[i++] = ItemName.fromQName(name);
        }
        return paths;
    }

    public static UniformItemPath[] asUniformPathArray(PrismContext prismContext, QName... names) {
        UniformItemPath[] paths = new UniformItemPath[names.length];
        int i = 0;
        for (QName name : names) {
            paths[i++] = prismContext.path(name);
        }
        return paths;
    }

    public static <T> T putToMap(Map<UniformItemPath, T> map, UniformItemPath itemPath, T value) {
        for (UniformItemPath key : map.keySet()) {
            if (key.equivalent(itemPath)) {
                return map.put(key, value);
            }
        }
        return map.put(itemPath, value);
    }

    public static <T> void putAllToMap(Map<UniformItemPath, T> target, Map<UniformItemPath, T> source) {
        for (Map.Entry<UniformItemPath, T> entry : source.entrySet()) {
            putToMap(target, entry.getKey(), entry.getValue());
        }
    }

    public static <T> T getFromMap(Map<UniformItemPath, T> map, UniformItemPath itemPath) {
        for (Map.Entry<UniformItemPath, T> entry : map.entrySet()) {
            if (entry.getKey().equivalent(itemPath)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
