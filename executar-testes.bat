@echo off
echo ========================================================
echo   Horizon API - Executando Bateria de Testes (TCC)
echo ========================================================
cd /d "%~dp0"
call .\mvnw.cmd test
echo.
echo ========================================================
echo   Execucao concluida! Verifique os resultados acima.
echo ========================================================
pause
