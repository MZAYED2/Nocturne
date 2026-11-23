Add-Type -AssemblyName System.Drawing

$targetDirs = @(
    "$PSScriptRoot\..\watchface\src\main\res\drawable",
    "$PSScriptRoot\..\app\src\main\res\drawable"
)

function Save-ImageBoth($name, $bmp) {
    foreach ($dir in $script:targetDirs) {
        $p = Join-Path $dir "$name.png"
        $bmp.Save($p, [System.Drawing.Imaging.ImageFormat]::Png)
    }
}

# --- 1. Obsidian Dial Base (Active) ---
$dial = New-Object System.Drawing.Bitmap(1000, 1000, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($dial)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

# Outer Black Bezel & Base
$baseBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 16, 19, 22)) # #101316
$g.FillEllipse($baseBrush, 0, 0, 1000, 1000)

# Main Dial Face with Vertical Satin Brush
$dialFace = New-Object System.Drawing.Drawing2D.GraphicsPath
$dialFace.AddEllipse(30, 30, 940, 940)
$faceBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush($dialFace)
$faceBrush.CenterColor = [System.Drawing.Color]::FromArgb(255, 38, 43, 48) # #262B30
$faceBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 20, 24, 28)) # #14181C
$g.FillPath($faceBrush, $dialFace)

# Subtle concentric tracks
$trackPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(40, 170, 182, 187), 1.5)
$g.DrawEllipse($trackPen, 50, 50, 900, 900)
$g.DrawEllipse($trackPen, 70, 70, 860, 860)

# --- 6 o'clock Small Seconds Subdial (Center at 500, 710, radius 130) ---
$secSub = New-Object System.Drawing.Drawing2D.GraphicsPath
$secSub.AddEllipse(370, 580, 260, 260)
$secBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush($secSub)
$secBrush.CenterColor = [System.Drawing.Color]::FromArgb(255, 25, 29, 34)
$secBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 12, 15, 18))
$g.FillPath($secBrush, $secSub)

# Concentric Azurage rings on Small Seconds
for ($r = 20; $r -le 120; $r += 12) {
    $azPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(25, 197, 205, 208), 1.0)
    $g.DrawEllipse($azPen, 500 - $r, 710 - $r, $r * 2, $r * 2)
}
# Small seconds rim
$rimPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(180, 170, 182, 187), 2.0)
$g.DrawEllipse($rimPen, 370, 580, 260, 260)

# Small seconds tick marks (every 5 seconds)
$fontSub = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Bold)
$subTextBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(220, 231, 229, 222))
for ($s = 0; $s -lt 60; $s += 5) {
    $ang = [Math]::PI * 2 * ($s / 60.0) - [Math]::PI / 2
    $x1 = 500 + [Math]::Cos($ang) * 115
    $y1 = 710 + [Math]::Sin($ang) * 115
    $x2 = 500 + [Math]::Cos($ang) * 125
    $y2 = 710 + [Math]::Sin($ang) * 125
    $g.DrawLine($rimPen, [float]$x1, [float]$y1, [float]$x2, [float]$y2)

    if ($s % 15 -eq 0) {
        $tx = 500 + [Math]::Cos($ang) * 95 - 12
        $ty = 710 + [Math]::Sin($ang) * 95 - 12
        $g.DrawString($s.ToString(), $fontSub, $subTextBrush, [float]$tx, [float]$ty)
    }
}

# --- 9 o'clock Power Reserve Subdial (Center at 285, 490, radius 105) ---
$pwrSub = New-Object System.Drawing.Drawing2D.GraphicsPath
$pwrSub.AddEllipse(180, 385, 210, 210)
$pwrBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush($pwrSub)
$pwrBrush.CenterColor = [System.Drawing.Color]::FromArgb(255, 28, 33, 38)
$pwrBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 14, 17, 20))
$g.FillPath($pwrBrush, $pwrSub)
$g.DrawEllipse($rimPen, 180, 385, 210, 210)

# Power Reserve Accent Arc (Sage Grey #778B86)
$sagePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(220, 119, 139, 134), 3.5)
$g.DrawArc($sagePen, 195, 400, 180, 180, 135, 120)

# Power Reserve Labels (E ... F)
$fontPwr = New-Object System.Drawing.Font("Segoe UI", 15, [System.Drawing.FontStyle]::Bold)
$pwrLabelBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(210, 231, 229, 222))
$g.DrawString("E", $fontPwr, $pwrLabelBrush, 210, 485)
$g.DrawString("F", $fontPwr, $pwrLabelBrush, 335, 485)

# --- 4:30 Date Window Frame (at 690, 660, size 100x64, angle 28 deg) ---
$dateState = $g.Save()
$g.TranslateTransform(690, 660)
$g.RotateTransform(28)
# Shadow cavity
$dateShadow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 8, 11, 13)) # #080B0D
$g.FillRectangle($dateShadow, -45, -28, 90, 56)
# Beveled Platinum Frame
$framePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(230, 197, 205, 208), 2.5)
$g.DrawRectangle($framePen, -45, -28, 90, 56)
$g.Restore($dateState)

