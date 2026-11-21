$appDraw = "app/src/main/res/drawable"
$genDraw = "watchface/build/generated/refinement/res/drawable"
$origDraw = "watchface/src/main/res/drawable"

if (!(Test-Path $appDraw)) {
    New-Item -ItemType Directory -Force -Path $appDraw | Out-Null
}

# 1. Moons (29 large + 29 small)
for ($i = 0; $i -le 28; $i++) {
    $pad = $i.ToString("D2")
    Copy-Item "$genDraw/lunar_moon_$pad.png" "$appDraw/" -Force
    Copy-Item "$genDraw/refined_moon_$pad.png" "$appDraw/" -Force
}

# 2. Sage Theme (0)
$sageFilesGen = @(
    "refined_0_dial_0.png", "refined_0_dial_0_aod.png",
    "refined_0_dial_1.png", "refined_0_dial_2.png",
    "refined_0_hour.png", "refined_0_hour_aod.png",
    "refined_0_minute.png", "refined_0_minute_aod.png",
    "visible_0_orn_0.png", "visible_0_orn_1.png", "visible_0_orn_2.png",
    "refined_battery.png"
)
foreach ($f in $sageFilesGen) {
    if (Test-Path "$genDraw/$f") { Copy-Item "$genDraw/$f" "$appDraw/" -Force }
}

$sageFilesOrig = @(
    "uc_0_ring.png", "uc_0_second.png", "uc_0_hub.png",
    "uc_dhikr_0.png", "uc_dhikr_1.png", "uc_dhikr_2.png", "uc_dhikr_3.png"
)
foreach ($f in $sageFilesOrig) {
    if (Test-Path "$origDraw/$f") { Copy-Item "$origDraw/$f" "$appDraw/" -Force }
}

# 3. Lunar Theme
$lunarFiles = @(
    "lunar_dial_aod.png", "lunar_hour.png", "lunar_hour_aod.png",
    "lunar_minute.png", "lunar_minute_aod.png", "lunar_second.png", "lunar_hub.png"
)
foreach ($f in $lunarFiles) {
    if (Test-Path "$genDraw/$f") { Copy-Item "$genDraw/$f" "$appDraw/" -Force }
}

# 4. Emerald Theme (3)
$emeraldFilesGen = @(
    "refined_3_dial_0.png", "refined_3_dial_0_aod.png",
    "refined_3_hour.png", "refined_3_hour_aod.png",
    "refined_3_minute.png", "refined_3_minute_aod.png",
    "visible_3_orn_1.png"
)
foreach ($f in $emeraldFilesGen) {
    if (Test-Path "$genDraw/$f") { Copy-Item "$genDraw/$f" "$appDraw/" -Force }
}
$emeraldFilesOrig = @(
    "uc_3_ring.png", "uc_3_second.png", "uc_3_hub.png"
)
foreach ($f in $emeraldFilesOrig) {
    if (Test-Path "$origDraw/$f") { Copy-Item "$origDraw/$f" "$appDraw/" -Force }
}

$count = (Get-ChildItem "$appDraw/*.png").Count
Write-Host "Total PNGs copied into app/src/main/res/drawable: $count"
