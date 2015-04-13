/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.users.component.TreeTablePanel;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/org/tree", action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_ORG_ALL,
                label = PageAdminUsers.AUTH_ORG_ALL_LABEL,
                description = PageAdminUsers.AUTH_ORG_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#orgTree",
                label = "PageOrgTree.auth.orgTree.label",
                description = "PageOrgTree.auth.orgTree.description")})
public class PageOrgTree extends PageAdminUsers {

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgTree.class);

    /**
     * todo what is this for? [lazyman]
     */
    public static final String PARAM_ORG_RETURN = "org";

    private static final String DOT_CLASS = PageOrgTree.class.getName() + ".";
    private static final String OPERATION_LOAD_ORG_UNIT = DOT_CLASS + "loadOrgUnit";

    private String ID_TABS = "tabs";


    public PageOrgTree() {
        initLayout();
    }
    
    private void initLayout() {
        final IModel<List<ITab>> tabModel = new LoadableModel<List<ITab>>(false) {

            @Override
            protected List<ITab> load() {
                LOGGER.debug("Loading org. roots for tabs for tabbed panel.");
                List<PrismObject<OrgType>> roots = loadOrgRoots();

                List<ITab> tabs = new ArrayList<>();
                for (PrismObject<OrgType> root : roots) {
                    final String oid = root.getOid();
                    tabs.add(new AbstractTab(createTabTitle(root)) {

                        @Override
                        public WebMarkupContainer getPanel(String panelId) {
                            return new TreeTablePanel(panelId, new Model(oid));
                        }
                    });
                }

                LOGGER.debug("Tab count is {}", new Object[]{tabs.size()});

                if (tabs.isEmpty()) {
                    getSession().warn(getString("PageOrgTree.message.noOrgStructDefined"));
                    throw new RestartResponseException(PageUsers.class);
                }

                return tabs;
            }
        };

        TabbedPanel tabbedPanel = new TabbedPanel(ID_TABS, tabModel, new Model<>(0));
        tabbedPanel.setOutputMarkupId(true);
        add(tabbedPanel);
    }

    private IModel<String> createTabTitle(final PrismObject<OrgType> org) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PolyString displayName = org.getPropertyRealValue(OrgType.F_DISPLAY_NAME, PolyString.class);
                if (displayName != null) {
                    return displayName.getOrig();
                }

                return WebMiscUtil.getName(org);
            }
        };
    }

    private List<PrismObject<OrgType>> loadOrgRoots() {
        Task task = createSimpleTask(OPERATION_LOAD_ORG_UNIT);
        OperationResult result = new OperationResult(OPERATION_LOAD_ORG_UNIT);

        List<PrismObject<OrgType>> list = new ArrayList<>();
        try {
            ObjectQuery query = ObjectQueryUtil.createRootOrgQuery(getPrismContext());
            list = getModelService().searchObjects(OrgType.class, query, null, task, result);

            if (list.isEmpty()) {
                warn(getString("PageOrgTree.message.noOrgStructDefined"));
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unable to load org. unit", ex);
            result.recordFatalError("Unable to load org unit", ex);
        } finally {
            result.computeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            showResult(result);
        }

        return list;
    }
}
