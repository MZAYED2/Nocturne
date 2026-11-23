Add-Type -AssemblyName System.Drawing

$artDir = 'C:\Users\mohamed zayed\.gemini\antigravity-acp\brain\2d4c81b1-82e2-4031-b244-001e9e90dc8f'
$resDir = "$PSScriptRoot\..\app\src\main\res\drawable"

# 1. Render Obsidian Active
$bmpActive = New-Object System.Drawing.Bitmap 1000, 1000
$g = [System.Drawing.Graphics]::FromImage($bmpActive)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.Clear([System.Drawing.Color]::Black)

$dial = [System.Drawing.Image]::FromFile("$resDir\obsidian_dial.png")
$g.DrawImage($dial, 0, 0, 1000, 1000)
$dial.Dispose()

# Date text at 690, 650
$font = New-Object System.Drawing.Font('Arial', [float]24, [System.Drawing.FontStyle]::Bold)
$brush = New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml('#E7E5DE'))
$sf = New-Object System.Drawing.StringFormat
$sf.Alignment = [System.Drawing.StringAlignment]::Center
$g.DrawString('21', $font, $brush, [float]690, [float]650, $sf)

# Small sec needle at 500, 710 (rotation 120 deg)
$secImg = [System.Drawing.Image]::FromFile("$resDir\obsidian_small_sec.png")
$state = $g.Save()
$g.TranslateTransform(500, 710)
$g.RotateTransform(120)
$g.DrawImage($secImg, -7, -104, 14, 130)
$g.Restore($state)
$secImg.Dispose()

# Power hand at 285, 520 (rotation 35 deg)
$powImg = [System.Drawing.Image]::FromFile("$resDir\obsidian_power_hand.png")
$state = $g.Save()
$g.TranslateTransform(285, 520)
$g.RotateTransform(35)
$g.DrawImage($powImg, -7, -80, 14, 100)
$g.Restore($state)
$powImg.Dispose()

# Hour hand at 500, 500 (10:10 -> 305 deg)
$hourImg = [System.Drawing.Image]::FromFile("$resDir\obsidian_hour.png")
$state = $g.Save()
$g.TranslateTransform(500, 500)
$g.RotateTransform(305)
$g.DrawImage($hourImg, -23, -259, 46, 290)
$g.Restore($state)
$hourImg.Dispose()

# Minute hand at 500, 500 (10:10 -> 60 deg)
$minImg = [System.Drawing.Image]::FromFile("$resDir\obsidian_minute.png")
$state = $g.Save()
$g.TranslateTransform(500, 500)
$g.RotateTransform(60)
$g.DrawImage($minImg, -19, -359, 38, 390)
$g.Restore($state)
$minImg.Dispose()

# Hub
$hubImg = [System.Drawing.Image]::FromFile("$resDir\obsidian_hub.png")
$g.DrawImage($hubImg, 474, 474, 52, 52)
$hubImg.Dispose()

$g.Dispose()
$bmpActive.Save("$artDir\preview_obsidian_active.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmpActive.Dispose()

# 2. Render Obsidian AOD
$bmpAod = New-Object System.Drawing.Bitmap 1000, 1000
$g2 = [System.Drawing.Graphics]::FromImage($bmpAod)
$g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g2.Clear([System.Drawing.Color]::Black)

$dialAod = [System.Drawing.Image]::FromFile("$resDir\obsidian_dial_aod.png")
$g2.DrawImage($dialAod, 0, 0, 1000, 1000)
$dialAod.Dispose()

$brushAod = New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml('#AAB6BB'))
$g2.DrawString('21', $font, $brushAod, [float]690, [float]650, $sf)

# Hour hand AOD
$hourAod = [System.Drawing.Image]::FromFile("$resDir\obsidian_hour_aod.png")
$state = $g2.Save()
$g2.TranslateTransform(500, 500)
$g2.RotateTransform(305)
$g2.DrawImage($hourAod, -23, -259, 46, 290)
$g2.Restore($state)
$hourAod.Dispose()

# Minute hand AOD
$minAod = [System.Drawing.Image]::FromFile("$resDir\obsidian_minute_aod.png")
$state = $g2.Save()
$g2.TranslateTransform(500, 500)
$g2.RotateTransform(60)
$g2.DrawImage($minAod, -19, -359, 38, 390)
$g2.Restore($state)
$minAod.Dispose()

# Hub
$hubImg2 = [System.Drawing.Image]::FromFile("$resDir\obsidian_hub.png")
$g2.DrawImage($hubImg2, 474, 474, 52, 52)
$hubImg2.Dispose()

$g2.Dispose()
$bmpAod.Save("$artDir\preview_obsidian_aod.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmpAod.Dispose()

Write-Host "SUCCESS: Saved preview_obsidian_active.png and preview_obsidian_aod.png"
