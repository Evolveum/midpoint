package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.List;
import java.util.function.Function;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class SingleValueChoosePanel<U, T extends ObjectType> extends ConvertingMultiValueChoosePanel<U,T> {
	
	private static final long serialVersionUID = 1L;
	private IModel<U> singleTargetModel;

	public SingleValueChoosePanel(String id, List<Class<? extends T>> types,
			Function<T, U> transformFunction, IModel<U> targetModel) {
		super(id, types, transformFunction, new ListModel<U>(), false);
		singleTargetModel = targetModel;
	}

	@Override
	protected void choosePerformedHook(AjaxRequestTarget target, List<T> selected) {
		super.choosePerformedHook(target, selected);
		
		if(selected != null) {
			U transformedSelectedObject = selected.stream()
				.findFirst()
				.map(this::transform)
				.orElse(null);
			AuditLogViewerPanel.LOGGER.debug("Setting model object to {}", transformedSelectedObject);
			singleTargetModel.setObject(transformedSelectedObject);
		}
	}

	@Override
	protected void removePerformedHook(AjaxRequestTarget target, T value) {
		super.removePerformedHook(target, value);
		singleTargetModel.setObject(null);
	}
}
