/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationPartType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;

public class InformationPanel extends BasePanel<InformationType> {

    private static final String ID_TITLE = "title";
    private static final String ID_PARTS = "parts";
    private static final String ID_PART = "part";

    public InformationPanel(String id, IModel<InformationType> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        Label titleLabel = new Label(ID_TITLE, () -> {
            InformationType info = getModelObject();
            if (info.getTitle() == null && info.getLocalizableTitle() == null) {
                return getString("ApprovalStageDefinitionType.additionalInformation");
            }

            return translate(info.getLocalizableTitle(), info.getTitle());
        });
        add(titleLabel);

        ListView<InformationPartType> list = new ListView<>(ID_PARTS, () -> getModelObject().getPart()) {

            @Override
            protected void populateItem(ListItem<InformationPartType> item) {
                Label label = new Label(ID_PART, () -> {
                    InformationPartType part = item.getModelObject();
                    return translate(part.getLocalizableText(), part.getText());
                });

                if (Boolean.TRUE.equals(item.getModelObject().isHasMarkup())) {
                    label.setEscapeModelStrings(false);
                }

                item.add(label);
            }
        };
        add(list);
    }

    private String translate(LocalizableMessageType msg, String defaultKey) {
        if (msg != null) {
            return LocalizationUtil.translateMessage(msg);
        }

        return LocalizationUtil.translate(defaultKey);
    }
}
