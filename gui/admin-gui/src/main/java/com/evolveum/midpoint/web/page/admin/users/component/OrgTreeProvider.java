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
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.PageOrgStruct;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTreeDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
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

    public OrgTreeProvider(Component component, IModel<String> rootOid) {
        this.component = component;
        this.rootOid = rootOid;
    }

    private PageBase getPage() {
        return (PageBase) component.getPage();
    }

    private ModelService getModelService() {
        PageBase page = (PageBase) component.getPage();
        return page.getModelService();
    }

    @Override
    public Iterator<? extends OrgTreeDto> getChildren(OrgTreeDto node) {
        LOGGER.debug("Loading children for {}", new Object[]{node});
        Iterator<OrgTreeDto> iterator = null;

        OrgFilter orgFilter = OrgFilter.createOrg(node.getOid(), null, 1);
        ObjectQuery query = ObjectQuery.createObjectQuery(orgFilter);
        query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

        OperationResult result = new OperationResult(LOAD_ORG_UNITS);
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = createOptions();
            Task task = getPage().createSimpleTask(LOAD_ORG_UNITS);

            List<PrismObject<OrgType>> units = getModelService().searchObjects(OrgType.class, query, options,
                    task, result);
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
            getPage().showResultInSession(result);
            throw new RestartResponseException(PageOrgTree.class);
        }

        if (iterator == null) {
            iterator = new ArrayList<OrgTreeDto>().iterator();
        }

        LOGGER.debug("Finished loading children.");
        return iterator;
    }

    private Collection<SelectorOptions<GetOperationOptions>> createOptions() {
        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(ObjectType.F_PARENT_ORG_REF,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        return options;
    }

    private OrgTreeDto createDto(OrgTreeDto parent, PrismObject<OrgType> unit) {
        String name = WebMiscUtil.getName(unit);
        String description = unit.getPropertyRealValue(OrgType.F_DESCRIPTION, String.class);
        String displayName = WebMiscUtil.getOrigStringFromPoly(
                unit.getPropertyRealValue(OrgType.F_DISPLAY_NAME, PolyString.class));
        String identifier = unit.getPropertyRealValue(OrgType.F_IDENTIFIER, String.class);

        return new OrgTreeDto(parent, unit.getOid(), name, description, displayName, identifier);
    }

    @Override
    public Iterator<? extends OrgTreeDto> getRoots() {
        LOGGER.debug("Getting roots for: " + rootOid.getObject());

        OperationResult result = new OperationResult(LOAD_ORG_UNIT);

        OrgTreeDto node = null;
        try {
            Task task = getPage().createSimpleTask(LOAD_ORG_UNIT);
            PrismObject<OrgType> root = getModelService().getObject(OrgType.class, rootOid.getObject(),
                    createOptions(), task, result);
            node = createDto(null, root);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load roots", ex);
            result.recordFatalError("Unable to load org unit", ex);
        } finally {
            result.computeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            getPage().showResultInSession(result);
            throw new RestartResponseException(PageOrgTree.class);
        }


        Iterator<? extends OrgTreeDto> iterator = getChildren(node);
        LOGGER.debug("Finished roots loading.");

        return iterator;
    }

    @Override
    public boolean hasChildren(OrgTreeDto node) {
        return true;
    }

    @Override
    public IModel<OrgTreeDto> model(OrgTreeDto object) {
        return new Model<OrgTreeDto>(object);
    }
}
