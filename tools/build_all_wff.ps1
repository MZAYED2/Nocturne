# build_all_wff.ps1
# Builds and validates Sage, Lunar, and Emerald WFF packages for Watch Face Push

Add-Type -AssemblyName System.IO.Compression.FileSystem

$buildTools = "C:\Users\mohamed zayed\AppData\Local\Android\Sdk\build-tools\36.0.0"
$apksigner = "$buildTools\apksigner.bat"
$zipalign = "$buildTools\zipalign.exe"
$keystore = "C:\Users\mohamed zayed\.android\debug.keystore"
$java = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
$validator = "F:\Progects-Android-Studio\Nocturne\tools\validator-push-cli.jar"

$baseApk = "F:\Progects-Android-Studio\Nocturne\watchface\build\outputs\apk\debug\watchface-debug.apk"
$outDir = "F:\Progects-Android-Studio\Nocturne\distribution"

if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

function Create-ZipPosix($sourceDir, $zipPath) {
    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
    & $java -cp "tools" tools.PosixZipRepack $sourceDir $zipPath | Out-Null
}

function Build-And-Validate-WFF($name, $id, $xmlFile, $outApkName, $desc, $previewFile) {
    Write-Host "`n=================================================="
    Write-Host "Building WFF Package: $name ($id)"
    Write-Host "=================================================="

    $tempDir = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), [System.Guid]::NewGuid().ToString())
    [System.IO.Compression.ZipFile]::ExtractToDirectory($baseApk, $tempDir)

    # 1. Ensure all generated drawables are present
    $genDrawable = "F:\Progects-Android-Studio\Nocturne\watchface\build\generated\refinement\res\drawable"
    $targetDrawable = [System.IO.Path]::Combine($tempDir, "res", "drawable")
    if (Test-Path $genDrawable) {
        Get-ChildItem -Path $genDrawable | ForEach-Object {
            $dest = [System.IO.Path]::Combine($targetDrawable, $_.Name)
            if (-not (Test-Path $dest)) {
                Copy-Item $_.FullName $dest -Force
            }
        }
    }

    # 2. Replace res/raw/watchface.xml
    $rawDir = [System.IO.Path]::Combine($tempDir, "res", "raw")
    if (-not (Test-Path $rawDir)) { New-Item -ItemType Directory -Path $rawDir -Force | Out-Null }
    Copy-Item $xmlFile ([System.IO.Path]::Combine($rawDir, "watchface.xml")) -Force

    # 3. Clean non-WFF files (*.dex, *.properties, signatures)
    Get-ChildItem -Path $tempDir -Filter "*.dex" -Recurse | Remove-Item -Force
    Get-ChildItem -Path $tempDir -Filter "*.properties" -Recurse | Remove-Item -Force
    $metaInf = [System.IO.Path]::Combine($tempDir, "META-INF")
    if (Test-Path $metaInf) {
        Get-ChildItem -Path $metaInf -Filter "*.SF" | Remove-Item -Force
        Get-ChildItem -Path $metaInf -Filter "*.RSA" | Remove-Item -Force
        Get-ChildItem -Path $metaInf -Filter "*.DSA" | Remove-Item -Force
        Get-ChildItem -Path $metaInf -Filter "*.EC" | Remove-Item -Force
        Get-ChildItem -Path $metaInf -Filter "MANIFEST.MF" | Remove-Item -Force
    }

    # 4. Repack with strictly forward slashes
    $unaligned = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), [System.Guid]::NewGuid().ToString() + "_unaligned.apk")
    Create-ZipPosix $tempDir $unaligned
    Remove-Item -Path $tempDir -Recurse -Force

    # 5. Zipalign
    $aligned = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), [System.Guid]::NewGuid().ToString() + "_aligned.apk")
    & $zipalign -p -f 4 $unaligned $aligned
    Remove-Item -Path $unaligned -Force

    # 6. Apksigner
    $finalApk = [System.IO.Path]::Combine($outDir, $outApkName)
    if (Test-Path $finalApk) { Remove-Item $finalApk -Force }
    & $apksigner sign --ks $keystore --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android --out $finalApk $aligned
    Remove-Item -Path $aligned -Force

    Write-Host "Created signed APK: $finalApk"

    # 7. Run Watch Face Push Validator
    Write-Host "Running Watch Face Push Validator..."
    $valOutput = & $java -jar $validator -a $finalApk -p "com.mohamedzayed.nocturne.wear" 2>&1
    $token = $null
    foreach ($line in $valOutput) {
        Write-Host "  $line"
        if ($line -match "generated token:\s*(\S+)") {
            $token = $matches[1]
        }
        if ($line -match "Validation token:\s*(\S+)") {
            $token = $matches[1]
        }
    }

    if ($token) {
        Write-Host ">>> SUCCESS! Validation token: $token" -ForegroundColor Green
        return @{
            id = $id
            name = $name
            description = $desc
            apk_filename = $outApkName
            validation_token = $token
            preview = $previewFile
            package_name = "com.mohamedzayed.nocturne.watchface"
            version_code = 1
            version_name = "1.0.0"
        }
    } else {
        Write-Host ">>> VALIDATION FAILED for $name" -ForegroundColor Red
        return $null
    }
}

# 1. Sage Nocturne
$sage = Build-And-Validate-WFF "Sage Nocturne" "sage_nocturne" "F:\Progects-Android-Studio\Nocturne\tools\watchface_sage.xml" "Nocturne-Sage-Push.apk" "Ottoman Sage Green heritage dial with engraved calligraphy, small moon phase, and active/AOD modes." "preview_sage.png"

# 2. Lunar Phase
$lunar = Build-And-Validate-WFF "Lunar Phase" "lunar_phase" "F:\Progects-Android-Studio\Nocturne\tools\watchface_lunar.xml" "Nocturne-Lunar-Push.apk" "Large poetic astronomical moon disc filling the watch display, with luminous dual-contrast hands and minimal AOD dial." "preview_lunar.png"

# 3. Emerald Nocturne (Test Modified Face)
$emerald = Build-And-Validate-WFF "Emerald Nocturne" "emerald_nocturne" "F:\Progects-Android-Studio\Nocturne\tools\watchface_emerald.xml" "Nocturne-Emerald-Push.apk" "Imperial Emerald and Gold edition with ornate Ottoman pattern, golden hands, and emerald accents." "preview_emerald.png"

# Generate catalog.json
$catalog = @{
    version = 1
    updated_at = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
    watchfaces = @($sage, $lunar, $emerald)
}

$catalogJson = $catalog | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllText("F:\Progects-Android-Studio\Nocturne\catalog.json", $catalogJson, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText("F:\Progects-Android-Studio\Nocturne\distribution\catalog.json", $catalogJson, [System.Text.Encoding]::UTF8)

Write-Host "`n=================================================="
Write-Host "CATALOG GENERATED AT catalog.json"
Write-Host "=================================================="
Write-Host $catalogJson
