/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.mark;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/marks", matchUrlForSecurity = "/admin/marks")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MARKS_ALL_URL,
                        label = "PageAdminUsers.auth.marksAll.label",
                        description = "PageAdminUsers.auth.marksAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MARKS_URL,
                        label = "PageTags.auth.marks.label",
                        description = "PageTags.auth.marks.description")
        })
@CollectionInstance(identifier = "allTags", applicableForType = MarkType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.marks.list", singularLabel = "ObjectTypes.MARK", icon = GuiStyleConstants.CLASS_MARK))
public class PageMarks extends PageAdmin {

    private static final long serialVersionUID = 1L;

    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";

    public PageMarks() {
        this(new PageParameters());
    }

    public PageMarks(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        if (!isNativeRepo()) {
            warn(getString("PageMarks.nonNativeRepositoryWarning"));
        }

        Form form = new MidpointForm(ID_FORM);
        form.add(new VisibleBehaviour(() -> isNativeRepo()));
        add(form);

        MainObjectListPanel<MarkType> table = new MarkObjectListPanel(ID_TABLE);
        form.add(table);
    }
}
