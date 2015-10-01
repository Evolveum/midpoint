package com.evolveum.midpoint.web.page.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.ContextRelativeResource;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentTableDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class PageAdminAbstractRole<T extends AbstractRoleType> extends PageAdminFocus<T> {

	private static final String ID_SUMMARY_PANEL = "summaryPanel";
	private static final String ID_SUMMARY_NAME = "summaryName";
	private static final String ID_SUMMARY_DISPLAY_NAME = "summaryDisplayName";
	private static final String ID_SUMMARY_IDENTIFIER = "summaryIdentifier";
	// private static final String ID_SUMMARY_FAMILY_NAME = "summaryFamilyName";
	private static final String ID_SUMMARY_PHOTO = "summaryPhoto";

	private static final String ID_INDUCEMENT_MENU = "assignmentMenu";
	private static final String ID_SHADOW_CHECK_ALL = "shadowCheckAll";
	private static final String ID_INDUCEMENT_CHECK_ALL = "assignmentCheckAll";
	private static final String OPERATION_LOAD_INDUCEMENTS = "loadInducements";

	private IModel<PrismObject<T>> summaryObject;
	private LoadableModel<List<AssignmentEditorDto>> inducementsModel;

	@Override
	protected void prepareFocusDeltaForModify(ObjectDelta<T> focusDelta) throws SchemaException {
		super.prepareFocusDeltaForModify(focusDelta);

		PrismObject<T> abstractRole = getFocusWrapper().getObject();
		PrismContainerDefinition<AssignmentType> def = abstractRole.getDefinition()
				.findContainerDefinition(AbstractRoleType.F_INDUCEMENT);
		handleAssignmentDeltas(focusDelta, inducementsModel.getObject(), def);

		}

	@Override
	protected void prepareFocusForAdd(PrismObject<T> focus) throws SchemaException {
		super.prepareFocusForAdd(focus);
		handleAssignmentForAdd(focus, AbstractRoleType.F_INDUCEMENT, focus.asObjectable().getInducement());
	
	}

	private void initSummaryInfo(Form mainForm) {

		WebMarkupContainer summaryContainer = new WebMarkupContainer(ID_SUMMARY_PANEL);
		summaryContainer.setOutputMarkupId(true);

		summaryContainer.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				if (getPageParameters().get(OnePageParameterEncoder.PARAMETER).isEmpty()) {
					return false;
				} else {
					return true;
				}
			}
		});

		mainForm.add(summaryContainer);

		summaryObject = new AbstractReadOnlyModel<PrismObject<T>>() {

			@Override
			public PrismObject<T> getObject() {
				ObjectWrapper object = getFocusWrapper();
				return object.getObject();
			}
		};

		summaryContainer.add(new Label(ID_SUMMARY_DISPLAY_NAME,
				new PrismPropertyModel<>(summaryObject, AbstractRoleType.F_DISPLAY_NAME)));
		summaryContainer.add(
				new Label(ID_SUMMARY_NAME, new PrismPropertyModel<>(summaryObject, AbstractRoleType.F_NAME)));

		summaryContainer.add(new Label(ID_SUMMARY_IDENTIFIER,
				new PrismPropertyModel<>(summaryObject, AbstractRoleType.F_IDENTIFIER)));

		Image img = new Image(ID_SUMMARY_PHOTO, new AbstractReadOnlyModel<AbstractResource>() {

			@Override
			public AbstractResource getObject() {
				 if(summaryObject.getObject().asObjectable().getJpegPhoto() != null){
	                    return new ByteArrayResource("image/jpeg", summaryObject.getObject().asObjectable().getJpegPhoto());
	                } else {
	                    return new ContextRelativeResource("img/placeholder.png");
	                }
				
			}
		});
		summaryContainer.add(img);
	}

	@Override
	protected void performCustomInitialization() {
		inducementsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {

			@Override
			protected List<AssignmentEditorDto> load() {
				return loadInducements();
			}
		};

	}

	// TODO unify with loadAssignments
	private List<AssignmentEditorDto> loadInducements() {

		List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

		ObjectWrapper focusWrapper = getFocusWrapper();
		PrismObject<T> focus = focusWrapper.getObject();
		List<AssignmentType> inducements = focus.asObjectable().getInducement();
		for (AssignmentType inducement : inducements) {
			list.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, inducement, this));
		}

		Collections.sort(list);

		return list;
	}

	@Override
	protected void initCustomLayout(Form mainForm) {

		initSummaryInfo(mainForm);

	}

	@Override
	protected void initTabs(List<ITab> tabs) {
		tabs.add(new AbstractTab(createStringResource("FocusType.inducement")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new AssignmentTablePanel(panelId, createStringResource("FocusType.inducement"), inducementsModel) {

					@Override
					public List<AssignmentType> getAssignmentTypeList() {
						return ((AbstractRoleType) getFocusWrapper().getObject().asObjectable())
								.getInducement();
					}

					@Override
					public String getExcludeOid() {
						return getFocusWrapper().getObject().asObjectable().getOid();
					}
				};
			}
		});
	}

	// private AssignmentTablePanel initInducements() {
	// AssignmentTablePanel inducements = new
	// AssignmentTablePanel(ID_INDUCEMENTS_TABLE,
	// new Model<AssignmentTableDto>(),
	// createStringResource("PageOrgUnit.title.inducements")) {
	//
	// @Override
	// public List<AssignmentType> getAssignmentTypeList() {
	// return ((AbstractRoleType)
	// getFocusWrapper().getObject().asObjectable()).getInducement();
	// }
	//
	// @Override
	// public String getExcludeOid() {
	// return getFocusWrapper().getObject().asObjectable().getOid();
	// }
	// };
	// return inducements;
	// }
}
