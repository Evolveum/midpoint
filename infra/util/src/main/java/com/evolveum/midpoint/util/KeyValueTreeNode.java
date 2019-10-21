/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Experimental.
 */
public class KeyValueTreeNode<K, V> extends TreeNode<Pair<K, V>> {

    public KeyValueTreeNode(K key, V value) {
        super(new ImmutablePair<>(key, value));
    }

    public KeyValueTreeNode<K, V> createChild(K key, V value) {
        KeyValueTreeNode<K, V> child = new KeyValueTreeNode<>(key, value);
        add(child);
        return child;
    }
}
