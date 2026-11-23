param()

$src = "$PSScriptRoot\..\legacy-projects\Minimal-Sage\watchface\src\main\res\raw\watchface.xml"
$dest = "$PSScriptRoot\..\watchface\src\main\res\raw\watchface.xml"

$doc = New-Object System.Xml.XmlDocument
$doc.PreserveWhitespace = $true
$doc.Load($src)

# 1. Add watch_theme to UserConfigurations
$userConfigs = $doc.DocumentElement["UserConfigurations"]
$themeConfig = $doc.CreateElement("ListConfiguration")
$themeConfig.SetAttribute("id", "watch_theme")
$themeConfig.SetAttribute("displayName", "theme_selection_label")
$themeConfig.SetAttribute("defaultValue", "sage_nocturne")

$opt1 = $doc.CreateElement("ListOption")
$opt1.SetAttribute("id", "sage_nocturne")
$opt1.SetAttribute("displayName", "theme_sage_nocturne")
$themeConfig.AppendChild($opt1) | Out-Null

$opt2 = $doc.CreateElement("ListOption")
$opt2.SetAttribute("id", "lunar_phase")
$opt2.SetAttribute("displayName", "theme_lunar_phase")
$themeConfig.AppendChild($opt2) | Out-Null

$userConfigs.InsertBefore($themeConfig, $userConfigs.FirstChild) | Out-Null

# 2. Modify Scene elements
$scene = $doc.DocumentElement["Scene"]

# Find active group
$activeGroup = $null
foreach ($child in $scene.ChildNodes) {
    if ($child.Name -eq "Group" -and $child.GetAttribute("name") -eq "active") {
        $activeGroup = $child
        break
    }
}

if ($activeGroup -ne $null) {
    $activeGroup.SetAttribute("alpha", "[CONFIGURATION.watch_theme] == 'lunar_phase' ? 0 : 255")
}

# 3. Create active_lunar group
$lunarActive = $doc.CreateElement("Group")
$lunarActive.SetAttribute("name", "active_lunar")
$lunarActive.SetAttribute("x", "0")
$lunarActive.SetAttribute("y", "0")
$lunarActive.SetAttribute("width", "1000")
$lunarActive.SetAttribute("height", "1000")
$lunarActive.SetAttribute("alpha", "[CONFIGURATION.watch_theme] == 'lunar_phase' ? 255 : 0")

$variantAmbient = $doc.CreateElement("Variant")
$variantAmbient.SetAttribute("mode", "AMBIENT")
$variantAmbient.SetAttribute("target", "alpha")
$variantAmbient.SetAttribute("value", "0")
$lunarActive.AppendChild($variantAmbient) | Out-Null

$condition = $doc.CreateElement("Condition")
$expressions = $doc.CreateElement("Expressions")

for ($i = 0; $i -lt 28; $i++) {
    $expr = $doc.CreateElement("Expression")
    $expr.SetAttribute("name", "lmoon$i")
    if ($i -eq 0) {
        $expr.InnerText = "[MOON_PHASE_POSITION] < 0.5 || [MOON_PHASE_POSITION] >= 28.5"
    } else {
        $limit = $i + 0.5
        $expr.InnerText = "[MOON_PHASE_POSITION] < $limit"
    }
    $expressions.AppendChild($expr) | Out-Null
}
$condition.AppendChild($expressions) | Out-Null

for ($i = 0; $i -lt 28; $i++) {
    $cmp = $doc.CreateElement("Compare")
    $cmp.SetAttribute("expression", "lmoon$i")
    $part = $doc.CreateElement("PartImage")
    $part.SetAttribute("x", "40")
    $part.SetAttribute("y", "40")
    $part.SetAttribute("width", "920")
    $part.SetAttribute("height", "920")
    $part.SetAttribute("alpha", "255")
    $img = $doc.CreateElement("Image")
    $pad = $i.ToString("D2")
    $img.SetAttribute("resource", "lunar_moon_$pad")
    $part.AppendChild($img) | Out-Null
    $cmp.AppendChild($part) | Out-Null
    $condition.AppendChild($cmp) | Out-Null
}

