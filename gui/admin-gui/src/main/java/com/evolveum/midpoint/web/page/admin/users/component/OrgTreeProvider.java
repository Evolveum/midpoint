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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableTreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
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
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author lazyman
 */
public class OrgTreeProvider extends SortableTreeProvider<SelectableBean<OrgType>, String> {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(OrgTreeProvider.class);

    private static final String DOT_CLASS = OrgTreeProvider.class.getName() + ".";
    private static final String LOAD_ORG_UNIT = DOT_CLASS + "loadOrgUnit";
    private static final String LOAD_ORG_UNITS = DOT_CLASS + "loadOrgUnits";

    private Component component;
    private IModel<String> rootOid;
    private SelectableBean<OrgType> root;
    
    private List<SelectableBean<OrgType>> availableData;

    public OrgTreeProvider(Component component, IModel<String> rootOid) {
        this.component = component;
        this.rootOid = rootOid;
    }

    public List<SelectableBean<OrgType>> getAvailableData() {
		if (availableData == null){
			availableData = new ArrayList<>();
		}
    	return availableData;
	}
    
    private PageBase getPageBase() {
        return WebComponentUtil.getPageBase(component);
    }

    private ModelService getModelService() {
        return getPageBase().getModelService();
    }
    
    

    @Override
    public Iterator<? extends SelectableBean<OrgType>> getChildren(SelectableBean<OrgType> node) {
//    	getAvailableData().clear();
        LOGGER.debug("Loading children for {}", new Object[]{node});
        Iterator<SelectableBean<OrgType>> iterator = null;

        ObjectQuery query = QueryBuilder.queryFor(ObjectType.class, getPageBase().getPrismContext())
                .isDirectChildOf(node.getValue().getOid())      // TODO what if getValue==null
                .asc(ObjectType.F_NAME)
                .build();
        OperationResult result = new OperationResult(LOAD_ORG_UNITS);
        try {
//            Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils.createOptionsForParentOrgRefs();
        	Collection<SelectorOptions<GetOperationOptions>> options = null;
            Task task = getPageBase().createSimpleTask(LOAD_ORG_UNITS);

            List<PrismObject<OrgType>> units = getModelService().searchObjects(OrgType.class, query, options,
                    task, result);
            LOGGER.debug("Found {} units.", units.size());

            List<SelectableBean<OrgType>> list = new ArrayList<SelectableBean<OrgType>>();
            for (PrismObject<OrgType> unit : units) {
                SelectableBean<OrgType> selectable =createObjectWrapper(node, unit);
            	list.add(selectable);
//                if (getAvailableData().contains(selectable)){
//                	getAvailableData().remove(selectable);
//                } 
//                getAvailableData().add(selectable);
            }
            
            getAvailableData().addAll(list);

//            Collections.sort(list);
            iterator = list.iterator();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load children", ex);
            result.recordFatalError("Unable to load org unit", ex);
        } finally {
            result.computeStatus();
        }

        if (WebComponentUtil.showResultInPage(result)) {
            getPageBase().showResult(result);
            throw new RestartResponseException(PageOrgTree.class);
        }

        if (iterator == null) {
            iterator = new ArrayList<SelectableBean<OrgType>>().iterator();
        }

        LOGGER.debug("Finished loading children.");
        return iterator;
    }

    private SelectableBean<OrgType> createObjectWrapper(SelectableBean<OrgType> parent, PrismObject<OrgType> unit) {
        if (unit == null) {
            return null;
        }

        //todo relation [lazyman]
//        OrgTreeDto orgDto = new OrgTreeDto(parent, unit);
        OrgType org = unit.asObjectable();
        if (parent != null){
        	org.getParentOrg().clear();
        org.getParentOrg().add(parent.getValue());
        }
        SelectableBean<OrgType> orgDto = new SelectableBean<OrgType>(org);
        orgDto.getMenuItems().addAll(createInlineMenuItems(orgDto.getValue()));
        return orgDto;
    }

    protected List<InlineMenuItem> createInlineMenuItems(OrgType org){
    	return null;
    }
    
    @Override
    public Iterator<SelectableBean<OrgType>> getRoots() {
        OperationResult result = null;
        if (root == null) {
        	Task task = getPageBase().createSimpleTask(LOAD_ORG_UNIT);
            result = task.getResult();
            LOGGER.debug("Getting roots for: " + rootOid.getObject());

            PrismObject<OrgType> object = WebModelServiceUtils.loadObject(OrgType.class, rootOid.getObject(),
                    WebModelServiceUtils.createOptionsForParentOrgRefs(), getPageBase(), task, result);
            result.computeStatus();

            root = createObjectWrapper(null, object);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\n{}", result.debugDump());
                LOGGER.debug("Finished roots loading.");
            }
        }

        if (WebComponentUtil.showResultInPage(result)) {
            getPageBase().showResult(result);
            throw new RestartResponseException(PageUsers.class);
        }

        List<SelectableBean<OrgType>> list = new ArrayList<SelectableBean<OrgType>>();
        if (root != null) {
            list.add(root);
            if (!getAvailableData().contains(root)){
            	getAvailableData().add(root);
            } 
            
        }
//        getAvailableData().addAll(list);

        return list.iterator();
    }

    @Override
    public boolean hasChildren(SelectableBean<OrgType> node) {
        return true;
    }

    @Override
    public IModel<SelectableBean<OrgType>> model(SelectableBean<OrgType> object) {
        return new Model<>(object);
    }
    
    public List<OrgType> getSelectedObjects(){
    	List<OrgType> selectedOrgs = new ArrayList<>();
    	for (SelectableBean<OrgType> selected : getAvailableData()){
    		if (selected.isSelected() && selected.getValue() != null) {
    			selectedOrgs.add(selected.getValue());
    		}
    	}
    	
    	return selectedOrgs;
    }
}
