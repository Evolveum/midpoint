/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.authentication;

public interface GuiProfileCompilerRegistry {

    void registerCompiler(GuiProfileCompilable compiler);

    void invokeCompiler(CompiledGuiProfile compiledGuiProfile);
}
