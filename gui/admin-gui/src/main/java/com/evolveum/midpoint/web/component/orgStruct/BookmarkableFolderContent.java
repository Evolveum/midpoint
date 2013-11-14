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

package com.evolveum.midpoint.web.component.orgStruct;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.orgStruct.AbstractTree.State;
import com.evolveum.midpoint.web.page.admin.users.PageOrgStruct;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mserbak
 */
public class BookmarkableFolderContent extends Content {
	private static final String DOT_CLASS = BookmarkableFolderContent.class.getName() + ".";
	private static final String OPERATION_LOAD_ORGUNIT = DOT_CLASS + "loadOrgUnit";

	public BookmarkableFolderContent() {
	}

	@Override
	public Component newContentComponent(String id, final AbstractTree<NodeDto> tree,
			final IModel<NodeDto> model) {
		return createContentComponent(id, tree, model);

	}

	@Override
	public Component newNodeComponent(String id, final AbstractTree<NodeDto> tree, final IModel<NodeDto> model) {
		return new TreeNode<NodeDto>(id, tree, model) {

			@Override
			protected Component createContent(String id, IModel<NodeDto> model) {
				return tree.newContentComponent(id, model);
			}

			@Override
			protected MarkupContainer createJunctionComponent(String id) {
				return new AjaxFallbackLink<Void>(id) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						NodeDto t = model.getObject();
						if (tree.getState(t) == State.EXPANDED) {
							tree.collapse(t);
						} else {
							t.setNodes(getNodes(t));
							tree.expand(t);
						}
						target.appendJavaScript("initMenuButtons()");
					}

					@Override
					public boolean isEnabled() {
						final NodeDto node = model.getObject();
						return tree.getProvider().hasChildren(node);
					}
				};
			}

