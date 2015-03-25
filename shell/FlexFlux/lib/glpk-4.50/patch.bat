REM Apply patch
REM Option --binary is needed for files without CR LF
REM %gnuwin32%\patch.exe --binary -p0 -i %1
%gnuwin32%\patch.exe -p0 -i %1
