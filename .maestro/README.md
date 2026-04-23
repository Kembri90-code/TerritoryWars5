# Maestro Tests

## 1) Install Maestro on Windows

Maestro is installed to:
- `C:\maestro\maestro\bin`

If command is not available in a new terminal, add to PATH (PowerShell):

```powershell
$current = [Environment]::GetEnvironmentVariable("Path","User")
if ($current -notlike "*C:\maestro\maestro\bin*") {
  [Environment]::SetEnvironmentVariable("Path", $current.TrimEnd(';') + ";C:\maestro\maestro\bin", "User")
}
```

Check:

```powershell
maestro --version
```

## 2) Prerequisites

- Android emulator is running
- `adb devices` shows at least one `device`
- app installed on emulator:
  - `.\gradlew.bat :app:installDebug`

## 3) Available flows

- `.maestro/smoke.yaml`  
  Unauthenticated smoke: launch -> login -> register -> back.

- `.maestro/capture-territory.yaml`  
  Authenticated map smoke: start capture -> verify capture panel -> cancel.

- `.maestro/clan-basic.yaml`  
  Authenticated clan smoke (non-destructive): open clan tab -> open create form OR verify existing clan UI.

- `.maestro/clan-create.yaml`  
  Destructive flow: create a clan.

- `.maestro/clan-request-join.yaml`  
  Send join request to a clan (for account without clan).

## 4) Run commands

### Login-required flows

```powershell
maestro test .maestro/capture-territory.yaml -e MAESTRO_EMAIL="your_email" -e MAESTRO_PASSWORD="your_password"
maestro test .maestro/clan-basic.yaml -e MAESTRO_EMAIL="your_email" -e MAESTRO_PASSWORD="your_password"
```

### Clan create (destructive)

```powershell
$ts = Get-Date -Format "MMddHHmm"
maestro test .maestro/clan-create.yaml `
  -e MAESTRO_EMAIL="your_email" `
  -e MAESTRO_PASSWORD="your_password" `
  -e CLAN_NAME="QA Clan $ts" `
  -e CLAN_TAG="Q$($ts.Substring(0,3))"
```

### Clan join request

```powershell
maestro test .maestro/clan-request-join.yaml -e MAESTRO_EMAIL="your_email" -e MAESTRO_PASSWORD="your_password"
```

## 5) Test hygiene

- Before each run (optional but recommended):
  - `adb shell pm clear com.territorywars`
- If location permissions dialog appears, grant it manually once.
- `capture-territory.yaml` validates capture UI start/cancel without closing polygon.
