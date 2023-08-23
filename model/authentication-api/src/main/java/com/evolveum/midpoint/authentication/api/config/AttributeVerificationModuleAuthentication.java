package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.prism.path.ItemPath;

import java.util.List;

public interface AttributeVerificationModuleAuthentication extends ModuleAuthentication {

    List<ItemPath> getPathsToVerify();
}
