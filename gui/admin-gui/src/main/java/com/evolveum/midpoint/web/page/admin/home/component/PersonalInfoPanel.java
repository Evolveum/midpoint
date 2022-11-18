/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.page.admin.home.dto.PersonalInfoDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationBehavioralDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

/**
 * @author lazyman
 */
public class PersonalInfoPanel extends BasePanel<PersonalInfoDto> {

    private static final String ID_LAST_LOGIN_DATE = "lastLoginDate";
    private static final String ID_LAST_LOGIN_FROM = "lastLoginFrom";
    private static final String ID_LAST_FAIL_DATE = "lastFailDate";
    private static final String ID_LAST_FAIL_FROM = "lastFailFrom";
    private static final String ID_PASSWORD_EXP = "passwordExp";

    private static final String DOT_CLASS = PersonalInfoPanel.class.getName() + ".";
    private static final String OPERATION_GET_CREDENTIALS_POLICY = DOT_CLASS + "getCredentialsPolicy";

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
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected PersonalInfoDto load() {
                return loadPersonalInfo();
            }
        };
    }

    private PersonalInfoDto loadPersonalInfo() {
        FocusType focus = AuthUtil.getPrincipalUser().getFocus();
        AuthenticationBehavioralDataType behaviour = focus.getBehavior() != null ? focus.getBehavior().getAuthentication() : null;
        PersonalInfoDto dto = new PersonalInfoDto();
        if (behaviour != null) {
            if (behaviour.getPreviousSuccessfulLogin() != null) {
                dto.setLastLoginDate(MiscUtil.asDate(behaviour.getPreviousSuccessfulLogin().getTimestamp()));
                dto.setLastLoginFrom(behaviour.getPreviousSuccessfulLogin().getFrom());
            }

            if (behaviour.getLastFailedLogin() != null) {
                dto.setLastFailDate(MiscUtil.asDate(behaviour.getLastFailedLogin().getTimestamp()));
                dto.setLastFailFrom(behaviour.getLastFailedLogin().getFrom());
            }
        }
        Task task = getPageBase().createSimpleTask(OPERATION_GET_CREDENTIALS_POLICY);
        CredentialsPolicyType credentialsPolicyType = WebComponentUtil.getPasswordCredentialsPolicy(focus.asPrismContainer(), getPageBase(), task);
        Duration maxAge = credentialsPolicyType != null && credentialsPolicyType.getPassword() != null ?
                credentialsPolicyType.getPassword().getMaxAge() : null;
        if (maxAge != null) {
            MetadataType credentialMetadata = focus.getCredentials() != null && focus.getCredentials().getPassword() != null ?
                    focus.getCredentials().getPassword().getMetadata() : null;
            XMLGregorianCalendar changeTimestamp = MiscSchemaUtil.getChangeTimestamp(credentialMetadata);
            if (changeTimestamp != null) {
                XMLGregorianCalendar passwordValidUntil = XmlTypeConverter.addDuration(changeTimestamp, maxAge);
                dto.setPasswordExp(MiscUtil.asDate(passwordValidUntil));
            }
        }
        return dto;
    }

    protected void initLayout() {
        DateLabelComponent lastLoginDate = new DateLabelComponent(ID_LAST_LOGIN_DATE, new IModel<>() {

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

        DateLabelComponent passwordExp = new DateLabelComponent(ID_PASSWORD_EXP, new IModel<Date>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Date getObject() {

                if (getModel() == null) {
                    return null;
                }
                PersonalInfoDto dto = getModel().getObject();
                return dto == null ? null : dto.getPasswordExp();
            }
        }, WebComponentUtil.getLongDateTimeFormat(getPageBase()));
        passwordExp.setBeforeTextOnDateNull(getPageBase().getString("PersonalInfoPanel.never"));
        add(passwordExp);
    }
}