			@Override
			protected String getStyleClass() {
				NodeDto t = getModelObject();
				if (t.getType().equals(NodeType.FOLDER)) {
					if (tree.getState(t).equals(State.EXPANDED)) {
						return getExpandedStyleClass(t);
					} else {
						return getCollapsedStyleClass();
					}
				}
				return getOtherStyleClass();
			}
		};
	}

	private List<NodeDto> getNodes(NodeDto parent) {
		OrgStructDto orgUnit = loadOrgUnit(parent);
		List<NodeDto> listNodes = new ArrayList<NodeDto>();

		if (orgUnit.getOrgUnitDtoList() != null && !orgUnit.getOrgUnitDtoList().isEmpty()) {
			for (Object orgObject : orgUnit.getOrgUnitDtoList()) {
				NodeDto org = (NodeDto) orgObject;
				listNodes.add(org);
			}
		}

		if (orgUnit.getUserDtoList() != null && !orgUnit.getUserDtoList().isEmpty()) {
			for (Object userObject : orgUnit.getUserDtoList()) {
				NodeDto user = (NodeDto) userObject;
				listNodes.add(user);
			}
		}
		return listNodes;
	}

	private Node<NodeDto> createContentComponent(String id, final AbstractTree<NodeDto> tree,
			final IModel<NodeDto> model) {
		return new Node<NodeDto>(id, model) {

			@Override
			protected MarkupContainer newLinkComponent(String id, IModel<NodeDto> model) {
				final NodeDto node = model.getObject();
//				if (tree.getProvider().hasChildren(node)) {
					tree.collapse(node);
					return super.newLinkComponent(id, model);

//				} else {
//					tree.collapse(node);
//					return new LabeledWebMarkupContainer(id, model) {
//					};
//				}
			}

			@Override
			protected void onClick(AjaxRequestTarget target) {
				NodeDto t = getModelObject();
				if (tree.getState(t) == State.EXPANDED) {
					tree.collapse(t);
				} else {
					t.setNodes(getNodes(t));
					tree.expand(t);
				}
				target.appendJavaScript("initMenuButtons()");
			}

			@Override
			protected String getStyleClass() {
				NodeDto t = getModelObject();
				String styleClass;
				if (tree.getProvider().hasChildren(t)) {
					if (tree.getState(t) == State.EXPANDED) {
						styleClass = getOpenStyleClass();
					} else {
						styleClass = getClosedStyleClass();
					}
				} else {
					styleClass = getOtherStyleClass(t);
				}

				if (isSelected()) {
					styleClass += " " + getSelectedStyleClass();
				}

				return styleClass;
			}

			@Override
			protected void initOrgMenu(WebMarkupContainer orgPanel) {
				AjaxLink edit = new AjaxLink("orgEdit", createStringResource("styledLinkLabel.orgMenu.edit")) {
					@Override
					public void onClick(AjaxRequestTarget target) {

					}
				};
				orgPanel.add(edit);

				AjaxLink rename = new AjaxLink("orgRename",
						createStringResource("styledLinkLabel.orgMenu.rename")) {
					@Override
					public void onClick(AjaxRequestTarget target) {
						// model.getObject().setEditable(!model.getObject().isEditable());
					}
				};
				orgPanel.add(rename);

				AjaxLink createSub = new AjaxLink("orgCreateSub",
						createStringResource("styledLinkLabel.orgMenu.createSub")) {
					@Override
					public void onClick(AjaxRequestTarget target) {

					}
				};
				orgPanel.add(createSub);

				AjaxLink del = new AjaxLink("orgDel", createStringResource("styledLinkLabel.orgMenu.del")) {
					@Override
					public void onClick(AjaxRequestTarget target) {

					}
				};
				orgPanel.add(del);
			}

			@Override
			protected void initUserMenu(WebMarkupContainer userPanel) {
				AjaxLink edit = new AjaxLink("userEdit",
						createStringResource("styledLinkLabel.userMenu.edit")) {
					@Override
					public void onClick(AjaxRequestTarget target) {

					}
				};
				userPanel.add(edit);

				AjaxLink rename = new AjaxLink("userRename",
						createStringResource("styledLinkLabel.userMenu.rename")) {
					@Override
					public void onClick(AjaxRequestTarget target) {

					}
				};
				userPanel.add(rename);

				AjaxLink enable = new AjaxLink("userEnable",
						createStringResource("styledLinkLabel.userMenu.enable")) {
					@Override
					public void onClick(AjaxRequestTarget target) {

					}
				};
				userPanel.add(enable);

				AjaxLink disable = new AjaxLink("userDisable",
						createStringResource("styledLinkLabel.userMenu.disable")) {
					@Override
					public void onClick(AjaxRequestTarget target) {

					}
				};
				userPanel.add(disable);

				AjaxLink changeAttr = new AjaxLink("userChangeAttr",
						createStringResource("styledLinkLabel.userMenu.changeAttr")) {
					@Override
					public void onClick(AjaxRequestTarget target) {

					}
				};
				userPanel.add(changeAttr);
			}

		};
	}

	private OrgStructDto loadOrgUnit(NodeDto parent) {
        TraceManager.getTrace(BookmarkableFolderContent.class).info(">>> LOADING ORG. UNITS");
		Task task = createSimpleTask(OPERATION_LOAD_ORGUNIT);
		OperationResult result = new OperationResult(OPERATION_LOAD_ORGUNIT);

		OrgStructDto newOrgModel = null;
		List<PrismObject<ObjectType>> orgUnitList;

		OrgFilter orgFilter = OrgFilter.createOrg(parent.getOid(), null, 1);
		ObjectQuery query = ObjectQuery.createObjectQuery(orgFilter);
        query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

		try {
            Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
            options.add(SelectorOptions.create(ObjectType.F_PARENT_ORG_REF,
                    GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

			orgUnitList = getModelService().searchObjects(ObjectType.class, query, options, task, result);
			newOrgModel = new OrgStructDto<ObjectType>(orgUnitList, parent, result);
			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Unable to load org unit", ex);
		}

		if (!result.isSuccess()) {
			showResultInSession(result);
			throw new RestartResponseException(PageOrgStruct.class);
		}

//		if (newOrgModel.getOrgUnitDtoList() == null) {
//			result.recordFatalError("Unable to load org unit");
//			showResultInSession(result);
//			throw new RestartResponseException(PageOrgStruct.class);
//		}
        TraceManager.getTrace(BookmarkableFolderContent.class).info(">>> LOADING ORG. UNITS FINISHED");
		return newOrgModel;
	}
}