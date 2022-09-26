/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.user.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author honchar
 */
@PanelType(name = "personas")
@PanelInstance(identifier = "personas",
        applicableForType = UserType.class,
        display = @PanelDisplay(label = "pageAdminFocus.personas", icon = GuiStyleConstants.CLASS_SHADOW_ICON_ENTITLEMENT, order = 80))
@Counter(provider = PersonasCounter.class)
public class UserPersonasPanel extends AbstractObjectMainPanel<UserType, UserDetailsModel> {
    private static final long serialVersionUID = 1L;
    private static final String DOT_CLASS = UserPersonasPanel.class.getName() + ".";
    private static final String OPERATION_SEARCH_PERSONAS_OBJECTS = DOT_CLASS + "searchPersonas";

    private static final String ID_PERSONAS_TABLE = "personasTable";

    public UserPersonasPanel(String id, UserDetailsModel focusDetailsModels, ContainerPanelConfigurationType config) {
        super(id, focusDetailsModels, config);
    }

    protected void initLayout() {
        MainObjectListPanel<UserType> userListPanel =
                new MainObjectListPanel<UserType>(ID_PERSONAS_TABLE, UserType.class, null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IColumn<SelectableBean<UserType>, String> createCheckboxColumn(){
                return null;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(new ButtonInlineMenuItem(createStringResource("AssignmentPanel.viewTargetObject")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public CompositedIconBuilder getIconCompositedBuilder() {
                        return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_NAVIGATE_ARROW);
                    }

                    @Override
                    public InlineMenuItemAction initAction() {
                        return new ColumnMenuAction<SelectableBeanImpl<UserType>>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                SelectableBean<UserType> personaRefSelectableBean = getRowModel().getObject();
                                UserType personaRefObj = personaRefSelectableBean.getValue();
                                ObjectReferenceType ort = new ObjectReferenceType();
                                ort.setOid(personaRefObj.getOid());
                                ort.setType(WebComponentUtil.classToQName(UserPersonasPanel.this.getPrismContext(), personaRefObj.getClass()));
                                WebComponentUtil.dispatchToObjectDetailsPage(ort, UserPersonasPanel.this, false);
                            }
                        };
                    }

                    @Override
                    public boolean isHeaderMenuItem(){
                        return false;
                    }
                });
                return menuItems;          }

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, UserType object) {
            }

            @Override
            protected boolean isCreateNewObjectEnabled() {
                        return false;
                    }

                    @Override
                    protected ISelectableDataProvider<SelectableBean<UserType>> createProvider() {
                        return createSelectableBeanObjectDataProvider(() -> getFocusPersonasQuery(), null);
                    }

//                    @Override
//            protected ObjectQuery getCustomizeContentQuery() {
//
//            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<UserType>> rowModel) {
                return false;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected boolean enableSavePageSize() {
                        return false;
                    }
        };
        userListPanel.setOutputMarkupId(true);
        add(userListPanel);
    }

    private ObjectQuery getFocusPersonasQuery() {
        List<String> personaOidsList = getPersonasOidsList();
        ObjectQuery query = getPageBase().getPrismContext().queryFor(FocusType.class)
                .id(personaOidsList.toArray(new String[0]))
                .build();
        return query;
    }

    private LoadableModel<List<PrismObject<FocusType>>> loadModel(){
        return new LoadableModel<List<PrismObject<FocusType>>>(false) {
            @Override
            protected List<PrismObject<FocusType>> load() {
                List<String> personaOidsList = getPersonasOidsList();
                List<PrismObject<FocusType>> personasList = new ArrayList<>();
                if (personaOidsList.size() > 0){
                    QueryFactory factory = getPrismContext().queryFactory();
                    ObjectQuery query = factory.createQuery(factory.createInOid(personaOidsList));
                    OperationResult result = new OperationResult(OPERATION_SEARCH_PERSONAS_OBJECTS);
                    personasList = WebModelServiceUtils.searchObjects(FocusType.class, query, result, getPageBase());
                }
                return personasList;
            }
        };
    }

    private List<String> getPersonasOidsList(){
        List<ObjectReferenceType> personasRefList = getObjectWrapper().getObject().asObjectable().getPersonaRef();
        List<String> oidsList = new ArrayList<>();
        if (personasRefList != null){
            for (ObjectReferenceType ref : personasRefList){
                oidsList.add(ref.getOid());
            }
        }
        return oidsList;
    }
}
