/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.hooks;

import java.util.Collection;
import java.util.List;

/**
 * @author semancik
 *
 */
public interface HookRegistry {

    void registerChangeHook(String url, ChangeHook changeHook);

    List<ChangeHook> getAllChangeHooks();

    void registerReadHook(String url, ReadHook searchHook);

    Collection<ReadHook> getAllReadHooks();
}
