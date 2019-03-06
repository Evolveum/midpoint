import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException

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
    String token = (String) tokenObject

    switch (objectClass) {
        case ObjectClass.ACCOUNT:
            handleSyncAccount(sql, token, handler)
            break
        default:
            throw new ConnectorException("Sync not supported for object class " + objectClass)
    }
}

Object handleGetLatestSyncToken(Sql sql) {
    switch (objectClass) {
        case ObjectClass.ACCOUNT:
            return handleSyncTokenAccount(sql)
        default:
            throw new ConnectorException("Sync not supported for object class " + objectClass)
    }
}

Object handleSyncAccount(Sql sql, Object token, SyncResultsHandler handler) {
    def result = []
    def tstamp = null

    if (token != null) {
        tstamp = new java.sql.Timestamp(token)
    } else {
        def today = new Date()
        tstamp = new java.sql.Timestamp(today.time)
    }

    Closure closure = { row ->
        SyncDelta delta = buildSyncDelta(row)

        if (!handler.handle(delta)) {
            throw new IllegalStateException()
        }
    }

    // XXX the following line is probably fine for MySQL
    // sql.eachRow("select * from Users where timestamp > ${tstamp}",
    //    {result.add([operation:"CREATE_OR_UPDATE", uid:Integer.toString(it.id), token:it.timestamp.getTime(), attributes:[__NAME__:it.login, firstname:it.firstname, lastname:it.lastname, fullname:it.fullname, organization:it.organization, email:it.email, __ENABLE__:!(it.disabled as Boolean)]])}

    // the following line (the select statement) is for PostgreSQL with
    // timestamp in microseconds - we truncate the timestamp to milliseconds
    try {
        sql.eachRow("select id,login,firstname,lastname,fullname,email,organization,disabled," +
                "date_trunc('milliseconds', timestamp) as timestamp from Users where date_trunc('milliseconds',timestamp) > ${tstamp}", closure)
    } catch (IllegalStateException ex) {

    }

    log.ok("Sync script: found " + result.size() + " events to sync")
    return result;
}

Object handleSyncTokenAccount(Sql sql) {
    row = sql.firstRow("select date_trunc('milliseconds', timestamp) as timestamp from users order by timestamp desc")

    return row["timestamp"].getTime();
}

private SyncDelta buildSyncDelta(SyncDeltaType type, GroovyRowResult row) {
    SyncDeltaBuilder builder = new SyncDeltaBuilder()
    builder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE)
    builder.setObjectClass(ObjectClass.ACCOUNT)

    def newToken = null // todo
    if (newToken != null) {
        builder.setToken(new SyncToken(newToken))
    }

    ConnectorObject obj = ICFObjectBuilder.co {
        uid row.id as String
        id row.name
        attribute 'description', row.description
    }
    builder.setObject(obj)

//result.add([operation: "CREATE_OR_UPDATE",
//            uid: Integer.toString(it.id),
//            token: it.timestamp.getTime(),
//            attributes: [__NAME__: it.login,
//                         firstname: it.firstname,
//                         lastname: it.lastname,
//                         fullname: it.fullname,
//                         organization: it.organization,
//                         email: it.email,
//                         __ENABLE__: !(it.disabled as Boolean)]])

    return builder.build()
}