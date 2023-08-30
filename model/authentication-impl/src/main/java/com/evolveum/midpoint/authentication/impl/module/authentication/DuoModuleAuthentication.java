/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.RemoteModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.util.ModuleType;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import java.io.Serializable;

public class DuoModuleAuthentication extends RemoteModuleAuthenticationImpl implements RemoteModuleAuthentication, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(DuoModuleAuthentication.class);

    private String duoState;

    private String duoUsername;

    private final ItemPath pathToUsernameAttribute;

    public DuoModuleAuthentication(AuthenticationSequenceModuleType sequenceModule, ItemPath pathToUsernameAttribute) {
        super(AuthenticationModuleNameConstants.DUO, sequenceModule);
        this.pathToUsernameAttribute = pathToUsernameAttribute;
        setType(ModuleType.REMOTE);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
        setSufficient(false);
    }

    public ModuleAuthenticationImpl clone() {
        DuoModuleAuthentication module = new DuoModuleAuthentication(this.getSequenceModule(), this.pathToUsernameAttribute);
        module.setAuthentication(this.getAuthentication());
        module.setDuoState(this.getDuoState());
        module.setDuoUsername(this.getDuoUsername());
        module.setProviders(this.getProviders());

        clone(module);
        return module;
    }

    @Override
    public boolean applicable() {
        if (getDuoUsername() != null) {
            return true;
        }

        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal == null || principal.getFocus() == null) {
            return false;
        }
        PrismObject<? extends FocusType> focus = principal.getFocusPrismObject();
        PrismProperty<Object> username = focus.findProperty(pathToUsernameAttribute);

        if (username == null) {
            LOGGER.debug("Couldn't find attribute for duo username with path " + pathToUsernameAttribute + " in focus " + focus);
            return false;
        }

        Object realValue = username.getRealValue();
        if (realValue == null) {
            LOGGER.debug("Found attribute for duo username with null value, found attribute:" + username);
            return false;
        }

        setDuoUsername(String.valueOf(realValue));

        return true;
    }

    public String getDuoState() {
        return duoState;
    }

    public void setDuoState(String duoState) {
        this.duoState = duoState;
    }

    public void setDuoUsername(String duoUsername) {
        this.duoUsername = duoUsername;
    }

    public String getDuoUsername() {
        return duoUsername;
    }
}
