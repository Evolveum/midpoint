package com.evolveum.midpoint.model.api.authentication;

public interface GuiProfileCompilerRegistry {

    void registerCompiler(GuiProfileCompilable compiler);

    void invokeCompiler(CompiledGuiProfile compiledGuiProfile);
}
