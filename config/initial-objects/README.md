# Initial objects

Initial objects were moved to the `system-init` module and are maintained only there:

    repo/system-init/src/main/resources/initial-objects

MidPoint loads initial objects at runtime from the classpath (the `system-init` jar),
see `InitialDataImport` and ninja `initial-objects` command.

The binary distribution still contains a copy in `doc/config/initial-objects` for
reference — it is assembled directly from the `system-init` module sources by
`dist/src/main/assembly/dist.xml`. This directory is intentionally empty (except
for this file) and no longer has to be synchronized before a release.
