import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log

def configuration = configuration as ScriptedSQLConfiguration
def operation = operation as OperationType
def objectClass = objectClass as ObjectClass
def log = log as Log

log.info("Entering " + operation + " Script");

def sql = new Sql(connection)

switch (operation) {
    case OperationType.SYNC:
        def token = token as Object
        def handler = handler as SyncResultsHandler

        handleSync(sql, token, handler)
        break
    case OperationType.GET_LATEST_SYNC_TOKEN:
        return handleGetLatestSyncToken(sql)
}

void handleSync(Sql sql, Object tokenObject, SyncResultsHandler handler) {
    // todo implement
}

Object handleGetLatestSyncToken(Sql sql) {
    // todo implement
}