/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.web.session.SessionStorage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.ResourceContentSearchDto;
import com.evolveum.midpoint.web.session.ResourceContentStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author katkav
 * @author semancik
 */
public class ResourceContentTabPanel extends Panel {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ResourceContentTabPanel.class);

	enum Operation {
		REMOVE, MODIFY;
	}

	private static final String DOT_CLASS = ResourceContentTabPanel.class.getName() + ".";

	private static final String ID_INTENT = "intent";
	private static final String ID_REAL_OBJECT_CLASS = "realObjectClass";
	private static final String ID_OBJECT_CLASS = "objectClass";
	private static final String ID_MAIN_FORM = "mainForm";

	private static final String ID_REPO_SEARCH = "repositorySearch";
	private static final String ID_RESOURCE_SEARCH = "resourceSearch";

	private static final String ID_TABLE = "table";

	private PageBase parentPage;
	private ShadowKindType kind;

	private boolean useObjectClass;
	private boolean isRepoSearch = true;

	private IModel<ResourceContentSearchDto> resourceContentSearch;

	public ResourceContentTabPanel(String id, final ShadowKindType kind,
			final IModel<PrismObject<ResourceType>> model, PageBase parentPage) {
		super(id, model);
		this.parentPage = parentPage;

		this.resourceContentSearch = createContentSearchModel(kind);

		this.kind = kind;

		initLayout(model, parentPage);
	}

	private IModel<ResourceContentSearchDto> createContentSearchModel(final ShadowKindType kind) {
		return new LoadableModel<ResourceContentSearchDto>(true) {

			private static final long serialVersionUID = 1L;

			@Override
			protected ResourceContentSearchDto load() {
                isRepoSearch  = !getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT).getResourceSearch();
                return getContentStorage(kind, isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                        SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).getContentSearch();

			}

		};

	}

	private void updateResourceContentSearch() {
		ResourceContentSearchDto searchDto = resourceContentSearch.getObject();
		getContentStorage(kind, isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).setContentSearch(searchDto);
	}

	private ResourceContentStorage getContentStorage(ShadowKindType kind, String searchMode) {
		return parentPage.getSessionStorage().getResourceContentStorage(kind, searchMode);
	}

	private void initLayout(final IModel<PrismObject<ResourceType>> model, final PageBase parentPage) {
		setOutputMarkupId(true);

		final Form mainForm = new Form(ID_MAIN_FORM);
		mainForm.setOutputMarkupId(true);
		mainForm.addOrReplace(initTable(model));
		add(mainForm);

		AutoCompleteTextPanel<String> intent = new AutoCompleteTextPanel<String>(ID_INTENT,
				new PropertyModel<String>(resourceContentSearch, "intent"), String.class) {
			private static final long serialVersionUID = 1L;

			@Override
			public Iterator<String> getIterator(String input) {
				RefinedResourceSchema refinedSchema = null;
				try {
					refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(model.getObject(),
							parentPage.getPrismContext());

				} catch (SchemaException e) {
					return new ArrayList<String>().iterator();
				}
				return RefinedResourceSchemaImpl.getIntentsForKind(refinedSchema, getKind()).iterator();

			}

		};
		intent.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(get(ID_REAL_OBJECT_CLASS));
				updateResourceContentSearch();
				mainForm.addOrReplace(initTable(model));
				target.add(mainForm);

			}
		});
		intent.setOutputMarkupId(true);
		intent.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !isUseObjectClass();
			}
		});
		add(intent);

		Label realObjectClassLabel = new Label(ID_REAL_OBJECT_CLASS, new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				RefinedObjectClassDefinition ocDef;
				try {
					RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl
							.getRefinedSchema(model.getObject(), parentPage.getPrismContext());
					if (refinedSchema == null) {
						return "NO SCHEMA DEFINED";
					}
					ocDef = refinedSchema.getRefinedDefinition(getKind(), getIntent());
					if (ocDef != null) {
						return ocDef.getObjectClassDefinition().getTypeName().getLocalPart();
					}
				} catch (SchemaException e) {
				}

				return "NOT FOUND";
			}
		});
		realObjectClassLabel.setOutputMarkupId(true);
		add(realObjectClassLabel);

		AutoCompleteQNamePanel objectClassPanel = new AutoCompleteQNamePanel(ID_OBJECT_CLASS,
				new PropertyModel<QName>(resourceContentSearch, "objectClass")) {
			private static final long serialVersionUID = 1L;

			@Override
			public Collection<QName> loadChoices() {
				return createObjectClassChoices(model);
			}

			@Override
			protected void onChange(AjaxRequestTarget target) {
				LOGGER.trace("Object class panel update: {}", isUseObjectClass());
				updateResourceContentSearch();
				mainForm.addOrReplace(initTable(model));
				target.add(mainForm);
			}

		};

		objectClassPanel.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isUseObjectClass();
			}
		});
		add(objectClassPanel);

		AjaxLink<Boolean> repoSearch = new AjaxLink<Boolean>(ID_REPO_SEARCH,
				new PropertyModel<Boolean>(resourceContentSearch, "resourceSearch")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
                isRepoSearch = true;
                getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT).setResourceSearch(Boolean.FALSE);
                getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).setResourceSearch(Boolean.FALSE);

                resourceContentSearch.getObject().setResourceSearch(Boolean.FALSE);
				updateResourceContentSearch();
				mainForm.addOrReplace(initRepoContent(model));
				target.add(getParent().addOrReplace(mainForm));
				target.add(this);
				target.add(getParent().get(ID_RESOURCE_SEARCH)
						.add(AttributeModifier.replace("class", "btn btn-sm btn-default")));
			}

			@Override
			protected void onBeforeRender() {
				super.onBeforeRender();
				if (!getModelObject().booleanValue())
					add(AttributeModifier.replace("class", "btn btn-sm btn-default active"));
			}
		};
		add(repoSearch);

		AjaxLink<Boolean> resourceSearch = new AjaxLink<Boolean>(ID_RESOURCE_SEARCH,
				new PropertyModel<Boolean>(resourceContentSearch, "resourceSearch")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
                isRepoSearch = false;
                getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT).setResourceSearch(Boolean.TRUE);
                getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).setResourceSearch(Boolean.TRUE);
                updateResourceContentSearch();
                resourceContentSearch.getObject().setResourceSearch(Boolean.TRUE);
				mainForm.addOrReplace(initResourceContent(model));
				target.add(getParent().addOrReplace(mainForm));
				target.add(this.add(AttributeModifier.append("class", " active")));
				target.add(getParent().get(ID_REPO_SEARCH)
						.add(AttributeModifier.replace("class", "btn btn-sm btn-default")));
			}

			@Override
			protected void onBeforeRender() {
				super.onBeforeRender();
				getModelObject().booleanValue();
				if (getModelObject().booleanValue())
					add(AttributeModifier.replace("class", "btn btn-sm btn-default active"));
			}
		};
		add(resourceSearch);

	}

	private List<QName> createObjectClassChoices(IModel<PrismObject<ResourceType>> model) {
		RefinedResourceSchema refinedSchema;
		try {
			refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(model.getObject(),
					parentPage.getPrismContext());
		} catch (SchemaException e) {
			warn("Could not determine defined obejct classes for resource");
			return new ArrayList<QName>();
		}
		Collection<ObjectClassComplexTypeDefinition> defs = refinedSchema.getObjectClassDefinitions();
		List<QName> objectClasses = new ArrayList<QName>(defs.size());
		for (ObjectClassComplexTypeDefinition def : defs) {
			objectClasses.add(def.getTypeName());
		}
		return objectClasses;
	}

	private ResourceContentPanel initTable(IModel<PrismObject<ResourceType>> model) {
		if (isResourceSearch()) {
			return initResourceContent(model);
		} else {
			return initRepoContent(model);
		}
	}

	private ResourceContentResourcePanel initResourceContent(IModel<PrismObject<ResourceType>> model) {
        String searchMode = isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT;
		ResourceContentResourcePanel resourceContent = new ResourceContentResourcePanel(ID_TABLE, model,
				getObjectClass(), getKind(), getIntent(), searchMode, parentPage);
		resourceContent.setOutputMarkupId(true);
		return resourceContent;

	}

	private ResourceContentRepositoryPanel initRepoContent(IModel<PrismObject<ResourceType>> model) {
        String searchMode = isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT;
		ResourceContentRepositoryPanel repositoryContent = new ResourceContentRepositoryPanel(ID_TABLE, model,
				getObjectClass(), getKind(), getIntent(), searchMode, parentPage);
		repositoryContent.setOutputMarkupId(true);
		return repositoryContent;
	}

	private ShadowKindType getKind() {
		return resourceContentSearch.getObject().getKind();
	}

	private String getIntent() {
		return resourceContentSearch.getObject().getIntent();
	}

	private QName getObjectClass() {
		return resourceContentSearch.getObject().getObjectClass();
	}

	private boolean isResourceSearch() {
		Boolean isResourceSearch = resourceContentSearch.getObject().isResourceSearch();
		if (isResourceSearch == null) {
			return false;
		}
		return resourceContentSearch.getObject().isResourceSearch();
	}

	private boolean isUseObjectClass() {
		return resourceContentSearch.getObject().isUseObjectClass();
	}

}
