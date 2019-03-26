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
package com.evolveum.midpoint.gui.impl.component.data.column;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

/**
 * @author katka
 *
 */
public class PrismContainerWrapperColumn<C extends Containerable> extends AbstractItemWrapperColumn<C, PrismContainerValueWrapper<C>>{

	public PrismContainerWrapperColumn(IModel<PrismContainerWrapper<C>> rowModel, ItemPath itemName, PageBase pageBase) {
		super(rowModel, itemName, pageBase, true);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 1L;
	
	private static final String ID_HEADER = "header";

	@Override
	public IModel<?> getDataModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
		return Model.of(rowModel.getObject().findContainer(itemName));
	}

	@Override
	protected String createLabel(PrismContainerValueWrapper<C> object) {
		C realValue = object.getRealValue();
		
		if (PolicyConstraintsType.class.isAssignableFrom(realValue.getClass())) {
			return PolicyRuleTypeUtil.toShortString((PolicyConstraintsType) realValue);
		}
		
		if (PolicyActionsType.class.isAssignableFrom(realValue.getClass())) {
			return PolicyRuleTypeUtil.toShortString((PolicyActionsType) realValue, new ArrayList<>());
		}
		
		if (ActivationType.class.isAssignableFrom(realValue.getClass())) {
			return getActivationLabelModel((ActivationType) object);
		}
		
		
		//TODO what to show?
		if (LifecycleStateModelType.class.isAssignableFrom(realValue.getClass())) {
			return realValue.toString();
		}
		
		if (ResourceObjectAssociationType.class.isAssignableFrom(realValue.getClass())) {
			return getAssociationLabel((ResourceObjectAssociationType) realValue);
		}
		
		return realValue.toString();
//		ContainerWrapperImpl lifecycleStateModel = rowModel.getObject().findContainerWrapperByName(ObjectPolicyConfigurationType.F_LIFECYCLE_STATE_MODEL);
//		
//		Label label = null;
//		if (lifecycleStateModel == null || lifecycleStateModel.getValues() == null || lifecycleStateModel.getValues().isEmpty()) {
//			item.add(new Label(componentId, ""));
//		} else {
//			ContainerWrapperImpl lifecycleState = lifecycleStateModel.findContainerWrapperByName(LifecycleStateModelType.F_STATE);
//			if (lifecycleState == null || lifecycleState.getValues() == null || lifecycleState.getValues().isEmpty()) {
//				item.add(new Label(componentId, ""));
//			} else {
//				item.add(new StaticItemWrapperColumnPanel(componentId, new PropertyModel(lifecycleState, ""), new Form("form"), null) {
//					protected IModel populateContainerItem(ContainerValueWrapper object) {
//						return Model.of(object != null ? WebComponentUtil.getDisplayName(object.getContainerValue()) : "");
//					};
//				});
//			}
//		}
		
		
	}

