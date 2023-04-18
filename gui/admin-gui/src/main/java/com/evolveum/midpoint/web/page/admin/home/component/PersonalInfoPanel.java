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
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lazyman
 */
public class PersonalInfoPanel extends BasePanel<List<PersonalInfoDto>> {

    private static final String ID_AUTHENTICATION_BEHAVIORS = "authenticationBehaviors";
    private static final String ID_AUTHENTICATION_BEHAVIOR = "authenticationBehavior";
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
    public IModel<List<PersonalInfoDto>> createModel() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<PersonalInfoDto> load() {
                return loadPersonalInfo();
            }
        };
    }

    private List<PersonalInfoDto> loadPersonalInfo() {
        FocusType focus = AuthUtil.getPrincipalUser().getFocus();
        if (focus.getBehavior() == null) {
            return Collections.emptyList();
        }
        return focus.getBehavior().getAuthentication().stream()
                .map(auth -> createPersonalInfo(auth, focus))
                .collect(Collectors.toList());
    }

    private PersonalInfoDto createPersonalInfo(AuthenticationBehavioralDataType behaviour, FocusType focus) {
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

            dto.setSequenceIdentifier(behaviour.getSequenceIdentifier());
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
        ListView<PersonalInfoDto> authenticationInfos = new ListView<PersonalInfoDto>(ID_AUTHENTICATION_BEHAVIORS, createModel()) {
            @Override
            protected void populateItem(ListItem<PersonalInfoDto> listItem) {
                listItem.add(new AuthenticationInfoPanel(ID_AUTHENTICATION_BEHAVIOR, listItem.getModel()));
            }
        };
        add(authenticationInfos);
    }
}