# --- Applied Faceted Titanium Hour Markers (1 to 12) ---
$markerBrushL = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 210, 218, 222))
$markerBrushR = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 145, 155, 160))
$markerShadow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(120, 8, 11, 13))

for ($h = 1; $h -le 12; $h++) {
    if ($h -eq 6) { continue } # Small seconds occupies 6
    $ang = [Math]::PI * 2 * ($h / 12.0) - [Math]::PI / 2
    $state = $g.Save()
    $mx = 500 + [Math]::Cos($ang) * 420
    $my = 500 + [Math]::Sin($ang) * 420
    $g.TranslateTransform($mx, $my)
    $g.RotateTransform($h * 30.0)

    # Drop shadow
    $g.FillRectangle($markerShadow, -5, -26, 12, 54)
    # Left facet (light)
    $g.FillPolygon($markerBrushL, @(
        (New-Object System.Drawing.PointF(-6, -25)),
        (New-Object System.Drawing.PointF(0, -28)),
        (New-Object System.Drawing.PointF(0, 28)),
        (New-Object System.Drawing.PointF(-6, 25))
    ))
    # Right facet (shade)
    $g.FillPolygon($markerBrushR, @(
        (New-Object System.Drawing.PointF(0, -28)),
        (New-Object System.Drawing.PointF(6, -25)),
        (New-Object System.Drawing.PointF(6, 25)),
        (New-Object System.Drawing.PointF(0, 28))
    ))
    $g.Restore($state)
}

# Minute track around perimeter (every minute)
$minPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(140, 170, 182, 187), 1.5)
$minMajorPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(220, 231, 229, 222), 2.5)
for ($m = 0; $m -lt 60; $m++) {
    $ang = [Math]::PI * 2 * ($m / 60.0) - [Math]::PI / 2
    $isMaj = ($m % 5 -eq 0)
    $len = if ($isMaj) { 14 } else { 7 }
    $p = if ($isMaj) { $minMajorPen } else { $minPen }
    $x1 = 500 + [Math]::Cos($ang) * 465
    $y1 = 500 + [Math]::Sin($ang) * 465
    $x2 = 500 + [Math]::Cos($ang) * (465 - $len)
    $y2 = 500 + [Math]::Sin($ang) * (465 - $len)
    $g.DrawLine($p, [float]$x1, [float]$y1, [float]$x2, [float]$y2)
}

$g.Dispose()
Save-ImageBoth "obsidian_dial" $dial

# --- 2. Obsidian Dial AOD (Pure Black AMOLED) ---
$dialAod = New-Object System.Drawing.Bitmap(1000, 1000, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gA = [System.Drawing.Graphics]::FromImage($dialAod)
$gA.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gA.Clear([System.Drawing.Color]::FromArgb(255, 0, 0, 0)) # AMOLED Black

# Simplified AOD Markers
$aodMarkerBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(180, 197, 205, 208))
for ($h = 1; $h -le 12; $h++) {
    if ($h -eq 6) { continue }
    $ang = [Math]::PI * 2 * ($h / 12.0) - [Math]::PI / 2
    $state = $gA.Save()
    $mx = 500 + [Math]::Cos($ang) * 420
    $my = 500 + [Math]::Sin($ang) * 420
    $gA.TranslateTransform($mx, $my)
    $gA.RotateTransform($h * 30.0)
    $gA.FillRectangle($aodMarkerBrush, -4, -22, 8, 44)
    $gA.Restore($state)
}
# AOD 5-min dots
$aodDotBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(120, 170, 182, 187))
for ($m = 0; $m -lt 60; $m += 5) {
    $ang = [Math]::PI * 2 * ($m / 60.0) - [Math]::PI / 2
    $x = 500 + [Math]::Cos($ang) * 460
    $y = 500 + [Math]::Sin($ang) * 460
    $gA.FillEllipse($aodDotBrush, [float]($x - 3), [float]($y - 3), 6, 6)
}
# Simplified subdial rings in AOD
$aodTrackPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(60, 170, 182, 187), 1.0)
$gA.DrawEllipse($aodTrackPen, 370, 580, 260, 260)
$gA.DrawEllipse($aodTrackPen, 180, 385, 210, 210)
$gA.Dispose()
Save-ImageBoth "obsidian_dial_aod" $dialAod

# --- 3. Brushed Platinum Dauphine Hour Hand (46 x 290, pivot at 259) ---
$hHand = New-Object System.Drawing.Bitmap(46, 290, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gh = [System.Drawing.Graphics]::FromImage($hHand)
$gh.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

$ghL = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 225, 232, 235))
$ghR = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 175, 185, 190))

