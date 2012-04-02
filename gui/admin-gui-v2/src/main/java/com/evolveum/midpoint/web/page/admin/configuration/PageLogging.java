/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.menu.left.LeftMenuItem;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageLogging extends PageAdmin {

    public PageLogging() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);
        DropDownChoice<LoggingLevelType> rootLevel =
                new DropDownChoice<LoggingLevelType>("rootLevel", createLoggingLevelModel());
        mainForm.add(rootLevel);

        DropDownChoice<LoggingLevelType> midPointLevel =
                new DropDownChoice<LoggingLevelType>("midPointLevel", createLoggingLevelModel());
        mainForm.add(midPointLevel);

        Accordion accordion = new Accordion("accordion");
        accordion.setMultipleSelect(true);
        accordion.setOpenedPanel(0);
        mainForm.add(accordion);

        AccordionItem loggers = new AccordionItem("loggers", createStringResource("pageLogging.loggers"));
        accordion.add(loggers);

        AccordionItem appenders = new AccordionItem("appenders", createStringResource("pageLogging.appenders"));
        accordion.add(appenders);
    }

    private IModel<List<LoggingLevelType>> createLoggingLevelModel() {
        return new AbstractReadOnlyModel<List<LoggingLevelType>>() {

            @Override
            public List<LoggingLevelType> getObject() {
                List<LoggingLevelType> list = new ArrayList<LoggingLevelType>();
                for (LoggingLevelType type : LoggingLevelType.values()) {
                    list.add(type);
                }

                return list;
            }
        };
    }

    private StringResourceModel createStringResource(String resourceKey) {
        return new StringResourceModel(resourceKey, this, null, null, null);
    }

    @Override
    public List<LeftMenuItem> getLeftMenuItems() {
        return new ArrayList<LeftMenuItem>();
    }

}
