package com.evolveum.midpoint.model.impl.mining;
import static org.testng.AssertJUnit.*;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierAttributeResolver;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierAttributeResolver.UnusualAttributeValueResult;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierAttributeResolver.UnusualSingleValueDetail;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

public class OutlierAttributeResolverTest extends AbstractUnitTest {

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    void shouldResolveAllAttributes() {
        given();
        var attributeDetails = List.of(
                makeAttributeAnalysis("attr1", "v11", 10, "v12", 8, "v13", 2),
                makeAttributeAnalysis("attr2", "v21", 1, "v22", 1),
                makeAttributeAnalysis("attr3", "v31", 15)
        );
        var userAttributeDetails = List.of(
                makeUserAttributeAnalysis("attr1", "v11"),
                makeUserAttributeAnalysis("attr2", "v21"),
                makeUserAttributeAnalysis("attr3", "v31")
        );
        var resolver = new OutlierAttributeResolver(0.1);

        when();
        var results = resolver.resolveUnusualAttributes(attributeDetails, userAttributeDetails);

        then();
        assertEquals("should have same size of attributes", 3, results.size());
        var paths = results.stream().map(UnusualAttributeValueResult::path).toList();
        assertTrue("should contain attr1 result", paths.contains(makePath("attr1")));
        assertTrue("should contain attr2 result", paths.contains(makePath("attr2")));
        assertTrue("should contain attr3 result", paths.contains(makePath("attr3")));
        assertEquals("should contain only original value", 1, findResults(results, "attr1").partialResults().size());
        assertEquals("should match original value", "v11", findResults(results, "attr1").partialResults().get(0).value());
    }


    @Test
    void shouldDetectUnusualValues() {
        given();
        var attributeDetails = List.of(
                makeAttributeAnalysis("attr1", "v11", 20, "v12", 15, "v13", 2), // median equals mode
                makeAttributeAnalysis("attr2", "v21", 30, "v22", 2, "v23", 4), // median equals mode
                makeAttributeAnalysis("attr3", "v31", 50, "v32", 30, "v33", 30, "v34", 3) // median does not equal mode
        );
        var userAttributeDetails = List.of(
                makeUserAttributeAnalysis("attr1", "v13"),
                makeUserAttributeAnalysis("attr2", "v22"),
                makeUserAttributeAnalysis("attr3", "v34")
        );
        var resolver = new OutlierAttributeResolver(0.1);

        when();
        var results = resolver.resolveUnusualAttributes(attributeDetails, userAttributeDetails);

        then();
        assertTrue("attr1 should have unusual value", findResults(results, "attr1").isUnusual());
        assertTrue("attr2 should have unusual value", findResults(results, "attr2").isUnusual());
        assertTrue("attr3 should have unusual value", findResults(results, "attr3").isUnusual());
    }

    @Test
    void shouldNotDetectUnusualValues() {
        given();
        var attributeDetails = List.of(
                makeAttributeAnalysis("attr1", "v11", 20, "v12", 15, "v13", 3, "v14", 10), // median equals mode
                makeAttributeAnalysis("attr2", "v21", 30, "v22", 18), // median equals mode
                makeAttributeAnalysis("attr3", "v31", 50, "v32", 30, "v33", 30, "v34", 4) // median does not equal mode
        );
        var userAttributeDetails = List.of(
                makeUserAttributeAnalysis("attr1", "v13"),
                makeUserAttributeAnalysis("attr2", "v22"),
                makeUserAttributeAnalysis("attr3", "v34")
        );
        var resolver = new OutlierAttributeResolver(0.1);

        when();
        var results = resolver.resolveUnusualAttributes(attributeDetails, userAttributeDetails);

        then();
        assertFalse("attr1 should have usual value", findResults(results, "attr1").isUnusual());
        assertFalse("attr2 should have usual value", findResults(results, "attr2").isUnusual());
        assertFalse("attr3 should have usual value", findResults(results, "attr3").isUnusual());
    }

    @Test
    void shouldNotConsiderMissingAttributes() {
        given();
        var attributeDetails = List.of(
                makeAttributeAnalysis("attr1", "v11", 20)
        );
        List<RoleAnalysisAttributeAnalysis> userAttributeDetails = List.of();
        var resolver = new OutlierAttributeResolver(0.1);

        when();
        var results = resolver.resolveUnusualAttributes(attributeDetails, userAttributeDetails);

        then();
        assertEquals("missing attributes are not supported", 0, results.size());
    }

    @Test
    void shouldConsiderMultivaluedValue() {
        given();
        var attributeDetails = List.of(
                makeAttributeAnalysis("attr1", "v11", 20, "v12", 20, "v13", 20),
                makeAttributeAnalysis("attr2", "v21", 20, "v22", 1, "v23", 1)
        );
        List<RoleAnalysisAttributeAnalysis> userAttributeDetails = List.of(
                makeAttributeAnalysis("attr1", "v12", null, "v13", null),
                makeAttributeAnalysis("attr2", "v21", null, "v22", null)
        );
        var resolver = new OutlierAttributeResolver(0.1);

        when();
        var results = resolver.resolveUnusualAttributes(attributeDetails, userAttributeDetails);

        then();
        assertFalse("missing attr1 is not unusual", findResults(results, "attr1").isUnusual());
        assertTrue("missing attr2 is unusual", findResults(results, "attr2").isUnusual());

        assertFalse("missing attr2 v21 is not unusual", findPartialResults(results, "attr2", "v21").isUnusual());
        assertTrue("missing attr2 v22 is unusual", findPartialResults(results, "attr2", "v22").isUnusual());

    }

    private ItemPathType makePath(String path) {
        return new ItemPathType(ItemPath.fromString(path));
    }

    private RoleAnalysisAttributeAnalysis makeAttributeAnalysis(String path, Object... valueCountTuples) {
        var result = new RoleAnalysisAttributeAnalysis();
        result.setItemPath(makePath(path));
        result.createAttributeStatisticsList();
        for (var i = 0; i < valueCountTuples.length; i += 2) {
            var stats = new RoleAnalysisAttributeStatistics();
            stats.setAttributeValue((String) valueCountTuples[i]);
            stats.setInRepo((Integer) valueCountTuples[i + 1]);
            result.getAttributeStatistics().add(stats);
        }
        return result;
    }

    private RoleAnalysisAttributeAnalysis makeUserAttributeAnalysis(String path, String value) {
        return makeAttributeAnalysis(path, value, null);
    }

    private UnusualAttributeValueResult findResults(List<UnusualAttributeValueResult> results, String path) {
        return results
                .stream()
                .filter(r -> r.path().equals(makePath(path)))
                .findFirst()
                .orElseThrow();
    }

    private UnusualSingleValueDetail findPartialResults(List<UnusualAttributeValueResult> results, String path, String value) {
        return findResults(results, path)
                .partialResults()
                .stream()
                .filter(p -> p.value().equals(value))
                .findFirst()
                .orElseThrow();
    }

}
