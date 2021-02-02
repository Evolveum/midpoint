package com.evolveum.midpoint.testing.schrodinger.component;

import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class AssignmentPanelTest extends AbstractSchrodingerTest {

    private static final File ASSIGNMENTS_COUNT_TEST_USER = new File("./src/test/resources/component/objects/assignment/assignments-count-test.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(ASSIGNMENTS_COUNT_TEST_USER);
    }

    @Test
    public void test0010assignmentsCountMatchesTableRowsCount() {
        basicPage
                .listUsers()
                    .table()
                        .search()
                            .byName()
                            .inputValue("assignmentsCountTest")
                            .updateSearch()
                        .and()
                    .clickByName("assignmentsCountTest")
                        .selectTabAssignments()
                            .assertAssignmentsCountLabelEquals("1")
                            .table()
                                .assertTableObjectsCountEquals(1);
        basicPage
                .listUsers()
                    .table()
                        .search()
                            .byName()
                            .inputValue("userForDelegation")
                            .updateSearch()
                        .and()
                    .clickByName("userForDelegation")
                        .selectTabAssignments()
                            .assertAssignmentsCountLabelEquals("0")
                            .table()
                                .assertTableObjectsCountEquals(0);

    }
}
