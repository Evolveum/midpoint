/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.data.*;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.self.PageAssignmentDetails;
import com.evolveum.midpoint.web.page.self.PageAssignmentShoppingKart;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class CatalogItemsPanel<P extends BaseSortableDataProvider> extends BasePanel<P> implements IPageableItems {
	private static final long serialVersionUID = 1L;

    private static final String ID_PAGING_FOOTER = "pagingFooter";
    private static final String ID_PAGING = "paging";
    private static final String ID_COUNT = "count";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_FOOTER = "footer";
    private static final String ID_ROW = "row";
    private static final String ID_CELL = "cell";
    private static final String ID_ITEM_BUTTON_CONTAINER = "itemButtonContainer";
    private static final String ID_INNER = "inner";
    private static final String ID_INNER_LABEL = "innerLabel";
    private static final String ID_INNER_DESCRIPTION = "innerDescription";
    private static final String ID_TYPE_ICON = "typeIcon";
    private static final String ID_ALREADY_ASSIGNED_ICON = "alreadyAssignedIcon";
    private static final String ID_ADD_TO_CART_LINK = "addToCartLink";
    private static final String ID_ADD_TO_CART_LINK_LABEL = "addToCartLinkLabel";
    private static final String ID_ADD_TO_CART_LINK_ICON = "addToCartLinkIcon";
    private static final String ID_DETAILS_LINK = "detailsLink";
    private static final String ID_DETAILS_LINK_LABEL = "detailsLinkLabel";
    private static final String ID_DETAILS_LINK_ICON = "detailsLinkIcon";

    private long itemsCount = 0;
    private boolean plusIconClicked = false;

    private int itemsPerRow = 4;
    private static final long DEFAULT_ROWS_COUNT = 5;
    private IModel<String> catalogOidModel;
    private long currentPage = 0;


    public CatalogItemsPanel(String id, IModel<P> providerModel, int itemsPerRow) {
        super(id, providerModel);
        this.catalogOidModel = Model.of(getSession().getSessionStorage().getRoleCatalog().getSelectedOid());
        if (itemsPerRow > 0){
            this.itemsPerRow = itemsPerRow;
        }
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        setCurrentPage(0);
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);
        initAssignmentItemsPanel();
        add(createFooter(ID_FOOTER));
    }

    private void initAssignmentItemsPanel(){
        itemsCount = getModel() != null ? (getModel().getObject() != null ? getModel().getObject().size() : 0) : 0;
        RepeatingView rows = new RepeatingView(ID_ROW);
        rows.setOutputMarkupId(true);
        if (itemsCount > 0 && itemsPerRow > 0){
            int index = 0;
            long rowCount = itemsCount % itemsPerRow == 0 ? (itemsCount / itemsPerRow) : (itemsCount / itemsPerRow + 1);
            for (int rowNumber = 0; rowNumber < rowCount; rowNumber++){
                WebMarkupContainer rowContainer = new WebMarkupContainer(rows.newChildId());
                rowContainer.setOutputMarkupId(true);
                rows.add(rowContainer);
                RepeatingView columns = new RepeatingView(ID_CELL);
                columns.setOutputMarkupId(true);
                rowContainer.add(columns);
                for (int colNumber = 0; colNumber < itemsPerRow; colNumber++){
                    WebMarkupContainer colContainer = new WebMarkupContainer(columns.newChildId());
                    colContainer.setOutputMarkupId(true);
                    columns.add(colContainer);

                    WebMarkupContainer itemButtonContainer = new WebMarkupContainer(ID_ITEM_BUTTON_CONTAINER);
                    itemButtonContainer.setOutputMarkupId(true);
                    itemButtonContainer.add(new AttributeAppender("class", getBackgroundClass((AssignmentEditorDto) getModelObject().getAvailableData().get(index))));
                    colContainer.add(itemButtonContainer);
                    populateCell(itemButtonContainer, new PropertyModel<AssignmentEditorDto>(getModelObject(), "availableData." + index));
                    index++;
                    if (index >= getModelObject().getAvailableData().size()){
                        break;
                    }

                }
            }
        }
        add(rows);
    }

    public void reloadProviderData() {
            if (getModelObject() != null) {
                if (getModelObject().getAvailableData() != null) {
                    getModelObject().getAvailableData().clear();
                }
                long from = currentPage * itemsPerRow * DEFAULT_ROWS_COUNT;
                getModelObject().internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
            }
    }

    protected WebMarkupContainer createFooter(String footerId) {
        PagingFooter footer = new PagingFooter(footerId, ID_PAGING_FOOTER, CatalogItemsPanel.this);
        footer.add(new VisibleEnableBehaviour(){
           @Override
            public boolean isVisible(){
               return !isCatalogOidEmpty() && getPageCount() > 1;
           }
        });
        return footer;
    }

    private static class PagingFooter extends Fragment {

        public PagingFooter(String id, String markupId, CatalogItemsPanel markupProvider) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);

            initLayout(markupProvider);
        }

        private void initLayout(final CatalogItemsPanel catalogItemsPanel) {
            WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
            footerContainer.setOutputMarkupId(true);

            final Label count = new Label(ID_COUNT, new AbstractReadOnlyModel<String>() {

                @Override
                public String getObject() {
                    return "";
                }
            });
            count.setOutputMarkupId(true);
            footerContainer.add(count);

            BoxedPagingPanel nb2 = new BoxedPagingPanel(ID_PAGING, catalogItemsPanel, true) {

                @Override
                protected void onPageChanged(AjaxRequestTarget target, long page) {
                    CatalogItemsPanel catalogPanel = PagingFooter.this.findParent(CatalogItemsPanel.class);
                    catalogPanel.reloadProviderData();
                    target.add(catalogPanel);
                    target.add(count);
                }
            };
            footerContainer.add(nb2);

            add(footerContainer);
        }
    }
    @Override
    public void setCurrentPage(long page) {
        currentPage = page;
        long from  = page * itemsPerRow * DEFAULT_ROWS_COUNT;
        if (getModelObject().getAvailableData() != null) {
            getModelObject().getAvailableData().clear();
        }
        getModelObject().internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
    }

    @Override
    public void setItemsPerPage(long page) {
    }

    @Override
    public long getCurrentPage() {
        return currentPage;
    }

    @Override
    public long getPageCount() {
        if (getModelObject() != null) {
            long itemsPerPage = getItemsPerPage();
            return itemsPerPage != 0 ? (getModelObject().getAvailableData().size() % itemsPerPage == 0 ? (getModelObject().size() / itemsPerPage) :
                    (getModelObject().getAvailableData().size() / itemsPerPage + 1)) : 0;
        }
        return 0;
    }

    @Override
    public long getItemsPerPage() {
        return DEFAULT_ROWS_COUNT * itemsPerRow;
    }

    @Override
    public long getItemCount() {
        return 0l;
    }

    private boolean isCatalogOidEmpty(){
        return AssignmentViewType.ROLE_CATALOG_VIEW.equals(getViewTypeFromSession()) &&
                (catalogOidModel == null || StringUtils.isEmpty(catalogOidModel.getObject()));
    }

    private AssignmentViewType getViewTypeFromSession(){
        return getSession().getSessionStorage().getRoleCatalog().getShoppingCartConfigurationDto().getDefaultViewType();
    }

    private IModel<String> getAlreadyAssignedIconTitleModel(AssignmentEditorDto dto) {
        return new LoadableModel<String>(false) {
            @Override
            protected String load() {
                List<RelationTypes> assignedRelations = dto.getAssignedRelationsList();
                String relations = "";
                if (assignedRelations != null && assignedRelations.size() > 0) {
                    relations = createStringResource("MultiButtonPanel.alreadyAssignedIconTitle").getString() + " ";
                    for (RelationTypes relation : assignedRelations) {
                        String relationName = createStringResource(relation).getString();
                        if (!relations.contains(relationName)) {
                            if (assignedRelations.indexOf(relation) > 0) {
                                relations = relations + ", ";
                            }
                            relations = relations + createStringResource(relation).getString();
                        }
                    }
                }
                return relations;
            }
        };
    }

    private boolean canAssign(final AssignmentEditorDto assignment) {
        return assignment.isAssignable();
    }

    private void assignmentDetailsPerformed(final AssignmentEditorDto assignment, AjaxRequestTarget target){
        if (!plusIconClicked) {
            assignment.setMinimized(false);
            assignment.setSimpleView(true);
            getPageBase().navigateToNext(new PageAssignmentDetails(Model.of(assignment)));
        } else {
            plusIconClicked = false;
        }
    }

    private void targetObjectDetailsPerformed(final AssignmentEditorDto assignment, AjaxRequestTarget target){
        if (assignment.getTargetRef() == null || assignment.getTargetRef().getOid() == null){
            return;
        }
        if (!plusIconClicked) {
            PageParameters parameters = new PageParameters();
            parameters.add(OnePageParameterEncoder.PARAMETER, assignment.getTargetRef().getOid());

            if (AssignmentEditorDtoType.ORG_UNIT.equals(assignment.getType())){
                getPageBase().navigateToNext(PageOrgUnit.class, parameters);
            } else if (AssignmentEditorDtoType.ROLE.equals(assignment.getType())){
                getPageBase().navigateToNext(PageRole.class, parameters);
            } else if (AssignmentEditorDtoType.SERVICE.equals(assignment.getType())){
                getPageBase().navigateToNext(PageService.class, parameters);
            }
        } else {
            plusIconClicked = false;
        }
    }

    private String getIconClass(AssignmentEditorDtoType type){
        // TODO: switch to icon constants
        if (AssignmentEditorDtoType.ROLE.equals(type)){
            return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
        }else if (AssignmentEditorDtoType.SERVICE.equals(type)){
            return GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON;
        }else if (AssignmentEditorDtoType.ORG_UNIT.equals(type)){
            return GuiStyleConstants.CLASS_OBJECT_ORG_ICON;
        } else {
            return "";
        }
    }

    private String getBackgroundClass(AssignmentEditorDto dto){
        if (!isMultiUserRequest() && !canAssign(dto)){
            return GuiStyleConstants.CLASS_DISABLED_OBJECT_ROLE_BG;
        } else if (AssignmentEditorDtoType.ROLE.equals(dto.getType())){
            return GuiStyleConstants.CLASS_OBJECT_ROLE_BG;
        }else if (AssignmentEditorDtoType.SERVICE.equals(dto.getType())){
            return GuiStyleConstants.CLASS_OBJECT_SERVICE_BG;
        }else if (AssignmentEditorDtoType.ORG_UNIT.equals(dto.getType())){
            return GuiStyleConstants.CLASS_OBJECT_ORG_BG;
        } else {
            return "";
        }
    }

    private void addAssignmentPerformed(AssignmentEditorDto assignment, AjaxRequestTarget target){
        plusIconClicked = true;
        RoleCatalogStorage storage = getPageBase().getSessionStorage().getRoleCatalog();
        if (storage.getAssignmentShoppingCart() == null){
            storage.setAssignmentShoppingCart(new ArrayList<AssignmentEditorDto>());
        }
        AssignmentEditorDto dto = assignment.clone();
        dto.setDefaultRelation();
        storage.getAssignmentShoppingCart().add(dto);
        //TODO refactor
        PageAssignmentShoppingKart parent = CatalogItemsPanel.this.findParent(PageAssignmentShoppingKart.class);
        parent.reloadCartButton(target);

    }

    private boolean isMultiUserRequest(){
        return getPageBase().getSessionStorage().getRoleCatalog().isMultiUserRequest();
    }

    private void populateCell(WebMarkupContainer cellContainer, final PropertyModel<AssignmentEditorDto> assignmentModel){
        AjaxLink inner = new AjaxLink(ID_INNER) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                targetObjectDetailsPerformed(assignmentModel.getObject(), ajaxRequestTarget);
            }
        };
        inner.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(assignmentModel.getObject());
            }
        });
        inner.add(new AttributeAppender("title", assignmentModel.getObject().getName()));
        cellContainer.add(inner);

        Label nameLabel = new Label(ID_INNER_LABEL, assignmentModel.getObject().getName());
        inner.add(nameLabel);

        Label descriptionLabel = new Label(ID_INNER_DESCRIPTION, assignmentModel.getObject().getTargetRef() != null ?
                assignmentModel.getObject().getTargetRef().getDescription() : "");
        descriptionLabel.setOutputMarkupId(true);
        inner.add(descriptionLabel);

        AjaxLink detailsLink = new AjaxLink(ID_DETAILS_LINK) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                assignmentDetailsPerformed(assignmentModel.getObject(), ajaxRequestTarget);
            }
        };
        detailsLink.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(assignmentModel.getObject());
            }
        });
        cellContainer.add(detailsLink);

        Label detailsLinkLabel = new Label(ID_DETAILS_LINK_LABEL, createStringResource("MultiButtonPanel.detailsLink"));
        detailsLinkLabel.setRenderBodyOnly(true);
        detailsLink.add(detailsLinkLabel);

        AjaxLink detailsLinkIcon = new AjaxLink(ID_DETAILS_LINK_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
            }

        };
        detailsLinkIcon.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(assignmentModel.getObject());
            }
        });
        detailsLink.add(detailsLinkIcon);

        AjaxLink addToCartLink = new AjaxLink(ID_ADD_TO_CART_LINK) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addAssignmentPerformed(assignmentModel.getObject(), ajaxRequestTarget);
            }
        };
        addToCartLink.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(assignmentModel.getObject());
            }
        });
        cellContainer.add(addToCartLink);

        AjaxLink addToCartLinkIcon = new AjaxLink(ID_ADD_TO_CART_LINK_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
            }

        };
        addToCartLinkIcon.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(assignmentModel.getObject());
            }
        });
        addToCartLink.add(addToCartLinkIcon);

        WebMarkupContainer icon = new WebMarkupContainer(ID_TYPE_ICON);
        icon.add(new AttributeAppender("class", getIconClass(assignmentModel.getObject().getType())));
        cellContainer.add(icon);

        WebMarkupContainer alreadyAssignedIcon = new WebMarkupContainer(ID_ALREADY_ASSIGNED_ICON);
        alreadyAssignedIcon.add(new AttributeAppender("title", getAlreadyAssignedIconTitleModel(assignmentModel.getObject())));
        alreadyAssignedIcon.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return !isMultiUserRequest() && assignmentModel.getObject().isAlreadyAssigned();
            }
        });
        cellContainer.add(alreadyAssignedIcon);

    }

}
