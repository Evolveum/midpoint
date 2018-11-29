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

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItemPanel<T extends Serializable> extends BasePanel<SearchItem<T>> {

	private static final long serialVersionUID = 1L;
	
    private static final Trace LOG = TraceManager.getTrace(SearchItemPanel.class);

    private static final String ID_MAIN_BUTTON = "mainButton";
    private static final String ID_LABEL = "label";
    private static final String ID_DELETE_BUTTON = "deleteButton";
    private static final String ID_POPOVER = "popover";
    private static final String ID_POPOVER_BODY = "popoverBody";
    private static final String ID_UPDATE = "update";
    private static final String ID_CLOSE = "close";
    private static final String ID_VALUES = "values";
    private static final String ID_VALUE = "value";

    private LoadableModel<SearchItemPopoverDto<T>> popoverModel;

    public SearchItemPanel(String id, IModel<SearchItem<T>> model) {
        super(id, model);
        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        SearchItem<T> item = getModelObject();
        if (!item.isEditWhenVisible()) {
            return;
        }

        item.setEditWhenVisible(false);

        //todo show popover for this item somehow [lazyman]
    }

    private void initLayout() {
        popoverModel = new LoadableModel<SearchItemPopoverDto<T>>(false) {

        	private static final long serialVersionUID = 1L;

			@Override
            protected SearchItemPopoverDto<T> load() {
                return loadPopoverItems();
            }
        };

        AjaxLink<Void> mainButton = new AjaxLink<Void>(ID_MAIN_BUTTON) {

        	private static final long serialVersionUID = 1L;
        	
            @Override
            public void onClick(AjaxRequestTarget target) {
                editPerformed(target);
            }
        };
        add(mainButton);

        Label label = new Label(ID_LABEL, createLabelModel());
        label.setRenderBodyOnly(true);
        mainButton.add(label);

        AjaxLink<Void> deleteButton = new AjaxLink<Void>(ID_DELETE_BUTTON) {

           private static final long serialVersionUID = 1L;

			@Override
            public void onClick(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        mainButton.add(deleteButton);
        deleteButton.add(new VisibleEnableBehaviour() {
        	
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !getModelObject().isFixed();
            }
        });

        initPopover();
    }

    private SearchItemPopoverDto<T> loadPopoverItems() {
        SearchItemPopoverDto<T> dto = new SearchItemPopoverDto<>();

        SearchItem<T> item = getModelObject();
        for (DisplayableValue<T> value : item.getValues()) {
        	//TODO : what if null reference
            DisplayableValue<T> itemValue = new SearchValue<T>(value.getValue(), value.getLabel());
            dto.getValues().add(itemValue);
        }

        if (dto.getValues().isEmpty()) {
            dto.getValues().add(new SearchValue<>());
        }

        return dto;
    }

    private void initPopover() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        WebMarkupContainer popoverBody = new WebMarkupContainer(ID_POPOVER_BODY);
        popoverBody.setOutputMarkupId(true);
        popover.add(popoverBody);

        ListView<DisplayableValue<T>> values = new ListView<DisplayableValue<T>>(ID_VALUES,
            new PropertyModel<>(popoverModel, SearchItem.F_VALUES)) {

        	private static final long serialVersionUID = 1L;
        	
            @Override
            protected void populateItem(final ListItem<DisplayableValue<T>> item) {
                item.add(AttributeModifier.replace("style", new AbstractReadOnlyModel<String>() {

                	private static final long serialVersionUID = 1L;
                	
                    @Override
                    public String getObject() {
                        return item.getIndex() != 0 ? "margin-top: 5px;" : null;
                    }
                }));

                SearchPopupPanel<T> fragment = createPopoverFragment(item.getModel());
                fragment.setRenderBodyOnly(true);
                item.add(fragment);
            }
        };
        popoverBody.add(values);

        AjaxSubmitButton update = new AjaxSubmitButton(ID_UPDATE, createStringResource("SearchItemPanel.update")) {

            private static final long serialVersionUID = 1L;

			@Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateItemPerformed(target);
            }
        };
        popoverBody.add(update);

        AjaxButton close = new AjaxButton(ID_CLOSE, createStringResource("SearchItemPanel.close")) {
        	
        	private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                closeEditPopoverPerformed(target);
            }
        };
        popoverBody.add(close);
    }

    private SearchPopupPanel<T> createPopoverFragment(IModel<DisplayableValue<T>> data) {
//        SearchPopupPanel<T> popup;
        SearchItem<T> item = getModelObject();

        IModel<List<DisplayableValue<T>>> choices = null;

        switch (item.getType()) {
        	case REFERENCE:
        		return (SearchPopupPanel) new ReferencePopupPanel(ID_VALUE, (IModel) data) {
        			
        			private static final long serialVersionUID = 1L;

					@Override
        			protected List<QName> getAllowedRelations() {
						if (item.getAllowedRelations() != null) {
							return item.getAllowedRelations();
						}
        				return super.getAllowedRelations();
        			}
					
					@Override
					protected List<QName> getSupportedTargetList() {
						return WebComponentUtil.createSupportedTargetTypeList(((PrismReferenceDefinition) item.getDefinition()).getTargetTypeName());
					}
        		};
//        		break;
//            case BROWSER:
//                popup = new BrowserPopupPanel(ID_VALUE, data);
//                break;
            case BOOLEAN:
                choices = (IModel) createBooleanChoices();
            case ENUM:
                if (choices == null) {
                    choices = new ListModel<>(item.getAllowedValues());
                }
                return (SearchPopupPanel<T>) new ComboPopupPanel<T>(ID_VALUE, data, choices);
//                break;
            case TEXT:
            default:
                PrismObject<LookupTableType> lookupTable = findLookupTable(item.getDefinition());
                return (SearchPopupPanel<T>) new TextPopupPanel<T>(ID_VALUE, data, lookupTable);
        }

//        return popup;
    }

    private <I extends Item> PrismObject<LookupTableType> findLookupTable(ItemDefinition<I> definition) {
        PrismReferenceValue valueEnumerationRef = definition.getValueEnumerationRef();
        if (valueEnumerationRef == null) {
            return null;
        }

        PageBase page = getPageBase();

        String lookupTableUid = valueEnumerationRef.getOid();
        Task task = page.createSimpleTask("loadLookupTable");
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils.createLookupTableRetrieveOptions(getSchemaHelper());
        return WebModelServiceUtils.loadObject(LookupTableType.class, lookupTableUid, options, page, task, result);
    }

    private IModel<List<DisplayableValue<Boolean>>> createBooleanChoices() {
    	
        return new AbstractReadOnlyModel<List<DisplayableValue<Boolean>>>() {

           private static final long serialVersionUID = 1L;

			@Override
            public List<DisplayableValue<Boolean>> getObject() {
                List<DisplayableValue<Boolean>> list = new ArrayList<>();
                list.add(new SearchValue<>(Boolean.TRUE, getString("Boolean.TRUE")));
                list.add(new SearchValue<>(Boolean.FALSE, getString("Boolean.FALSE")));

                return list;
            }
        };
    }

    private IModel<String> createLabelModel() {
        return new AbstractReadOnlyModel<String>() {

        	private static final long serialVersionUID = 1L;
        	
            @Override
            public String getObject() {
                SearchItem<T> item = getModelObject();

                StringBuilder sb = new StringBuilder();
                sb.append(item.getName());
                sb.append(": ");

                List<String> values = new ArrayList<>();
                for (DisplayableValue<T> value : item.getValues()) {
                    if (StringUtils.isNotEmpty(value.getLabel())) {
                        values.add(value.getLabel());
                    }
                }

                if (!values.isEmpty()) {
                    String or = createStringResource("SearchItemPanel.or").getString();

                    sb.append('"');
                    sb.append(StringUtils.join(values, "\" " + or + " \""));
                    sb.append('"');
                } else {
                    String all = createStringResource("SearchItemPanel.all").getString();
                    sb.append(all);
                }

                return sb.toString();
            }
        };
    }

    private void updateItemPerformed(AjaxRequestTarget target) {
        SearchItem<T> item = getModelObject();
        item.getValues().clear();

        SearchItemPopoverDto<T> dto = popoverModel.getObject();
        for (DisplayableValue<T> value : dto.getValues()) {
            item.getValues().add(value);
        }

        LOG.debug("Update item performed, item {} value is {}", item.getName(), item.getValues());

        SearchPanel panel = findParent(SearchPanel.class);
        panel.refreshSearchForm(target);
        panel.searchPerformed(target);
    }

    public LoadableModel<SearchItemPopoverDto<T>> getPopoverModel() {
        return popoverModel;
    }

    private void closeEditPopoverPerformed(AjaxRequestTarget target) {
        togglePopover(target);
    }

    private void editPerformed(AjaxRequestTarget target) {
        LOG.debug("Edit performed");

        popoverModel.reset();
        target.add(get(createComponentPath(ID_POPOVER, ID_POPOVER_BODY)));
        togglePopover(target);
    }

    public void togglePopover(AjaxRequestTarget target) {
        SearchPanel panel = findParent(SearchPanel.class);
        panel.togglePopover(target, get(ID_MAIN_BUTTON), get(ID_POPOVER), 0);
    }

    private void deletePerformed(AjaxRequestTarget target) {
        SearchItem<T> item = getModelObject();
        LOG.debug("Delete of item {} performed", item.getName());

        Search search = item.getSearch();
        search.delete(item);

        SearchPanel panel = findParent(SearchPanel.class);
        panel.refreshSearchForm(target);
        panel.searchPerformed(target);
    }

    void updatePopupBody(AjaxRequestTarget target) {
        target.add(get(createComponentPath(ID_POPOVER, ID_POPOVER_BODY)));
    }
    
    public boolean isReferenceDefinition() {
    	SearchItem<T> searchItem = getModelObject();
    	if (searchItem == null) {
    		return false;
    	}
    	
    	return searchItem.getDefinition() instanceof PrismReferenceDefinition;
    }
}
