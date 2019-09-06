/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.web.page.admin;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import java.util.*;

/**
 * Created by honchar
 */
public abstract class PageAdminObjectList<O extends ObjectType> extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAdminObjectList.class);

    private static final String DOT_CLASS = PageAdminObjectList.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";


    public PageAdminObjectList() {
        this(null);
    }

    public PageAdminObjectList(PageParameters parameters) {
        super(parameters);
        initLayout();
    }
    
    protected void initLayout() {
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        initTable(mainForm);
    }

    private void initTable(Form mainForm) {
        StringValue collectionNameParameter = getCollectionNameParameterValue();
        MainObjectListPanel<O, CompiledObjectCollectionView> userListPanel = new  MainObjectListPanel<O, CompiledObjectCollectionView>(ID_TABLE,
                getType(), !isCollectionViewPage() ?
                getTableId() : UserProfileStorage.TableId.COLLECTION_VIEW_TABLE, getQueryOptions(), this) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<IColumn<SelectableBean<O>, String>> createColumns() {
                return PageAdminObjectList.this.initColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, O object) {
                PageAdminObjectList.this.objectDetailsPerformed(target, object);
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, CompiledObjectCollectionView collectionView) {
                newObjectActionPerformed(target, collectionView);
            }

            @Override
            protected boolean isCreateNewObjectEnabled(){
                return PageAdminObjectList.this.isCreateNewObjectEnabled();
            }

            @Override
            protected List<CompiledObjectCollectionView> getNewObjectInfluencesList(){
                if (isCollectionViewPage()){
                    return new ArrayList<>();
                }
                return getCompiledUserProfile().findAllApplicableArchetypeViews(ObjectTypes.getObjectType(getType()).getTypeQName());
            }

            @Override
            protected DisplayType getNewObjectButtonStandardDisplayType(){
                if (!isCollectionViewPage()){
                    return super.getNewObjectButtonStandardDisplayType();
                }

                CompiledObjectCollectionView view = getCollectionViewObject();
                if (view.getCollection() != null && view.getCollection().getCollectionRef() != null &&
                        ArchetypeType.COMPLEX_TYPE.equals(view.getCollection().getCollectionRef().getType())){
                    return WebComponentUtil.getNewObjectDisplayTypeFromCollectionView(getCollectionViewObject(), PageAdminObjectList.this);
                }
                return super.getNewObjectButtonStandardDisplayType();
            }

            @Override
            protected Map<IconCssStyle, IconType> getNewObjectButtonLayerIconStyleMap(){
                if (!isCollectionViewPage()){
                    return null;
                }
                Map<IconCssStyle, IconType> layerIconMap = new HashMap<>();
                layerIconMap.put(IconCssStyle.BOTTOM_RIGHT_STYLE, WebComponentUtil.createIconType(GuiStyleConstants.CLASS_PLUS_CIRCLE, "green"));
                return layerIconMap;
            }

            @Override
            protected DisplayType getNewObjectButtonAdditionalDisplayType(CompiledObjectCollectionView collectionView){
                return WebComponentUtil.getNewObjectDisplayTypeFromCollectionView(collectionView, PageAdminObjectList.this);
            }

            @Override
            protected ObjectQuery createContentQuery() {
                ObjectQuery contentQuery = super.createContentQuery();
                ObjectFilter usersViewFilter = getArchetypeViewFilter();
                if (usersViewFilter != null){
                    if (contentQuery == null) {
                        contentQuery = PageAdminObjectList.this.getPrismContext().queryFactory().createQuery();
                    }
                    contentQuery.addFilter(usersViewFilter);
                }
                return contentQuery;
            }

            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
                return PageAdminObjectList.this.addCustomFilterToContentQuery(query);
            }

            @Override
            protected boolean isClickable(IModel<SelectableBean<O>> rowModel) {
                return isNameColumnClickable(rowModel);
            }

            @Override
            protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
                return PageAdminObjectList.this.createCustomOrdering(sortParam);
            }

            @Override
            protected String getTableIdKeyValue(){
                return !isCollectionViewPage() ?
                        super.getTableIdKeyValue() : super.getTableIdKeyValue() + "." + collectionNameParameter.toString();
            }

            @Override
            protected String getStorageKey() {
                StringValue collectionName = getCollectionNameParameterValue();
                String collectionNameValue = collectionName != null ? collectionName.toString() : "";
                String key = isCollectionViewPage() ? WebComponentUtil.getObjectListPageStorageKey(collectionNameValue) :
                        super.getStorageKey();
//WebComponentUtil.getObjectListPageStorageKey(getType().getSimpleName()
                return key;
            }
        };

        userListPanel.setAdditionalBoxCssClasses(WebComponentUtil.getBoxCssClasses(WebComponentUtil.classToQName(getPrismContext(), getType())));
        userListPanel.setOutputMarkupId(true);
        mainForm.add(userListPanel);
    }

    protected abstract Class<O> getType();

    protected abstract List<IColumn<SelectableBean<O>, String>> initColumns();

    protected abstract List<InlineMenuItem> createRowActions();

    protected void objectDetailsPerformed(AjaxRequestTarget target, O object){}

    protected boolean isCreateNewObjectEnabled(){
        return true;
    }

    protected void newObjectActionPerformed(AjaxRequestTarget target, CompiledObjectCollectionView collectionView){
        if (collectionView == null){
            collectionView = getCollectionViewObject();
        }
        ObjectReferenceType collectionViewReference = collectionView != null && collectionView.getCollection() != null ?
                collectionView.getCollection().getCollectionRef() : null;
        try {
            WebComponentUtil.initNewObjectWithReference(PageAdminObjectList.this,
                    WebComponentUtil.classToQName(getPrismContext(), getType()),
                    collectionViewReference != null && ArchetypeType.COMPLEX_TYPE.equals(collectionViewReference.getType()) ?
                            Arrays.asList(collectionViewReference) : null);
        } catch (SchemaException ex){
            getFeedbackPanel().getFeedbackMessages().error(PageAdminObjectList.this, ex.getUserFriendlyMessage());
            target.add(getFeedbackPanel());
        }
    }

    protected ObjectFilter getArchetypeViewFilter(){
        CompiledObjectCollectionView view = getCollectionViewObject();
        return view != null ? view.getFilter() : null;
    }

    protected CompiledObjectCollectionView getCollectionViewObject(){
        if (!isCollectionViewPage()) {
            return null;
        }
        String collectionName = getCollectionNameParameterValue().toString();
        return getCompiledUserProfile().findObjectViewByViewName(getType(), collectionName);
    }

    protected StringValue getCollectionNameParameterValue(){
        PageParameters parameters = getPageParameters();
        return parameters ==  null ? null : parameters.get(PARAMETER_OBJECT_COLLECTION_NAME);
    }

    protected ObjectQuery addCustomFilterToContentQuery(ObjectQuery query){
        return query;
    }

    protected boolean isNameColumnClickable(IModel<SelectableBean<O>> rowModel) {
        return true;
    }

    protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
        return null;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getQueryOptions(){
        return null;
    }

    protected abstract UserProfileStorage.TableId getTableId();

    protected Form getMainForm(){
        return (Form) get(ID_MAIN_FORM);
    }

    public MainObjectListPanel<O, CompiledObjectCollectionView> getObjectListPanel() {
        return (MainObjectListPanel<O, CompiledObjectCollectionView>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private boolean isCollectionViewPage(){
        StringValue collectionNameParam = getCollectionNameParameterValue();
        return collectionNameParam != null && !collectionNameParam.isEmpty() && !collectionNameParam.toString().equals("null");
    }
}
