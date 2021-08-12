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