$defaultCmp = $doc.CreateElement("Default")
$defaultPart = $doc.CreateElement("PartImage")
$defaultPart.SetAttribute("x", "40")
$defaultPart.SetAttribute("y", "40")
$defaultPart.SetAttribute("width", "920")
$defaultPart.SetAttribute("height", "920")
$defaultPart.SetAttribute("alpha", "255")
$defaultImg = $doc.CreateElement("Image")
$defaultImg.SetAttribute("resource", "lunar_moon_28")
$defaultPart.AppendChild($defaultImg) | Out-Null
$defaultCmp.AppendChild($defaultPart) | Out-Null
$condition.AppendChild($defaultCmp) | Out-Null

$lunarActive.AppendChild($condition) | Out-Null

$scene.InsertAfter($lunarActive, $activeGroup) | Out-Null

# 4. Hide ComplicationSlots for lunar_phase
foreach ($child in $scene.ChildNodes) {
    if ($child.Name -eq "ComplicationSlot") {
        $child.SetAttribute("alpha", "[CONFIGURATION.watch_theme] == 'lunar_phase' ? 0 : 255")
    }
}

# 5. Hide active_foreground for lunar_phase and add active_foreground_lunar
$activeFg = $null
foreach ($child in $scene.ChildNodes) {
    if ($child.Name -eq "Group" -and $child.GetAttribute("name") -eq "active_foreground") {
        $activeFg = $child
        break
    }
}

if ($activeFg -ne $null) {
    $activeFg.SetAttribute("alpha", "[CONFIGURATION.watch_theme] == 'lunar_phase' ? 0 : 255")

    $lunarFg = $doc.CreateElement("Group")
    $lunarFg.SetAttribute("name", "active_foreground_lunar")
    $lunarFg.SetAttribute("x", "0")
    $lunarFg.SetAttribute("y", "0")
    $lunarFg.SetAttribute("width", "1000")
    $lunarFg.SetAttribute("height", "1000")
    $lunarFg.SetAttribute("alpha", "[CONFIGURATION.watch_theme] == 'lunar_phase' ? 255 : 0")

    $vAmb = $doc.CreateElement("Variant")
    $vAmb.SetAttribute("mode", "AMBIENT")
    $vAmb.SetAttribute("target", "alpha")
    $vAmb.SetAttribute("value", "0")
    $lunarFg.AppendChild($vAmb) | Out-Null

    $clock = $doc.CreateElement("AnalogClock")
    $clock.SetAttribute("x", "0")
    $clock.SetAttribute("y", "0")
    $clock.SetAttribute("width", "1000")
    $clock.SetAttribute("height", "1000")

    $hour = $doc.CreateElement("HourHand")
    $hour.SetAttribute("resource", "lunar_hour")
    $hour.SetAttribute("x", "480")
    $hour.SetAttribute("y", "250")
    $hour.SetAttribute("width", "40")
    $hour.SetAttribute("height", "280")
    $hour.SetAttribute("pivotX", "0.5")
    $hour.SetAttribute("pivotY", "0.8928571428571429")
    $clock.AppendChild($hour) | Out-Null

    $min = $doc.CreateElement("MinuteHand")
    $min.SetAttribute("resource", "lunar_minute")
    $min.SetAttribute("x", "483")
    $min.SetAttribute("y", "150")
    $min.SetAttribute("width", "34")
    $min.SetAttribute("height", "380")
    $min.SetAttribute("pivotX", "0.5")
    $min.SetAttribute("pivotY", "0.9210526315789473")
    $clock.AppendChild($min) | Out-Null

    $sec = $doc.CreateElement("SecondHand")
    $sec.SetAttribute("resource", "lunar_second")
    $sec.SetAttribute("x", "492")
    $sec.SetAttribute("y", "140")
    $sec.SetAttribute("width", "16")
    $sec.SetAttribute("height", "395")
    $sec.SetAttribute("pivotX", "0.5")
    $sec.SetAttribute("pivotY", "0.9113924050632911")
    $clock.AppendChild($sec) | Out-Null

    $lunarFg.AppendChild($clock) | Out-Null

    $hub = $doc.CreateElement("PartImage")
    $hub.SetAttribute("x", "483")
    $hub.SetAttribute("y", "483")
    $hub.SetAttribute("width", "34")
    $hub.SetAttribute("height", "34")
    $hubImg = $doc.CreateElement("Image")
    $hubImg.SetAttribute("resource", "lunar_hub")
    $hub.AppendChild($hubImg) | Out-Null
    $lunarFg.AppendChild($hub) | Out-Null

    $scene.InsertAfter($lunarFg, $activeFg) | Out-Null
}

