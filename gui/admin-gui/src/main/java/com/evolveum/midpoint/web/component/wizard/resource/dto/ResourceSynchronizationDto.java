/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConditionalSearchFilterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author shood
 * */
public class ResourceSynchronizationDto implements Serializable{

    public static final String F_OBJECT_SYNCRONIZATION_LIST = "objectSynchronizationList";
    public static final String F_SELECTED = "selected";
	public static final String F_SELECTED_CORRELATION = "selectedCorrelation";
	public static final String F_SELECTED_REACTION = "selectedReaction";
    public static final String F_OBJECT_CLASS_LIST = "objectClassList";

	private final List<ObjectSynchronizationType> objectSynchronizationList;		// live list in resourceModel
    private ObjectSynchronizationType selected;
	private ConditionalSearchFilterType selectedCorrelation;
	private SynchronizationReactionType selectedReaction;
    private List<QName> objectClassList;
    private Map<String, String> objectTemplateMap = new HashMap<>();

	public ResourceSynchronizationDto(List<ObjectSynchronizationType> objectSynchronizationList) {
		this.objectSynchronizationList = objectSynchronizationList;
	}

	public List<ObjectSynchronizationType> getObjectSynchronizationList() {
		return objectSynchronizationList;
	}

	public ObjectSynchronizationType getSelected() {
        return selected;
    }

    public void setSelected(ObjectSynchronizationType selected) {
        this.selected = selected;
    }

	public ConditionalSearchFilterType getSelectedCorrelation() {
		return selectedCorrelation;
	}

	public void setSelectedCorrelation(ConditionalSearchFilterType selectedCorrelation) {
		this.selectedCorrelation = selectedCorrelation;
	}

	public SynchronizationReactionType getSelectedReaction() {
		return selectedReaction;
	}

	public void setSelectedReaction(SynchronizationReactionType selectedReaction) {
		this.selectedReaction = selectedReaction;
	}

	public List<QName> getObjectClassList() {
        if(objectClassList == null){
            objectClassList = new ArrayList<>();
        }

        return objectClassList;
    }

    public void setObjectClassList(List<QName> objectClassList) {
        this.objectClassList = objectClassList;
    }

    public Map<String, String> getObjectTemplateMap() {
        return objectTemplateMap;
    }

    public void setObjectTemplateMap(Map<String, String> objectTemplateMap) {
        this.objectTemplateMap = objectTemplateMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceSynchronizationDto)) return false;

        ResourceSynchronizationDto that = (ResourceSynchronizationDto) o;

        if (objectClassList != null ? !objectClassList.equals(that.objectClassList) : that.objectClassList != null)
            return false;
        if (objectTemplateMap != null ? !objectTemplateMap.equals(that.objectTemplateMap) : that.objectTemplateMap != null)
            return false;
        if (selected != null ? !selected.equals(that.selected) : that.selected != null) return false;
        if (selectedCorrelation != null ? !selectedCorrelation.equals(that.selectedCorrelation) : that.selectedCorrelation != null) return false;
		if (selectedReaction != null ? !selectedReaction.equals(that.selectedReaction) : that.selectedReaction != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (selected != null ? selected.hashCode() : 0);
        result = 31 * result + (objectClassList != null ? objectClassList.hashCode() : 0);
        result = 31 * result + (objectTemplateMap != null ? objectTemplateMap.hashCode() : 0);
        return result;
    }
}
