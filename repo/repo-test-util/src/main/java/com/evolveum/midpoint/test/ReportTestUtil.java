/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimpleObjectResolver;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ReportTestUtil {

    public static File findOutputFile(PrismObject<TaskType> taskObject, SimpleObjectResolver resolver, OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        TaskType task = taskObject.asObjectable();
        ActivityStateType activity = task.getActivityState() != null ? task.getActivityState().getActivity() : null;
        if (activity == null) {
            return null;
        }

        if (!(activity.getWorkState() instanceof ReportExportWorkStateType)) {
            return null;
        }

        ReportExportWorkStateType state = (ReportExportWorkStateType) activity.getWorkState();
        ObjectReferenceType reportDataRef = state.getReportDataRef();
        if (reportDataRef == null) {
            return null;
        }

        ReportDataType reportData = resolver
                .getObject(ReportDataType.class, reportDataRef.getOid(), null, result)
                .asObjectable();

        String filePath = reportData.getFilePath();
        if (filePath == null) {
            return null;
        }

        return new File(filePath);
    }

    public static List<String> getLinesOfOutputFile(
            PrismObject<TaskType> task, SimpleObjectResolver resolver, OperationResult result)
            throws IOException, SchemaException, ObjectNotFoundException {

        File outputFile = findOutputFile(task, resolver, result);
        PrismTestUtil.display("Found report file", outputFile);
        assertNotNull("No output file for " + task, outputFile);

        List<String> lines = Files.readAllLines(Paths.get(outputFile.getPath()));
        PrismTestUtil.display("Report content (" + lines.size() + " lines)", String.join("\n", lines));
        // TODO is this rename necessary?
        boolean renamedSuccessful =
                outputFile.renameTo(new File(outputFile.getParentFile(), "processed-" + outputFile.getName()));
        assertThat(renamedSuccessful).as("rename successful").isTrue();
        return lines;
    }
}
