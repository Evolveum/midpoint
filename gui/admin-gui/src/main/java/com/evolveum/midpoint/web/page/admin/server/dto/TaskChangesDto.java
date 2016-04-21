/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.WrapperScene;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.addIgnoreNull;

/**
 * @author mederly
 */
public class TaskChangesDto implements Serializable {

//    public static final String F_TITLE_KEY = "titleKey";
    public static final String F_PRIMARY_DELTAS = "primaryDeltas";

    private String titleKey;
    private SceneDto primarySceneDto;

	public TaskChangesDto(SceneDto primarySceneDto) {
//		this.titleKey = titleKey;
		this.primarySceneDto = primarySceneDto;
	}

//	public String getTitleKey() {
//		return titleKey;
//	}

	public SceneDto getPrimaryDeltas() {
        return primarySceneDto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskChangesDto that = (TaskChangesDto) o;

        if (titleKey != null ? !titleKey.equals(that.titleKey) : that.titleKey != null) return false;
        return !(primarySceneDto != null ? !primarySceneDto.equals(that.primarySceneDto) : that.primarySceneDto != null);

    }

    @Override
    public int hashCode() {
        int result = titleKey != null ? titleKey.hashCode() : 0;
        result = 31 * result + (primarySceneDto != null ? primarySceneDto.hashCode() : 0);
        return result;
    }
}
