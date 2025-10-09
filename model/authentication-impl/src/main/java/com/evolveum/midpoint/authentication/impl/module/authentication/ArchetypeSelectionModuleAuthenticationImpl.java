/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.config.ArchetypeSelectionModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeSelectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

import java.io.Serializable;

public class ArchetypeSelectionModuleAuthenticationImpl extends CredentialModuleAuthenticationImpl implements
        ArchetypeSelectionModuleAuthentication, Serializable {

    private boolean allowUndefined;
    private ArchetypeSelectionType archetypeSelection;

    public ArchetypeSelectionModuleAuthenticationImpl(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.ARCHETYPE_SELECTION, sequenceModule);
        setSufficient(false);
    }

    public ModuleAuthenticationImpl clone() {
        ArchetypeSelectionModuleAuthenticationImpl module = new ArchetypeSelectionModuleAuthenticationImpl(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        module.setArchetypeSelection(this.archetypeSelection);
        module.setAllowUndefined(this.allowUndefined);
        super.clone(module);
        return module;
    }

    @Override
    public boolean isAllowUndefined() {
        return allowUndefined;
    }

    public void setAllowUndefined(boolean allowUndefined) {
        this.allowUndefined = allowUndefined;
    }

    public void setArchetypeSelection(ArchetypeSelectionType archetypeSelection) {
        this.archetypeSelection = archetypeSelection;
    }

    @Override
    public ArchetypeSelectionType getArchetypeSelection() {
        return archetypeSelection;
    }
}
