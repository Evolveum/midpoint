/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.page.admin.home.*;

import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/dashboardNew", matchUrlForSecurity = "/self/dashboardNew")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
                        label = PageAdminHome.AUTH_HOME_ALL_LABEL,
                        description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                        label = "PageDashboard.auth.dashboard.label",
                        description = "PageDashboard.auth.dashboard.description")
        })
public class PageSelfDashboardConfigurable extends PageDashboardConfigurable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSelfDashboardConfigurable.class);

    private static final String ID_OBJECT_COLLECTION_VIEWS = "objectCollectionViews";
    private static final String ID_OBJECT_COLLECTION_VIEW = "objectCollectionView";

    protected void initLayout() {
        super.initLayout();
        initObjectCollectionViewsLayout();
    }

    private <C extends Containerable>  void initObjectCollectionViewsLayout() {
        add(new ListView<String>(ID_OBJECT_COLLECTION_VIEWS, new PropertyModel<>(dashboardModel, "objectCollectionViewIdentifier")) {
            @Override
            protected void populateItem(ListItem<String> item) {
                ContainerableListPanel listPanel = new ContainerableListPanel(ID_OBJECT_COLLECTION_VIEW, ObjectType.class) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return UserProfileStorage.TableId.PAGE_SELF_DASHBOARD_CONFIGURABLE;
                    }

                    @Override
                    public CompiledObjectCollectionView getObjectCollectionView() {
                        return getPageBase().getCompiledGuiProfile().findObjectCollectionView
                                (WebComponentUtil.containerClassToQName(getPageBase().getPrismContext(), getType()), item.getModelObject());
                    }

                    @Override
                    protected Containerable getRowRealValue(SelectableRow rowModelObject) {
                        return null;
                    }

                    @Override
                    protected IColumn createIconColumn() {
                        return null;
                    }

                    @Override
                    protected List<IColumn> createDefaultColumns() {
                        GuiObjectListViewType defaultView = DefaultColumnUtils.getDefaultView(getType());
                        if (defaultView == null) {
                            return null;
                        }
                        return getViewColumnsTransformed(defaultView.getColumn());
                    }

                    @Override
                    protected ISelectableDataProvider createProvider() {
                        SelectableBeanContainerDataProvider<C> provider
                                = new SelectableBeanContainerDataProvider<C>(
                                getPageBase(), getSearchModel(), null, true) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected PageStorage getPageStorage() {
                                return null;
                            }

                            @Override
                            protected ObjectQuery getCustomizeContentQuery() {
                                return null;
                            }

                            @Override
                            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                                return super.createObjectOrderings(sortParam);
                            }

                            @Override
                            protected Set<C> getSelected() {
                                return new HashSet<>();
                            }
                        };
                        provider.setCompiledObjectCollectionView(getObjectCollectionView());
//                        provider.setOptions(createOptions());
                        return provider;
                    }

                    @Override
                    public List getSelectedRealObjects() {
                        return null;
                    }
                };

                item.add(listPanel);
            }
        });
    }
}
