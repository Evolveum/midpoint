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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTreeDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableTreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.*;

/**
 * @author lazyman
 */
public class OrgTreeProvider extends SortableTreeProvider<OrgTreeDto, String> {

    private static final Trace LOGGER = TraceManager.getTrace(OrgTreeProvider.class);

    private static final String DOT_CLASS = OrgTreeProvider.class.getName() + ".";
    private static final String LOAD_ORG_UNIT = DOT_CLASS + "loadOrgUnit";
    private static final String LOAD_ORG_UNITS = DOT_CLASS + "loadOrgUnits";

    private Component component;
    private IModel<String> rootOid;
    private OrgTreeDto root;

    public OrgTreeProvider(Component component, IModel<String> rootOid) {
        this.component = component;
        this.rootOid = rootOid;
    }

    private PageBase getPageBase() {
        return WebMiscUtil.getPageBase(component);
    }

    private ModelService getModelService() {
        return getPageBase().getModelService();
    }

    @Override
    public Iterator<? extends OrgTreeDto> getChildren(OrgTreeDto node) {
        LOGGER.debug("Loading children for {}", new Object[]{node});
        Iterator<OrgTreeDto> iterator = null;

        OrgFilter orgFilter = OrgFilter.createOrg(node.getOid(), OrgFilter.Scope.ONE_LEVEL);
        ObjectQuery query = ObjectQuery.createObjectQuery(orgFilter);
        query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

        OperationResult result = new OperationResult(LOAD_ORG_UNITS);
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = WebModelUtils.createOptionsForParentOrgRefs();
            Task task = getPageBase().createSimpleTask(LOAD_ORG_UNITS);

            List<PrismObject<OrgType>> units = getModelService().searchObjects(OrgType.class, query, options,
                    task, result);
            LOGGER.debug("Found {} units.", units.size());

            List<OrgTreeDto> list = new ArrayList<OrgTreeDto>();
            for (PrismObject<OrgType> unit : units) {
                list.add(createDto(node, unit));
            }

            Collections.sort(list);
            iterator = list.iterator();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load children", ex);
            result.recordFatalError("Unable to load org unit", ex);
        } finally {
            result.computeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            getPageBase().showResultInSession(result);
            throw new RestartResponseException(PageOrgTree.class);
        }

        if (iterator == null) {
            iterator = new ArrayList<OrgTreeDto>().iterator();
        }

        LOGGER.debug("Finished loading children.");
        return iterator;
    }

    private OrgTreeDto createDto(OrgTreeDto parent, PrismObject<OrgType> unit) {
        if (unit == null) {
            return null;
        }

        String name = WebMiscUtil.getName(unit);
        String description = unit.getPropertyRealValue(OrgType.F_DESCRIPTION, String.class);
        String displayName = WebMiscUtil.getOrigStringFromPoly(
                unit.getPropertyRealValue(OrgType.F_DISPLAY_NAME, PolyString.class));
        String identifier = unit.getPropertyRealValue(OrgType.F_IDENTIFIER, String.class);

        //todo relation [lazyman]
        return new OrgTreeDto(parent, unit);
    }

    @Override
    public Iterator<? extends OrgTreeDto> getRoots() {
        OperationResult result = null;
        if (root == null) {
        	Task task = getPageBase().createSimpleTask(LOAD_ORG_UNIT);
            result = task.getResult();
            LOGGER.debug("Getting roots for: " + rootOid.getObject());

            PrismObject<OrgType> object = WebModelUtils.loadObject(OrgType.class, rootOid.getObject(),
                    WebModelUtils.createOptionsForParentOrgRefs(), getPageBase(), task, result);
            result.computeStatus();

            root = createDto(null, object);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\n{}", result.debugDump());
                LOGGER.debug("Finished roots loading.");
            }
        }

        if (WebMiscUtil.showResultInPage(result)) {
            getPageBase().showResultInSession(result);
            throw new RestartResponseException(PageUsers.class);
        }

        List<OrgTreeDto> list = new ArrayList<OrgTreeDto>();
        if (root != null) {
            list.add(root);
        }

        return list.iterator();
    }

    @Override
    public boolean hasChildren(OrgTreeDto node) {
        return true;
    }

    @Override
    public IModel<OrgTreeDto> model(OrgTreeDto object) {
        return new Model<>(object);
    }
}
