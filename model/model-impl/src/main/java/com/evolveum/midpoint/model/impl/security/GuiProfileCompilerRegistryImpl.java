/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilable;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilerRegistry;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GuiProfileCompilerRegistryImpl implements GuiProfileCompilerRegistry {

    private List<GuiProfileCompilable> compilers = new ArrayList<>();

    public void registerCompiler(GuiProfileCompilable compiler) {
        compilers.add(compiler);
    }

    public void invokeCompiler(CompiledGuiProfile compiledGuiProfile) {
        for (GuiProfileCompilable compiler : compilers) {
            compiler.postProcess(compiledGuiProfile);
        }
    }
}
