package com.evolveum.midpoint.testing.story.sysperf;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

class TaskDumper {

    void dumpTask(PrismObject<TaskType> task, String prefix) throws SchemaException, IOException {
        var string = PrismContext.get()
                .xmlSerializer()
                .serialize(task);
        Files.writeString(
                getFile(prefix, task).toPath(),
                string);
    }

    private File getFile(String prefix, PrismObject<TaskType> task) {
        return new File(TARGET_DIR, START + "-" + OTHER_PARAMETERS.label + "-" + prefix + "-" + task.asObjectable().getName() + ".xml");
    }
}
