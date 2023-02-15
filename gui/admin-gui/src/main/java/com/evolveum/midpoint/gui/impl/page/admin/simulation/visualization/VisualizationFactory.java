package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class VisualizationFactory {

    public static <O extends ObjectType> ObjectVisualization createObjectVisualization(@NotNull ObjectDelta<O> delta) {
        ObjectVisualization visualization = new ObjectVisualization();
        visualization.setChangeType(delta.getChangeType());

        return visualization;
    }

    public static <O extends ObjectType> ObjectVisualization createObjectVisualization(@NotNull ObjectDeltaType delta) throws SchemaException {
        ObjectDelta<O> objectDelta = DeltaConvertor.createObjectDelta(delta);
        return createObjectVisualization(objectDelta);
    }
}
