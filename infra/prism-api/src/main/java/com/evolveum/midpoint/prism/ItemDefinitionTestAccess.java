/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemName;

/**
 * TEMPORARY HACK to avoid issues with schema immutability for some tests.
 *
 * Reserved for TestStrangeCases in model-intest.
 *
 * DO NOT USE for other purposes.
 *
 * TODO Remove this interface eventually.
 */
public interface ItemDefinitionTestAccess {

    void replaceName(ItemName newName);
}
