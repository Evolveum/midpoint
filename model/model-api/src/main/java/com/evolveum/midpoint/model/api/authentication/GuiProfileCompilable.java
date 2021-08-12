package com.evolveum.midpoint.model.api.authentication;

public interface GuiProfileCompilable {

    void register();

    void postProcess(CompiledGuiProfile compiledGuiProfile);
}
