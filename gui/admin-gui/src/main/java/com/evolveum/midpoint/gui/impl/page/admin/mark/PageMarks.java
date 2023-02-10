/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.mark;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

        MainObjectListPanel<MarkType> table = new MainObjectListPanel<>(ID_TABLE, MarkType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_MARKS_TABLE;
            }

            @Override
            protected IColumn<SelectableBean<MarkType>, String> createCustomExportableColumn(
                    IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {

                ItemPath path = WebComponentUtil.getPath(customColumn);
                if (ItemPath.create(MarkType.F_EVENT_MARK, EventMarkInformationType.F_DOMAIN).equivalent(path)) {
                    return createDomainColumn(displayModel);
                }

                return super.createCustomExportableColumn(displayModel, customColumn, expression);
            }

            private IColumn<SelectableBean<MarkType>, String> createDomainColumn(IModel<String> displayModel) {
                return new LambdaColumn<>(displayModel, selectableBean -> {

                    MarkType mark = selectableBean.getValue();
                    EventMarkInformationType info = mark.getEventMark();
                    if (info == null || info.getDomain() == null) {
                        return null;
                    }

                    EventMarkDomainType domain = info.getDomain();
                    if (domain.getSimulation() != null) {
                        return getString("EventMarkDomainType.simulation");
                    }

                    return null;
                });
            }
        };
        form.add(table);
    }
}
