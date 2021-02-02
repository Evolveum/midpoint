package com.evolveum.midpoint.schema.performance;

import static org.testng.Assert.assertNotNull;

import java.util.UUID;

import javax.xml.namespace.QName;

import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TestDeltaPerfComparison extends AbstractSchemaPerformanceTest {

    private static final int REPETITIONS = 10000;



    @DataProvider(name = "combinations")
    public Object[][] testIncrementAndCompare() throws SchemaException {

        int level = 4;

        Object[][] ret = new Object[2*level][];
        for (int i = 0; i < level; i++) {
            int count=(int) Math.pow(10, i+1);
            ret[i] = new Object[] { count, PlusMinusZero.PLUS};
            ret[i+level] = new Object[] {count, PlusMinusZero.MINUS};
        }
        return ret;
    }


    @Test(dataProvider = "combinations")
    public void complexStructure(int assigmentCount, PlusMinusZero operation) throws SchemaException {

        PrismObject<UserType> user = PrismTestUtil.getPrismContext().createObject(UserType.class);
        user.setOid(newUuid());
        user.asObjectable().setName(PolyString.toPolyStringType(PolyString.fromOrig("User")));
        AssignmentType assigment = null;
        for(int i = 0; i < assigmentCount; i++) {
            assigment = randomAssigment();
            user.asObjectable().assignment(assigment);
        }


        PrismContainerValue lastAssign = assigment.asPrismContainerValue().clone();
        lastAssign.freeze();
        ObjectDelta<UserType> delta = PrismTestUtil.getPrismContext()
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .mod(operation, lastAssign)
                .asObjectDelta(user.getOid());

        Stopwatch clone = stopwatch(monitorName("delta", "clone", String.valueOf(assigmentCount)) ,
                "Cloning of structure");
        Stopwatch applyDelta = stopwatch(monitorName("delta" , operation.toString() , "apply", String.valueOf(assigmentCount))
                , "Application of delta");
        for(int i = 0; i < REPETITIONS; i++) {
            PrismObject<UserType> userMod = null;
            try (Split s = clone.start()) {
                    userMod = user.clone();
            }
            // Measure
            try (Split s = applyDelta.start()) {
                delta.applyTo(userMod);
            }
            assertNotNull(userMod.getOid());
            assertNotNull(userMod.asObjectable().getAssignment());
        }
    }

    private String newUuid() {
        return UUID.randomUUID().toString();
    }

    private AssignmentType randomAssigment() {
        AssignmentType assigment = new AssignmentType();
        ObjectReferenceType target = new ObjectReferenceType();
        target.setOid(newUuid());
        target.setRelation(new QName("default"));
        assigment.setTargetRef(target);
        return assigment;
    }
}
