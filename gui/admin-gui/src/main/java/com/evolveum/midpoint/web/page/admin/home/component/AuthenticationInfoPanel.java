/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.home.dto.PersonalInfoDto;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.Date;

public class AuthenticationInfoPanel extends BasePanel<PersonalInfoDto> {

    private static final String ID_PANEL_HEADER = "panelHeader";
    private static final String ID_LAST_LOGIN_DATE = "lastLoginDate";
    private static final String ID_LAST_LOGIN_FROM = "lastLoginFrom";
    private static final String ID_LAST_FAIL_DATE = "lastFailDate";
    private static final String ID_LAST_FAIL_FROM = "lastFailFrom";
    private static final String ID_PASSWORD_EXP = "passwordExp";

    public AuthenticationInfoPanel(String id, IModel<PersonalInfoDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label headerLabel = new Label(ID_PANEL_HEADER, createHeaderLabelModel());
        headerLabel.add(new VisibleBehaviour(this::sequenceIdentifierNotEmpty));
        headerLabel.setOutputMarkupId(true);
        add(headerLabel);

        DateLabelComponent lastLoginDate = new DateLabelComponent(ID_LAST_LOGIN_DATE, new IModel<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Date getObject() {

                if (getModel() == null) {
                    return null;
                }
                PersonalInfoDto dto = getModel().getObject();
                return dto == null ? null : dto.getLastLoginDate();
            }
        }, WebComponentUtil.getLongDateTimeFormat(getPageBase()));
        lastLoginDate.setTextOnDateNull(getPageBase().getString("PersonalInfoPanel.never"));
        add(lastLoginDate);

        Label lastLoginFrom = new Label(ID_LAST_LOGIN_FROM, new IModel<String>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (getModel() == null) {
                    return AuthenticationInfoPanel.this.getString("PersonalInfoPanel.undefined");
                }
                PersonalInfoDto dto = getModel().getObject();

                return StringUtils.isNotEmpty(dto.getLastLoginFrom()) ? dto.getLastLoginFrom() :
                        AuthenticationInfoPanel.this.getString("PersonalInfoPanel.undefined");
            }
        });
        add(lastLoginFrom);

        DateLabelComponent lastFailDate = new DateLabelComponent(ID_LAST_FAIL_DATE, new IModel<Date>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Date getObject() {
                if (getModel() == null) {
                    return null;
                }
                PersonalInfoDto dto = getModel().getObject();
                return dto == null ? null : dto.getLastFailDate();
            }
        }, WebComponentUtil.getLongDateTimeFormat(getPageBase()));
        lastFailDate.setTextOnDateNull(getPageBase().getString("PersonalInfoPanel.never"));
        add(lastFailDate);

        Label lastFailFrom = new Label(ID_LAST_FAIL_FROM, new IModel<String>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (getModel() == null) {
                    return AuthenticationInfoPanel.this.getString("PersonalInfoPanel.undefined");
                }
                PersonalInfoDto dto = getModel().getObject();

                return StringUtils.isNotEmpty(dto.getLastFailFrom()) ? dto.getLastFailFrom() :
                        AuthenticationInfoPanel.this.getString("PersonalInfoPanel.undefined");
            }
        });
        add(lastFailFrom);

        DateLabelComponent passwordExp = new DateLabelComponent(ID_PASSWORD_EXP, new IModel<Date>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Date getObject() {

                if (getModel() == null) {
                    return null;
                }
                PersonalInfoDto dto = getModel().getObject();
                return dto == null ? null : dto.getPasswordExp();
            }
        }, WebComponentUtil.getLongDateTimeFormat(getPageBase()));
        passwordExp.setTextOnDateNull(getPageBase().getString("PersonalInfoPanel.never"));
        add(passwordExp);
    }

    private IModel<String> createHeaderLabelModel() {
        return createStringResource("AuthenticationInfoPanel.panelHeader", getModelObject().getSequenceIdentifier());
    }

    private boolean sequenceIdentifierNotEmpty() {
        return StringUtils.isNotEmpty(getModelObject().getSequenceIdentifier());
    }
}
