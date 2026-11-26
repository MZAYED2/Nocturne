$releaseDir = "F:\Projects\Nocturne\release-v1.0.9-beta.1"
Copy-Item "F:\Projects\Nocturne\catalog.json" "$releaseDir\" -Force

$files = Get-ChildItem -Path "$releaseDir\*" -Include *.apk, *.json
$lines = @()
foreach ($f in $files) {
    $hash = (Get-FileHash -Path $f.FullName -Algorithm SHA256).Hash.ToLower()
    $lines += "$hash  $($f.Name)"
}
[System.IO.File]::WriteAllLines("$releaseDir\SHA256SUMS.txt", $lines)

Write-Host "Generated SHA256SUMS.txt:"
Get-Content "$releaseDir\SHA256SUMS.txt"
