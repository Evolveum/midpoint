/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.config.AttributeVerificationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.util.List;
import java.util.stream.Collectors;

public class AttributeVerificationModuleAuthenticationImpl extends CredentialModuleAuthenticationImpl implements AttributeVerificationModuleAuthentication {

    private List<ItemPathType> paths;

    public AttributeVerificationModuleAuthenticationImpl(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION, sequenceModule);
        setSufficient(false);
    }

    public ModuleAuthenticationImpl clone() {
        AttributeVerificationModuleAuthenticationImpl module = new AttributeVerificationModuleAuthenticationImpl(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        module.setPathsToVerify(this.paths);
        super.clone(module);
        return module;
    }

    public void setPathsToVerify(List<ItemPathType> paths) {
        this.paths = paths;
    }

    public List<ItemPath> getPathsToVerify() {
        if (paths == null) {
            return null;
        }
        return paths.stream()
                .map(ItemPathType::getItemPath)
                .collect(Collectors.toList());
    }
}
