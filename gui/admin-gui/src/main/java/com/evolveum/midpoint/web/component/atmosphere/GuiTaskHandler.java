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

package com.evolveum.midpoint.web.component.atmosphere;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import org.apache.commons.lang.NotImplementedException;

import java.util.List;

/**
 * Just a sample class which performs "async" task for atmosphere example (ajax push).
 *
 * @author lazyman
 */
@Deprecated
public class GuiTaskHandler implements TaskHandler {

    @Override
    public String getCategoryName(Task task) {
        return "GuiTaskHandler.categoryName";
    }

    @Override
    public TaskRunResult run(Task task) {
        TaskRunResult result = new TaskRunResult();

        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep((int) Math.random() * 2000);

                //todo notify user session
            } catch (Exception ex) {
                //todo notify user session
            }
        }

        return result;
    }

    @Override
    public Long heartbeat(Task task) {
        throw new NotImplementedException();
    }

    @Override
    public void refreshStatus(Task task) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
