echo "Stopping midPoint"
FOR /F "usebackq tokens=5" %%i IN (`netstat -aon ^| find "8080"`) DO taskkill /F /PID %%i
