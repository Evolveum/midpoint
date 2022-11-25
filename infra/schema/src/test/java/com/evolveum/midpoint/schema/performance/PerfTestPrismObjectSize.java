package com.evolveum.midpoint.schema.performance;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.axiom.concepts.CheckedFunction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParserNoIO;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.impl.xnode.ListXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class PerfTestPrismObjectSize extends AbstractSchemaPerformanceTest {

    public static final int EXECUTION = 3000;
    public static final int REPEATS = 1;

    private static final QName CONTAINER_ID = new QName("id");

    private static final String[] FILE_FORMATS = new String[] {"xml","json","yaml"};

    private static final int[] CONTAINER_COUNTS = new int[] { 10, 20, 50, 100, 200, 500, 1000};

    private static final int[] DELTA_OP_COUNT = new int[] {1,2,5,10,20,50,100,200};


    @Test(dataProvider = "combinations")
    public void fromXnodeToFile(ContainerTestParams config) throws Exception {
        RootXNode input = config.testObject();
        for(String format : FILE_FORMATS) {
            PrismSerializer<String> serializer = getPrismContext().serializerFor(format);
            String monitorId = monitorName("serialize.xnode", config.monitorId() , format);
            String note = "Measures serialization from xnode to data stream. Test parameters: " + config;
            measure(monitorId, note, () -> serializer.serialize(input));
        }
    }

    @Test(dataProvider = "combinationsConflicts")
    public void fromXnodeToPrism(ContainerTestParams config) throws Exception {
        RootXNode input = config.testObject();
        PrismParserNoIO parser = getPrismContext().parserFor(input);
        String monitorId = monitorName("parse.prism", config.monitorId());
        String note = "Measures unmarshalling of Prism Objects from XNodes. Test parameters: " + config;
        measure(monitorId, note, parser::parse);
    }

    @Test(dataProvider = "combinations")
    public void fromPrismToXnode(ContainerTestParams config) throws Exception {
        PrismObject<?> input = getPrismContext().parserFor(config.testObject()).parse();
        PrismSerializer<RootXNode> serializer = getPrismContext().xnodeSerializer();
        String monitorId = monitorName("serialize.prism", config.monitorId());
        String note = "Measures unmarshalling of Prism Objects from XNodes. Test parameters: " + config;
        measure(monitorId, note, () -> serializer.serialize(input));
    }

    @Test(dataProvider = "combinations")
    public void fromFileToXNode(ContainerTestParams config) throws Exception {
        for(String format : FILE_FORMATS) {
            String input = getPrismContext().serializerFor(format).serialize(config.testObject());
            PrismParserNoIO parser = getPrismContext().parserFor(input);
            String monitorId = monitorName("parse.xnode", config.monitorId(), format);
            String note = "Measures parsing of JSON/XML/YAML to XNodes. Test parameters: " + config;
            measure(monitorId, note, parser::parseToXNode);
        }
    }

    private void measureDelta(ContainerTestParams config, String name,  CheckedFunction<List<AssignmentType>,ObjectDelta<UserType>,SchemaException> deltaFactory) throws SchemaException {
        PrismObject<UserType> xnodeObj = getPrismContext().parserFor(config.testObject()).parse();
        List<AssignmentType> existingAssignments = xnodeObj.asObjectable().getAssignment();
        ObjectDelta<UserType> delta = deltaFactory.apply(existingAssignments);
        String monitorId = monitorName("delta", name);
        String note = "Application of delta " + name + " Test Params: " + config;
        try {
            measure(monitorId, note, () -> {
                PrismObject<UserType> objClone = xnodeObj.clone();
                delta.applyTo(objClone);
                return objClone;
            });
        } catch (CommonException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(dataProvider = "combinations")
    public void replaceDelta(ContainerTestParams config) throws SchemaException {
        for (int maxOps : DELTA_OP_COUNT) {
            int opCount = Math.min(config.count, maxOps);
            measureDelta(config, monitorName("replace",config.monitorId(), Integer.toString(opCount)), assignments -> {
                DeltaBuilder<UserType> delta = new DeltaBuilder<>(UserType.class, getPrismContext(), null);
                for (int i = assignments.size() - opCount; i < assignments.size(); i++) {
                    AssignmentType assignment = assignments.get(i).clone();
                    assignment.description("Modified");
                    delta = (DeltaBuilder<UserType>) delta.item(UserType.F_ASSIGNMENT).replace(assignment.asPrismContainerValue());
                }
                return delta.asObjectDelta("");
            });
        }
    }

    @Test(dataProvider = "combinations")
    public void addDelta(ContainerTestParams config) throws SchemaException {
        for (int maxOps : DELTA_OP_COUNT) {
            int opCount = maxOps;
            measureDelta(config, monitorName("add",config.monitorId(), Integer.toString(opCount)), assignments -> {
                DeltaBuilder<UserType> delta = new DeltaBuilder<>(UserType.class, getPrismContext(), null);
                for (int i = 0; i < opCount; i++) {
                    AssignmentType assignment = assignments.get(0).clone();
                    assignment.getConstruction().resourceRef(newUuid(), assignment.getConstruction().getResourceRef().getType());
                    assignment.setId(null);
                    delta = (DeltaBuilder<UserType>) delta.item(UserType.F_ASSIGNMENT).add(assignment.asPrismContainerValue());
                }
                return delta.asObjectDelta("");
            });
        }
    }

    @Test(dataProvider = "combinations")
    public void deleteDelta(ContainerTestParams config) throws SchemaException {
        for (int maxOps : DELTA_OP_COUNT) {
            int opCount = Math.min(config.count, maxOps);
            measureDelta(config, monitorName("delete",config.monitorId(), Integer.toString(opCount)), assignments -> {
                DeltaBuilder<UserType> delta = new DeltaBuilder<>(UserType.class, getPrismContext(), null);
                for (int i = assignments.size() - opCount; i < assignments.size(); i++) {
                    AssignmentType assignment = assignments.get(i).clone();
                    delta = (DeltaBuilder<UserType>) delta.item(UserType.F_ASSIGNMENT).delete(assignment.asPrismContainerValue());
                }
                return delta.asObjectDelta("");
            });
        }
    }


    @Test(dataProvider = "combinations")
    public void deleteNoIdDelta(ContainerTestParams config) throws SchemaException {
        for (int maxOps : DELTA_OP_COUNT) {
            int opCount = Math.min(config.count, maxOps);
            measureDelta(config, monitorName("delete.no.id",config.monitorId(), Integer.toString(opCount)), assignments -> {
                DeltaBuilder<UserType> delta = new DeltaBuilder<>(UserType.class, getPrismContext(), null);
                for (int i = assignments.size() - opCount; i < assignments.size(); i++) {
                    AssignmentType assignment = assignments.get(i).clone();
                    delta = (DeltaBuilder<UserType>) delta.item(UserType.F_ASSIGNMENT).delete(assignment.asPrismContainerValue());
                }
                return delta.asObjectDelta("");
            });
        }
    }

    private static final List<String> generateUUIDs(int count) {
        List<String> ret = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ret.add(newUuid());
        }
        return ret;
    }

    private static String newUuid() {
        return UUID.randomUUID().toString();
    }

    @DataProvider(name = "combinations")
    public Object[][] combinationsWithoutConflicts() {
        return createCombinations(false);
    }

    @DataProvider(name = "combinationsConflicts")
    public Object[][] combinationsWithConflicts() {
        return createCombinations(true);
    }

    private static Object[][] createCombinations(boolean withConflicts) {
        ArrayList<Object[]> configs = new ArrayList<>();
        for (int containerCount : CONTAINER_COUNTS) {
            int conflicts = 2 * containerCount / 10;
            configs.add(new Object[] { new ContainerTestParams(containerCount, 0, false)});
            configs.add(new Object[] { new ContainerTestParams(containerCount, 0, true)});
            if (withConflicts && conflicts > 0) {
                configs.add(new Object[] { new ContainerTestParams(containerCount, conflicts, false)});
                configs.add(new Object[] { new ContainerTestParams(containerCount, conflicts, true)});
            }
        }
        return configs.toArray(new Object[configs.size()][]);
    }

    @Override
    protected void measure(String label, String note, CheckedProducer<?> producer) throws CommonException, IOException {
        super.measure(label, note, producer, EXECUTION, REPEATS);
    }

    private static final RootXNode generateTestObject(int count, int conflicts, boolean withIds) throws SchemaException, IOException {
        @NotNull
        PrismObject<UserType> baseObject = getJack();

        @NotNull
        RootXNode rootNode = getPrismContext().xnodeSerializer().serialize(baseObject);
        MapXNodeImpl user = (MapXNodeImpl) rootNode.getSubnode();

        ListXNodeImpl assignments = new ListXNodeImpl();

        int unique = count - conflicts;
        List<String> uuids = generateUUIDs(unique);
        // Lets insert unique

        for (int i = 0; i < unique; i++) {
            assignments.add(createXNodeAssignment(uuids.get(i), withIds ? i : -1));
        }
        for (int i = 0; i < conflicts; i++) {
            int id = unique - 1 - i;
            assignments.add(createXNodeAssignment(uuids.get(id), withIds ? id : -1));
        }
        user.put(UserType.F_ASSIGNMENT, assignments);
        return rootNode;
    }


    private static final MapXNodeImpl createXNodeAssignment(String uuid, int id) {
        MapXNodeImpl assignment = new MapXNodeImpl();
        if(id > 0) {
            assignment.put(CONTAINER_ID, attribute(Long.valueOf(id)));
        }
        MapXNodeImpl construction = new MapXNodeImpl();
        assignment.put(AssignmentType.F_CONSTRUCTION, construction);
        MapXNodeImpl resourceRef = new MapXNodeImpl();
        construction.put(ConstructionType.F_RESOURCE_REF, resourceRef);
        resourceRef.put(ObjectReferenceType.F_OID, attribute(uuid));
        resourceRef.put(ObjectReferenceType.F_TYPE, attribute(ResourceType.COMPLEX_TYPE));
        return assignment;
    }

    private static @NotNull XNodeImpl attribute(Object value) {
        PrimitiveXNodeImpl<Object> attr = new PrimitiveXNodeImpl<>(value);
        attr.setAttribute(true);
        return attr;
    }


    public static class ContainerTestParams {

        public ContainerTestParams(int count, int conflicts, boolean withIds) {
            this.count = count;
            this.conflicts = conflicts;
            this.withIds = withIds;
        }

        private final int count;
        private final int conflicts;
        private final boolean withIds;

        public RootXNode testObject() {
            try {
                return generateTestObject(count, conflicts, withIds);
            } catch (Exception e) {
                throw new IllegalStateException("Test object generation failed",e);
            }
        }

        public String monitorId() {
            return new StringBuilder()
                    .append(count)
                    .append(".")
                    .append(conflicts)
                    .append(".")
                    .append(withIds ? "ids" : "noids")
                    .toString()
                    ;
        }

        public String testNote() {
            return new StringBuilder("container count: ")
                    .append(count)
                    .append(", conflicting content count: ")
                    .append(conflicts)
                    .append(" container ids: ")
                    .append(withIds ? "yes" : "no")
                    .toString()
                    ;
        }

        @Override
        public String toString() {
            return "[count=" + count + ", conflicts=" + conflicts + ", withIds=" + withIds + "]";
        }
    }
}