	private String getActivationLabelModel(ActivationType activation){
//		ContainerWrapperImpl<ActivationType> activationContainer = assignmentContainer.findContainerWrapper(assignmentContainer.getPath().append(AssignmentType.F_ACTIVATION));
//		ActivationStatusType administrativeStatus = null;
		
		if (activation.getAdministrativeStatus() != null) {
			return activation.getAdministrativeStatus().value();
		}
		
		PrismContainerWrapper<AssignmentType> assignmentModel =  (PrismContainerWrapper<AssignmentType>) getMainModel().getObject();
		PrismPropertyWrapper<String> lifecycle = assignmentModel.findProperty(AssignmentType.F_LIFECYCLE_STATE);
		
		String lifecycleState = getLifecycleState(lifecycle);
		
		ActivationStatusType status = WebModelServiceUtils.getAssignmentEffectiveStatus(lifecycleState, activation, getPageBase());
		return AssignmentsUtil.createActivationTitleModel(status, activation.getValidFrom(), activation.getValidTo(), getPageBase());
		
//		XMLGregorianCalendar validFrom = activation.getV;
//		XMLGregorianCalendar validTo = null;
//		ActivationType activation = null;
//		String lifecycleStatus = "";
//		PropertyOrReferenceWrapper lifecycleStatusProperty = assignmentContainer.findPropertyWrapperByName(AssignmentType.F_LIFECYCLE_STATE);
//		if (lifecycleStatusProperty != null && lifecycleStatusProperty.getValues() != null){
//			Iterator<ValueWrapperOld> iter = lifecycleStatusProperty.getValues().iterator();
//			if (iter.hasNext()){
//				lifecycleStatus = (String) iter.next().getValue().getRealValue();
//			}
//		}
//		if (activationContainer != null){
//			activation = new ActivationType();
//			PropertyOrReferenceWrapper administrativeStatusProperty = activationContainer.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
//			if (administrativeStatusProperty != null && administrativeStatusProperty.getValues() != null){
//				Iterator<ValueWrapperOld> iter = administrativeStatusProperty.getValues().iterator();
//				if (iter.hasNext()){
//					administrativeStatus = (ActivationStatusType) iter.next().getValue().getRealValue();
//					activation.setAdministrativeStatus(administrativeStatus);
//				}
//			}
//			PropertyOrReferenceWrapper validFromProperty = activationContainer.findPropertyWrapper(ActivationType.F_VALID_FROM);
//			if (validFromProperty != null && validFromProperty.getValues() != null){
//				Iterator<ValueWrapperOld> iter = validFromProperty.getValues().iterator();
//				if (iter.hasNext()){
//					validFrom = (XMLGregorianCalendar) iter.next().getValue().getRealValue();
//					activation.setValidFrom(validFrom);
//				}
//			}
//			PropertyOrReferenceWrapper validToProperty = activationContainer.findPropertyWrapper(ActivationType.F_VALID_TO);
//			if (validToProperty != null && validToProperty.getValues() != null){
//				Iterator<ValueWrapperOld> iter = validToProperty.getValues().iterator();
//				if (iter.hasNext()){
//					validTo = (XMLGregorianCalendar) iter.next().getValue().getRealValue();
//					activation.setValidTo(validTo);
//				}
//			}
//		}
//		if (administrativeStatus != null){
//			return Model.of(WebModelServiceUtils
//					.getAssignmentEffectiveStatus(lifecycleStatus, activation, getPageBase()).value().toLowerCase());
//		} else {
//			return AssignmentsUtil.createActivationTitleModel(WebModelServiceUtils
//							.getAssignmentEffectiveStatus(lifecycleStatus, activation, getPageBase()),
//					validFrom, validTo, getPageBase());
//		}

	}
	

    private String getAssociationLabel(ResourceObjectAssociationType association){
        if (association == null){
            return "";
        }
//        ContainerWrapperImpl<ConstructionType> constructionWrapper = assignmentWrapper.findContainerWrapper(assignmentWrapper.getPath()
//                .append(AssignmentType.F_CONSTRUCTION));
//        if (constructionWrapper == null || constructionWrapper.findContainerValueWrapper(constructionWrapper.getPath()) == null){
//            return null;
//        }
//        ContainerWrapperImpl<ResourceObjectAssociationType> associationWrapper = constructionWrapper.findContainerValueWrapper(constructionWrapper.getPath())
//                .findContainerWrapper(constructionWrapper.getPath().append(ConstructionType.F_ASSOCIATION));
//        if (associationWrapper == null || associationWrapper.getValues() == null || associationWrapper.getValues().size() == 0){
//            return null;
//        }

        //for now only use case with single association is supported
//        ContainerValueWrapper<ResourceObjectAssociationType> associationValueWrapper = associationWrapper.getValues().get(0);
//        ResourceObjectAssociationType association = associationValueWrapper.getContainerValue().asContainerable();
//
        return association != null ?
                (StringUtils.isNotEmpty(association.getDisplayName()) ? association.getDisplayName() : association.getRef().toString())
                : null;

    }

	
	private String getLifecycleState(PrismPropertyWrapper<String> lifecycle) {
		if (lifecycle == null) {
			return null;
		}
		
		List<PrismPropertyValueWrapper<String>> values = lifecycle.getValues();
		if (CollectionUtils.isEmpty(values)) {
			return null;
		}
		
		return values.iterator().next().getRealValue();
	}
	
	
	@Override
	protected Panel createValuePanel(IModel<?> headerModel, PrismContainerValueWrapper<C> object) {
		throw new UnsupportedOperationException("Panels nto supported for container values.");
	}

	@Override
	protected Component createHeader(IModel<PrismContainerWrapper<C>> mainModel) {
		return new PrismContainerHeaderPanel<>(ID_HEADER, Model.of(mainModel.getObject().findContainer(itemName)));
	}
	
}
