@echo off

rem
rem Copyright (C) 2010-2025 Evolveum and contributors
rem
rem Licensed under the EUPL-1.2 or later.
rem

for /l %%x in (1, 1, 100) do (
   echo %%x
   mvn -o -P extratest -Dit.test=TestAssociationsFirstStepsReal -Dtest.config.file=test-config-new-repo.xml -pl :story integration-test
   if errorlevel 1 exit /b
)
