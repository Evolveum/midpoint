/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.util.*;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableTreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.model.SelectableObjectModel;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgTree;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author lazyman
 */
public class OrgTreeProvider extends SortableTreeProvider<TreeSelectableBean<OrgType>, String> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(OrgTreeProvider.class);

    private static final String DOT_CLASS = OrgTreeProvider.class.getName() + ".";
    private static final String LOAD_ORG_UNIT = DOT_CLASS + "loadOrgUnit";
    private static final String LOAD_ORG_UNITS = DOT_CLASS + "loadOrgUnits";

    private Component component;
    private IModel<String> rootOid;

    private long offset;
    private long count;

    private Map<String, TreeSelectableBean<OrgType>> availableData;

    public OrgTreeProvider(Component component, IModel<String> rootOid) {
        this.component = component;
        this.rootOid = rootOid;
    }

    private Map<String, TreeSelectableBean<OrgType>> getAvailableData() {
        if (availableData == null){
            availableData = new HashMap<>();
        }
        return availableData;
    }

    private PageBase getPageBase() {
        if (component instanceof PageBase) {
            return (PageBase) component;
        }
        if (component == null) {
            return null;
        }
        Page page = component.findParent(Page.class);
        if (page == null) {
            return null;
        }
        return WebComponentUtil.getPageBase(component);
    }

    private ModelService getModelService() {
        return getPageBase().getModelService();
    }

    /*
     *  Wicket calls getChildren twice: in order to get actual children data, but also to know their number.
     *  We'll cache the children to avoid duplicate processing.
     */
    private static final long EXPIRATION_AFTER_LAST_FETCH_OPERATION = 500L;
    private long lastFetchOperation = 0;
    private Map<String, List<TreeSelectableBean<OrgType>>> childrenCache = new HashMap<>();        // key is the node OID

    public long size(TreeSelectableBean<OrgType> node) {
        Task task = getPageBase().createSimpleTask(LOAD_ORG_UNITS);
        OperationResult result = task.getResult();

        String nodeOid = null;
        if (node != null) {
            nodeOid = node.getValue().getOid();
        } else {
            nodeOid = rootOid.getObject();
        }

        Integer orgs = null;
        try {
            ObjectQuery query = getPageBase().getPrismContext().queryFor(OrgType.class)
                    .isDirectChildOf(nodeOid)
                    .build();

            orgs = getModelService().countObjects(OrgType.class, query, null, task, result);

            LOGGER.debug("Found {} sub-orgs.", orgs);
        } catch (CommonException|RuntimeException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load children", ex);
            result.recordFatalError(getPageBase().createStringResource("OrgTreeProvider.message.getChildren.fatalError").getString(), ex);
        } finally {
            result.computeStatus();
        }
        if (WebComponentUtil.showResultInPage(result)) {
            getPageBase().showResult(result);
            throw new RestartResponseException(PageOrgTree.class);
        }

        return orgs == null ? 0 : orgs.longValue();
    }

    @Override
    public Iterator<? extends TreeSelectableBean<OrgType>> getChildren(TreeSelectableBean<OrgType> node) {
        LOGGER.debug("Getting children for {}", node.getValue());
        String nodeOid = node.getValue().getOid();
        List<TreeSelectableBean<OrgType>> children;

        long currentTime = System.currentTimeMillis();
        if (currentTime > lastFetchOperation + EXPIRATION_AFTER_LAST_FETCH_OPERATION) {
            childrenCache.clear();
        }

        if (childrenCache.containsKey(nodeOid)) {
            LOGGER.debug("Using cached children for {}", node.getValue());
            children = childrenCache.get(nodeOid);
        } else {
            LOGGER.debug("Loading fresh children for {}", node.getValue());
            OperationResult result = new OperationResult(LOAD_ORG_UNITS);
            try {
                ObjectQuery query = getPageBase().getPrismContext().queryFor(OrgType.class)
                        .isDirectChildOf(nodeOid)
                        .build();
                ObjectFilter customFilter = getCustomFilter();
                if (customFilter != null){
                    query.addFilter(customFilter);
                }
                Task task = getPageBase().createSimpleTask(LOAD_ORG_UNITS);
                    ObjectPaging paging = createPaging(node);
                    query.setPaging(paging);

                List<PrismObject<OrgType>> orgs = getModelService().searchObjects(OrgType.class, query, null, task, result);
                LOGGER.debug("Found {} sub-orgs.", orgs.size());
                children = new ArrayList<>();
                for (PrismObject<OrgType> org : orgs) {
                    children.add(createObjectWrapper(node, org, null));
                }
                childrenCache.put(nodeOid, children);
            } catch (CommonException|RuntimeException ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load children", ex);
                result.recordFatalError(getPageBase().createStringResource("OrgTreeProvider.message.getChildren.fatalError").getString(), ex);
                children = new ArrayList<>();
            } finally {
                result.computeStatus();
            }
            if (WebComponentUtil.showResultInPage(result)) {
                getPageBase().showResult(result);
                throw new RestartResponseException(PageOrgTree.class);
            }
            children.forEach(orgUnit -> {
                getAvailableData().putIfAbsent(orgUnit.getValue().getOid(), orgUnit);
            });
        }
        LOGGER.debug("Finished getting children.");
        lastFetchOperation = System.currentTimeMillis();
        return children.iterator();
    }

    private ObjectPaging createPaging(TreeSelectableBean<OrgType> node) {

        List<ObjectOrdering> orderings = new ArrayList<>();

        OrderDirection order = OrderDirection.ASCENDING;
        orderings.add(getPageBase().getPrismContext().queryFactory().createOrdering(
        ItemPath.create(OrgType.F_DISPLAY_NAME), order));
        orderings.add(getPageBase().getPrismContext().queryFactory().createOrdering(
        ItemPath.create(OrgType.F_NAME), order));

        Integer o = WebComponentUtil.safeLongToInteger(offset);
        Integer size = WebComponentUtil.safeLongToInteger(count);
        return getPageBase().getPrismContext().queryFactory().createPaging(o, size, orderings);
    }



    protected ObjectFilter getCustomFilter(){
        return null;
    }

    private TreeSelectableBean<OrgType> createObjectWrapper(TreeSelectableBean<OrgType> parent, PrismObject<OrgType> unit, Collection<SelectorOptions<GetOperationOptions>> options) {
        if (unit == null) {
            return null;
        }

        //todo relation [lazyman]
        OrgType org = unit.asObjectable();
        if (parent != null) {
            org.getParentOrgRef().clear();
            ObjectReferenceType parentOrgRef = new ObjectReferenceType();
            parentOrgRef.asReferenceValue().setObject(parent.getValue().asPrismObject());
            org.getParentOrgRef().add(parentOrgRef);
        }
        SelectableObjectModel<OrgType> orgModel = new SelectableObjectModel<OrgType>(unit.asObjectable(), options) {

            @Override
            protected OrgType load() {
                if (getPageBase() == null) {
                    return null;
                }
                Task task = getPageBase().createSimpleTask("Load org");
                OperationResult result = task.getResult();
                PrismObject<OrgType> org = WebModelServiceUtils.loadObject(getType(), getOid(), getOptions(), getPageBase(), task, result);
                if (org == null) {
                    return null;
                }

                OrgType orgType = org.asObjectable();
                return orgType;
            }
        };


        TreeSelectableBean<OrgType> orgDto = new TreeSelectableBean<>(orgModel);
        return orgDto;
    }


    @Override
    public Iterator<TreeSelectableBean<OrgType>> getRoots() {
        Task task = getPageBase().createSimpleTask(LOAD_ORG_UNIT);
        OperationResult result = task.getResult();
        LOGGER.debug("Getting roots for: " + rootOid.getObject());

        Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils.createOptionsForParentOrgRefs(getPageBase().getOperationOptionsBuilder());
        PrismObject<OrgType> object = WebModelServiceUtils.loadObject(OrgType.class, rootOid.getObject(),
                options, getPageBase(), task, result);
        result.computeStatus();

        TreeSelectableBean<OrgType> root = createObjectWrapper(null, object, options);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n{}", result.debugDump());
            LOGGER.debug("Finished roots loading.");
        }

        if (WebComponentUtil.showResultInPage(result)) {
            getPageBase().showResult(result);
        }

        List<TreeSelectableBean<OrgType>> list = new ArrayList<>();
        if (root != null) {
            list.add(root);
            if (!getAvailableData().containsKey(root.getValue().getOid())){
                getAvailableData().put(root.getValue().getOid(), root);
            }

        }
        return list.iterator();
    }

    @Override
    public boolean hasChildren(TreeSelectableBean<OrgType> node) {
        return true;
    }

    @Override
    public IModel<TreeSelectableBean<OrgType>> model(TreeSelectableBean<OrgType> object) {
        return new Model<>(object);
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public void detach() {
        getAvailableData().clear();
        super.detach();
    }
}
