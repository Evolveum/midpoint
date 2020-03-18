package com.evolveum.midpoint.repo.sql.testing;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Class for DB cleanup before the test class is run.
 * Partially duplicates {@link TestSqlRepositoryBeanPostProcessor} - if this is successful
 * we should somehow consolidate/merge these.
 */
@Component
public class DbDeleter {

    private static final Trace LOGGER = TraceManager.getTrace(DbDeleter.class);

    private static final String TRUNCATE_FUNCTION = "cleanupTestDatabase";
    private static final String TRUNCATE_PROCEDURE = "cleanupTestDatabaseProc";

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TestSqlRepositoryFactory testSqlRepositoryFactory;

    public void cleanupTestDatabase() {
        SqlRepositoryConfiguration config = testSqlRepositoryFactory.getSqlConfiguration();
        if (!config.isDropIfExists()) {
            LOGGER.info("We're not deleting objects from DB if drop-if-exists=false.");
            return;
        }

        System.out.println("\n!!! DELETING objects from database.");
        LOGGER.info("Deleting objects from database.");

        long startMs = System.currentTimeMillis();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            Query query;
            if (config.isUsingH2()) {
                LOGGER.info("Using bunch of deletes for H2.");
                deleteAllTables(entityManager);
            } else if (config.isUsingOracle() || config.isUsingSQLServer()) {
                LOGGER.info("Using truncate procedure.");
                query = entityManager.createNativeQuery("{ call " + TRUNCATE_PROCEDURE + "() }");
                query.executeUpdate();
            } else {
                LOGGER.info("Using truncate function.");
                query = entityManager.createNativeQuery("select " + TRUNCATE_FUNCTION + "();");
                query.getSingleResult();
            }

            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            LOGGER.error("Couldn't cleanup database, reason:", ex);
            entityManager.getTransaction().rollback();
            throw ex;
        } finally {
            entityManager.close();
        }
        LOGGER.info("DB cleaned up in {} ms", System.currentTimeMillis() - startMs);
    }

    private void deleteAllTables(EntityManager entityManager) {
        deleteTable(entityManager, "m_object_text_info");
        deleteTable(entityManager, "m_operation_execution");
        deleteTable(entityManager, "m_sequence");
        deleteTable(entityManager, "m_acc_cert_wi_reference");
        deleteTable(entityManager, "m_acc_cert_wi");
        deleteTable(entityManager, "m_acc_cert_case");
        deleteTable(entityManager, "m_acc_cert_campaign");
        deleteTable(entityManager, "m_acc_cert_definition");
        deleteTable(entityManager, "m_audit_resource");
        deleteTable(entityManager, "m_audit_ref_value");
        deleteTable(entityManager, "m_audit_prop_value");
        deleteTable(entityManager, "m_audit_delta");
        deleteTable(entityManager, "m_audit_item");
        deleteTable(entityManager, "m_audit_event");
        deleteTable(entityManager, "m_object_ext_date");
        deleteTable(entityManager, "m_object_ext_long");
        deleteTable(entityManager, "m_object_ext_string");
        deleteTable(entityManager, "m_object_ext_poly");
        deleteTable(entityManager, "m_object_ext_reference");
        deleteTable(entityManager, "m_object_ext_boolean");
        deleteTable(entityManager, "m_reference");
        deleteTable(entityManager, "m_assignment_ext_date");
        deleteTable(entityManager, "m_assignment_ext_long");
        deleteTable(entityManager, "m_assignment_ext_poly");
        deleteTable(entityManager, "m_assignment_ext_reference");
        deleteTable(entityManager, "m_assignment_ext_string");
        deleteTable(entityManager, "m_assignment_ext_boolean");
        deleteTable(entityManager, "m_assignment_extension");
        deleteTable(entityManager, "m_assignment_reference");
        deleteTable(entityManager, "m_assignment_policy_situation");
        deleteTable(entityManager, "m_assignment");
        deleteTable(entityManager, "m_connector_target_system");
        deleteTable(entityManager, "m_connector");
        deleteTable(entityManager, "m_connector_host");
        deleteTable(entityManager, "m_lookup_table_row");
        deleteTable(entityManager, "m_lookup_table");
        deleteTable(entityManager, "m_node");
        deleteTable(entityManager, "m_shadow");
        deleteTable(entityManager, "m_task_dependent");
        deleteTable(entityManager, "m_task");
        deleteTable(entityManager, "m_object_template");
        deleteTable(entityManager, "m_value_policy");
        deleteTable(entityManager, "m_resource");
        deleteTable(entityManager, "m_user_employee_type");
        deleteTable(entityManager, "m_user_organization");
        deleteTable(entityManager, "m_user_organizational_unit");
        deleteTable(entityManager, "m_focus_photo");
        deleteTable(entityManager, "m_focus_policy_situation");
        deleteTable(entityManager, "m_user");
        deleteTable(entityManager, "m_report");
        deleteTable(entityManager, "m_report_output");
        deleteTable(entityManager, "m_org_org_type");
        deleteTable(entityManager, "m_org_closure");
        deleteTable(entityManager, "m_org");
        deleteTable(entityManager, "m_role");
        deleteTable(entityManager, "m_service_type");
        deleteTable(entityManager, "m_service");
        deleteTable(entityManager, "m_archetype");
        deleteTable(entityManager, "m_abstract_role");
        deleteTable(entityManager, "m_system_configuration");
        deleteTable(entityManager, "m_generic_object");
        deleteTable(entityManager, "m_trigger");
        deleteTable(entityManager, "m_focus");
        deleteTable(entityManager, "m_security_policy");
        deleteTable(entityManager, "m_form");
        deleteTable(entityManager, "m_case_wi_reference");
        deleteTable(entityManager, "m_case_wi");
        deleteTable(entityManager, "m_case");
        deleteTable(entityManager, "m_function_library");
        deleteTable(entityManager, "m_ext_item");
        deleteTable(entityManager, "m_object_subtype");
        deleteTable(entityManager, "m_object_collection");
        deleteTable(entityManager, "m_dashboard");
        deleteTable(entityManager, "m_object");
    }

    private void deleteTable(EntityManager entityManager, String tableName) {
        entityManager.createNativeQuery("DELETE FROM " + tableName).executeUpdate();
    }
}