# Dauphine polygon: Top tip at (23, 10), widest at (23, 190) with w=42, tail at (23, 280)
$gh.FillPolygon($ghL, @(
    (New-Object System.Drawing.PointF(23, 8)),
    (New-Object System.Drawing.PointF(2, 195)),
    (New-Object System.Drawing.PointF(23, 275))
))
$gh.FillPolygon($ghR, @(
    (New-Object System.Drawing.PointF(23, 8)),
    (New-Object System.Drawing.PointF(44, 195)),
    (New-Object System.Drawing.PointF(23, 275))
))
$gh.Dispose()
Save-ImageBoth "obsidian_hour" $hHand

# Hour Hand AOD
$hHandAod = New-Object System.Drawing.Bitmap(46, 290, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gha = [System.Drawing.Graphics]::FromImage($hHandAod)
$gha.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$penAod = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(240, 210, 220, 225), 2.5)
$gha.DrawPolygon($penAod, @(
    (New-Object System.Drawing.PointF(23, 8)),
    (New-Object System.Drawing.PointF(2, 195)),
    (New-Object System.Drawing.PointF(23, 275)),
    (New-Object System.Drawing.PointF(44, 195))
))
$gha.Dispose()
Save-ImageBoth "obsidian_hour_aod" $hHandAod

# --- 4. Brushed Platinum Dauphine Minute Hand (38 x 390, pivot at 359) ---
$mHand = New-Object System.Drawing.Bitmap(38, 390, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gm = [System.Drawing.Graphics]::FromImage($mHand)
$gm.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

$gm.FillPolygon($ghL, @(
    (New-Object System.Drawing.PointF(19, 8)),
    (New-Object System.Drawing.PointF(2, 275)),
    (New-Object System.Drawing.PointF(19, 375))
))
$gm.FillPolygon($ghR, @(
    (New-Object System.Drawing.PointF(19, 8)),
    (New-Object System.Drawing.PointF(36, 275)),
    (New-Object System.Drawing.PointF(19, 375))
))
$gm.Dispose()
Save-ImageBoth "obsidian_minute" $mHand

# Minute Hand AOD
$mHandAod = New-Object System.Drawing.Bitmap(38, 390, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gma = [System.Drawing.Graphics]::FromImage($mHandAod)
$gma.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gma.DrawPolygon($penAod, @(
    (New-Object System.Drawing.PointF(19, 8)),
    (New-Object System.Drawing.PointF(2, 275)),
    (New-Object System.Drawing.PointF(19, 375)),
    (New-Object System.Drawing.PointF(36, 275))
))
$gma.Dispose()
Save-ImageBoth "obsidian_minute_aod" $mHandAod

# --- 5. Small Seconds Needle (14 x 130, pivot at 104) ---
$sHand = New-Object System.Drawing.Bitmap(14, 130, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gs = [System.Drawing.Graphics]::FromImage($sHand)
$gs.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$sNeedleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 230, 235, 238))
$gs.FillPolygon($sNeedleBrush, @(
    (New-Object System.Drawing.PointF(7, 4)),
    (New-Object System.Drawing.PointF(4, 100)),
    (New-Object System.Drawing.PointF(7, 126)),
    (New-Object System.Drawing.PointF(10, 100))
))
$gs.FillEllipse($sNeedleBrush, 2, 99, 10, 10)
$gs.Dispose()
Save-ImageBoth "obsidian_small_sec" $sHand

# --- 6. Power Reserve Needle (14 x 100, pivot at 80) ---
$pHand = New-Object System.Drawing.Bitmap(14, 100, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gp = [System.Drawing.Graphics]::FromImage($pHand)
$gp.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$pNeedleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 119, 139, 134)) # Sage Grey
$gp.FillPolygon($pNeedleBrush, @(
    (New-Object System.Drawing.PointF(7, 4)),
    (New-Object System.Drawing.PointF(4, 76)),
    (New-Object System.Drawing.PointF(7, 96)),
    (New-Object System.Drawing.PointF(10, 76))
))
$gp.FillEllipse($ghL, 2, 75, 10, 10)
$gp.Dispose()
Save-ImageBoth "obsidian_power_hand" $pHand

# --- 7. Central Hub Cap (52 x 52) ---
$hub = New-Object System.Drawing.Bitmap(52, 52, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$ghub = [System.Drawing.Graphics]::FromImage($hub)
$ghub.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$hubPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$hubPath.AddEllipse(2, 2, 48, 48)
$hubBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush($hubPath)
$hubBrush.CenterColor = [System.Drawing.Color]::FromArgb(255, 240, 245, 248)
$hubBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 140, 150, 155))
$ghub.FillPath($hubBrush, $hubPath)
$ghub.DrawEllipse((New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 180, 190, 195), 2.0)), 2, 2, 48, 48)
$ghub.Dispose()
Save-ImageBoth "obsidian_hub" $hub

Write-Output "SUCCESS: All Obsidian Atelier mechanical assets successfully generated and deployed to both watchface and app modules!"