# 6. Ambient mode
$ambientGroup = $null
foreach ($child in $scene.ChildNodes) {
    if ($child.Name -eq "Group" -and $child.GetAttribute("name") -eq "ambient") {
        $ambientGroup = $child
        break
    }
}

if ($ambientGroup -ne $null) {
    $ambientGroup.SetAttribute("alpha", "[CONFIGURATION.watch_theme] == 'lunar_phase' ? 0 : 255")

    $lunarAmbient = $doc.CreateElement("Group")
    $lunarAmbient.SetAttribute("name", "ambient_lunar")
    $lunarAmbient.SetAttribute("x", "0")
    $lunarAmbient.SetAttribute("y", "0")
    $lunarAmbient.SetAttribute("width", "1000")
    $lunarAmbient.SetAttribute("height", "1000")
    $lunarAmbient.SetAttribute("alpha", "[CONFIGURATION.watch_theme] == 'lunar_phase' ? 255 : 0")

    $vInt = $doc.CreateElement("Variant")
    $vInt.SetAttribute("mode", "INTERACTIVE")
    $vInt.SetAttribute("target", "alpha")
    $vInt.SetAttribute("value", "0")
    $lunarAmbient.AppendChild($vInt) | Out-Null

    $vAmb2 = $doc.CreateElement("Variant")
    $vAmb2.SetAttribute("mode", "AMBIENT")
    $vAmb2.SetAttribute("target", "alpha")
    $vAmb2.SetAttribute("value", "255")
    $lunarAmbient.AppendChild($vAmb2) | Out-Null

    # Lunar AOD dial (hours & minutes marks)
    $dialPart = $doc.CreateElement("PartImage")
    $dialPart.SetAttribute("x", "0")
    $dialPart.SetAttribute("y", "0")
    $dialPart.SetAttribute("width", "1000")
    $dialPart.SetAttribute("height", "1000")
    $dialImg = $doc.CreateElement("Image")
    $dialImg.SetAttribute("resource", "lunar_dial_aod")
    $dialPart.AppendChild($dialImg) | Out-Null
    $lunarAmbient.AppendChild($dialPart) | Out-Null

    # Lunar AOD hands (hours & minutes only)
    $clockAmb = $doc.CreateElement("AnalogClock")
    $clockAmb.SetAttribute("x", "0")
    $clockAmb.SetAttribute("y", "0")
    $clockAmb.SetAttribute("width", "1000")
    $clockAmb.SetAttribute("height", "1000")

    $hourAmb = $doc.CreateElement("HourHand")
    $hourAmb.SetAttribute("resource", "lunar_hour")
    $hourAmb.SetAttribute("x", "480")
    $hourAmb.SetAttribute("y", "250")
    $hourAmb.SetAttribute("width", "40")
    $hourAmb.SetAttribute("height", "280")
    $hourAmb.SetAttribute("pivotX", "0.5")
    $hourAmb.SetAttribute("pivotY", "0.8928571428571429")
    $clockAmb.AppendChild($hourAmb) | Out-Null

    $minAmb = $doc.CreateElement("MinuteHand")
    $minAmb.SetAttribute("resource", "lunar_minute")
    $minAmb.SetAttribute("x", "483")
    $minAmb.SetAttribute("y", "150")
    $minAmb.SetAttribute("width", "34")
    $minAmb.SetAttribute("height", "380")
    $minAmb.SetAttribute("pivotX", "0.5")
    $minAmb.SetAttribute("pivotY", "0.9210526315789473")
    $clockAmb.AppendChild($minAmb) | Out-Null

    $lunarAmbient.AppendChild($clockAmb) | Out-Null

    $hubAmb = $doc.CreateElement("PartImage")
    $hubAmb.SetAttribute("x", "483")
    $hubAmb.SetAttribute("y", "483")
    $hubAmb.SetAttribute("width", "34")
    $hubAmb.SetAttribute("height", "34")
    $hubImgAmb = $doc.CreateElement("Image")
    $hubImgAmb.SetAttribute("resource", "lunar_hub")
    $hubAmb.AppendChild($hubImgAmb) | Out-Null
    $lunarAmbient.AppendChild($hubAmb) | Out-Null

    $scene.InsertAfter($lunarAmbient, $ambientGroup) | Out-Null
}

$doc.Save($dest)
Write-Host "Transformed watchface.xml saved successfully!"
