/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.component;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.page.admin.home.dto.PersonalInfoDto;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;

/**
 * @author lazyman
 */
public class PersonalInfoPanel extends BasePanel<PersonalInfoDto> {

    private static final String ID_LAST_LOGIN_DATE = "lastLoginDate";
    private static final String ID_LAST_LOGIN_FROM = "lastLoginFrom";
    private static final String ID_LAST_FAIL_DATE = "lastFailDate";
    private static final String ID_LAST_FAIL_FROM = "lastFailFrom";
    private static final String ID_PASSWORD_EXP = "passwordExp";

    public PersonalInfoPanel(String id) {
        super(id, null);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    public IModel<PersonalInfoDto> createModel() {
        return new LoadableModel<PersonalInfoDto>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected PersonalInfoDto load() {
                return loadPersonalInfo();
            }
        };
    }

    private PersonalInfoDto loadPersonalInfo() {
        FocusType user = SecurityUtils.getPrincipalUser().getFocus();
        CredentialsType credentials = user.getCredentials();
        PersonalInfoDto dto = new PersonalInfoDto();
        if (credentials != null) {
            PasswordType password = credentials.getPassword();

            if (password.getPreviousSuccessfulLogin() != null) {
                dto.setLastLoginDate(MiscUtil.asDate(password.getPreviousSuccessfulLogin().getTimestamp()));
                dto.setLastLoginFrom(password.getPreviousSuccessfulLogin().getFrom());
            }

            if (password.getLastFailedLogin() != null) {
                dto.setLastFailDate(MiscUtil.asDate(password.getLastFailedLogin().getTimestamp()));
                dto.setLastFailFrom(password.getLastFailedLogin().getFrom());
            }
        }
        if (user.getActivation() != null) {
            //todo fix, this is not password expiration date...
            dto.setPasswordExp(MiscUtil.asDate(user.getActivation().getValidTo()));
        }

        return dto;
    }

    protected void initLayout() {
        DateLabelComponent lastLoginDate = new DateLabelComponent(ID_LAST_LOGIN_DATE, new IModel<Date>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Date getObject() {

                if (getModel() == null) {
                    return null;
                }
                PersonalInfoDto dto = getModel().getObject();
                return dto == null ? null : dto.getLastLoginDate();
            }
        }, WebComponentUtil.getLongDateTimeFormat(getPageBase()));
        lastLoginDate.setBeforeTextOnDateNull(getPageBase().getString("PersonalInfoPanel.never"));
        add(lastLoginDate);

        Label lastLoginFrom = new Label(ID_LAST_LOGIN_FROM, new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (getModel() == null) {
                    return PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
                }
                PersonalInfoDto dto = getModel().getObject();

                return StringUtils.isNotEmpty(dto.getLastLoginFrom()) ? dto.getLastLoginFrom() :
                        PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
            }
        });
        add(lastLoginFrom);

        DateLabelComponent lastFailDate = new DateLabelComponent(ID_LAST_FAIL_DATE, new IModel<Date>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Date getObject() {
                if (getModel() == null) {
                    return null;
                }
                PersonalInfoDto dto = getModel().getObject();
                return dto == null ? null : dto.getLastFailDate();
            }
        }, WebComponentUtil.getLongDateTimeFormat(getPageBase()));
        lastFailDate.setBeforeTextOnDateNull(getPageBase().getString("PersonalInfoPanel.never"));
        add(lastFailDate);

        Label lastFailFrom = new Label(ID_LAST_FAIL_FROM, new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (getModel() == null) {
                    return PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
                }
                PersonalInfoDto dto = getModel().getObject();

                return StringUtils.isNotEmpty(dto.getLastFailFrom()) ? dto.getLastFailFrom() :
                        PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
            }
        });
        add(lastFailFrom);

        Label passwordExp = new Label(ID_PASSWORD_EXP, new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (getModel() == null) {
                    return PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
                }
                PersonalInfoDto dto = getModel().getObject();

                return dto.getPasswordExp() != null ? WebComponentUtil.formatDate(dto.getPasswordExp()) :
                        PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
            }
        });
        add(passwordExp);
    }
}
