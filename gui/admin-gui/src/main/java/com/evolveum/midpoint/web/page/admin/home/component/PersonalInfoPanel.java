/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.PersonalInfoDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class PersonalInfoPanel extends SimplePanel<PersonalInfoDto> {

    private static final String ID_LAST_LOGIN_DATE = "lastLoginDate";
    private static final String ID_LAST_LOGIN_FROM = "lastLoginFrom";
    private static final String ID_LAST_FAIL_DATE = "lastFailDate";
    private static final String ID_LAST_FAIL_FROM = "lastFailFrom";
    private static final String ID_PASSWORD_EXP = "passwordExp";


    public PersonalInfoPanel(String id) {
        super(id);
    }

    @Override
    public IModel<PersonalInfoDto> createModel() {
        return new LoadableModel<PersonalInfoDto>(false) {

            @Override
            protected PersonalInfoDto load() {
                return loadPersonalInfo();
            }
        };
    }

    private PersonalInfoDto loadPersonalInfo() {
        UserType user = SecurityUtils.getPrincipalUser().getUser();
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

    @Override
    protected void initLayout() {
        Label lastLoginDate = new Label(ID_LAST_LOGIN_DATE, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PersonalInfoDto dto = getModel().getObject();

                return dto.getLastLoginDate() != null ? WebMiscUtil.formatDate(dto.getLastLoginDate()) :
                        PersonalInfoPanel.this.getString("PersonalInfoPanel.never");
            }
        });
        add(lastLoginDate);

        Label lastLoginFrom = new Label(ID_LAST_LOGIN_FROM, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PersonalInfoDto dto = getModel().getObject();

                return StringUtils.isNotEmpty(dto.getLastLoginFrom()) ? dto.getLastLoginFrom() :
                        PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
            }
        });
        add(lastLoginFrom);

        Label lastFailDate = new Label(ID_LAST_FAIL_DATE, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PersonalInfoDto dto = getModel().getObject();

                return dto.getLastFailDate() != null ? WebMiscUtil.formatDate(dto.getLastFailDate()) :
                        PersonalInfoPanel.this.getString("PersonalInfoPanel.never");
            }
        });
        add(lastFailDate);

        Label lastFailFrom = new Label(ID_LAST_FAIL_FROM, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PersonalInfoDto dto = getModel().getObject();

                return StringUtils.isNotEmpty(dto.getLastFailFrom()) ? dto.getLastFailFrom() :
                        PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
            }
        });
        add(lastFailFrom);

        Label passwordExp = new Label(ID_PASSWORD_EXP, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PersonalInfoDto dto = getModel().getObject();

                return dto.getPasswordExp() != null ? WebMiscUtil.formatDate(dto.getPasswordExp()) :
                        PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
            }
        });
        add(passwordExp);
    }
}
