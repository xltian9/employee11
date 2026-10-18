# restart.ps1 - rebuild and restart employee11
$ErrorActionPreference = "Stop"
$project = "D:\ruanjian\DeepSeek Harness\code\employee11"
$env:JAVA_HOME = "D:\Code\jdk-8.0.504.1-hotspot"
$mvn = "D:\Code\apache-maven-3.9.16\bin\mvn.cmd"
$log = Join-Path $project "app.log"
$errLog = Join-Path $project "app.err.log"
Set-Location $project

Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

& $mvn -B -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "maven package failed" }
"jar size: " + (Get-Item (Join-Path $project "target\employee-1.0-SNAPSHOT.jar")).Length

$argList = @('-Dfile.encoding=UTF-8', '-jar', 'target\employee-1.0-SNAPSHOT.jar')
$proc = Start-Process -FilePath (Join-Path $env:JAVA_HOME "bin\java.exe") -ArgumentList $argList -WorkingDirectory $project -RedirectStandardOutput $log -RedirectStandardError $errLog -WindowStyle Hidden -PassThru
"started pid=" + $proc.Id

for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    $hit = Select-String -Path $log -Pattern "Started EmployeeApplication" -ErrorAction SilentlyContinue | Select-Object -Last 1
    if ($hit) {
        "started: " + $hit.Line
        exit 0
    }
}
"timeout, tail:"
Get-Content $log -Tail 20
Get-Content $errLog -Tail 20
exit 1