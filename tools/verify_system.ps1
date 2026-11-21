Write-Host "=== 1. Test catalog.json parsing ==="
$cat = Get-Content 'catalog.json' | ConvertFrom-Json
Write-Host "Catalog version: $($cat.version)"
Write-Host "Watchfaces count: $($cat.watchfaces.Count)"
foreach ($wf in $cat.watchfaces) {
    Write-Host " - ID: $($wf.id) | Name: $($wf.name) | Token: $($wf.validation_token)"
}

Write-Host "`n=== 2. Test GitHub Releases API Accessibility ==="
try {
    $resp = Invoke-RestMethod -Uri 'https://api.github.com/repos/MZAYED2/Nocturne/releases/latest' -Headers @{'User-Agent'='Nocturne-Verifier'} -ErrorAction Stop
    Write-Host "Latest Release Tag on GitHub: $($resp.tag_name)"
    Write-Host "Assets on GitHub: $($resp.assets.Count)"
    foreach ($a in $resp.assets) {
        $mb = [math]::Round($a.size / 1048576, 2)
        Write-Host "   Asset: $($a.name) ($mb MB)"
    }
} catch {
    Write-Host "GitHub API response: $($_.Exception.Message)"
}

Write-Host "`n=== 3. Test APK Header Magic Bytes ==="
$apk = 'app\build\outputs\apk\release\app-release.apk'
$bytes = [System.IO.File]::ReadAllBytes($apk)[0..3]
$hex = ($bytes | ForEach-Object { $_.ToString('X2') }) -join ' '
Write-Host "APK Magic Bytes: $hex (Expected: 50 4B 03 04)"
if ($hex -eq '50 4B 03 04') { Write-Host '✓ Valid Zip/APK container header' }

Write-Host "`n=== 4. Test Local WFF Cache in App Assets ==="
$assetApks = Get-ChildItem 'app\src\main\assets\watchfaces\*.apk'
foreach ($f in $assetApks) {
    $mb = [math]::Round($f.Length / 1048576, 2)
    Write-Host "   Local Asset: $($f.Name) ($mb MB)"
}
