/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.PersonalInfoDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.text.SimpleDateFormat;

/**
 * @author lazyman
 */
public class PersonalInfoPanel extends SimplePanel<PersonalInfoDto> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss");

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
        PasswordType password = credentials.getPassword();

        PersonalInfoDto dto = new PersonalInfoDto();
        if (password.getPreviousSuccessfulLogin() != null) {
            dto.setLastLoginDate(MiscUtil.asDate(password.getPreviousSuccessfulLogin().getTimestamp()));
            dto.setLastLoginFrom(password.getPreviousSuccessfulLogin().getFrom());
        }

        if (password.getLastFailedLogin() != null) {
            dto.setLastFailDate(MiscUtil.asDate(password.getLastFailedLogin().getTimestamp()));
            dto.setLastFailFrom(password.getLastFailedLogin().getFrom());
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

                return dto.getLastLoginDate() != null ? DATE_FORMAT.format(dto.getLastLoginDate()) :
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

                return dto.getLastFailDate() != null ? DATE_FORMAT.format(dto.getLastFailDate()) :
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

                return dto.getPasswordExp() != null ? DATE_FORMAT.format(dto.getPasswordExp()) :
                        PersonalInfoPanel.this.getString("PersonalInfoPanel.undefined");
            }
        });
        add(passwordExp);
    }
}
