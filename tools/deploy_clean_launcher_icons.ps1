Add-Type -AssemblyName System.Drawing

$cleanEmblemPath = "C:\Users\mohamed zayed\.gemini\antigravity-acp\brain\2d4c81b1-82e2-4031-b244-001e9e90dc8f\clean_emblem_preview.png"
$emblem = [System.Drawing.Bitmap]::FromFile($cleanEmblemPath)

# 1. Generate 1080x1080 Foreground PNG for Adaptive Icon
$fg1080 = New-Object System.Drawing.Bitmap(1080, 1080, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($fg1080)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

# Emblem fits inside 72dp safe zone -> 720px / 1080px
$targetSize = 740
$destX = [int]((1080 - $targetSize) / 2)
$destY = [int]((1080 - $targetSize) / 2)
$g.DrawImage($emblem, $destX, $destY, $targetSize, $targetSize)
$g.Dispose()

$fgPathApp = "F:\Progects-Android-Studio\Nocturne\app\src\main\res\drawable\ic_launcher_foreground.png"
$fgPathWear = "F:\Progects-Android-Studio\Nocturne\wear-bridge\src\main\res\drawable\ic_launcher_foreground.png"
$fg1080.Save($fgPathApp, [System.Drawing.Imaging.ImageFormat]::Png)
$fg1080.Save($fgPathWear, [System.Drawing.Imaging.ImageFormat]::Png)

# 2. Generate Mipmap densities
$densities = @(
    @{ name = "mdpi"; size = 48 },
    @{ name = "hdpi"; size = 72 },
    @{ name = "xhdpi"; size = 96 },
    @{ name = "xxhdpi"; size = 144 },
    @{ name = "xxxhdpi"; size = 192 }
)

$modules = @(
    "F:\Progects-Android-Studio\Nocturne\app\src\main\res",
    "F:\Progects-Android-Studio\Nocturne\wear-bridge\src\main\res"
)

foreach ($mod in $modules) {
    foreach ($d in $densities) {
        $dim = $d.size
        $folder = Join-Path $mod "mipmap-$($d.name)"
        if (!(Test-Path $folder)) { New-Item -ItemType Directory -Force -Path $folder | Out-Null }
        
        # --- Square / Squircle Icon ---
        $bmpSquare = New-Object System.Drawing.Bitmap($dim, $dim, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $gS = [System.Drawing.Graphics]::FromImage($bmpSquare)
        $gS.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $gS.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        
        # Draw Midnight Navy background
        $rect = New-Object System.Drawing.Rectangle(0, 0, $dim, $dim)
        $pathSquircle = New-Object System.Drawing.Drawing2D.GraphicsPath
        $radius = [int]($dim * 0.22)
        $pathSquircle.AddArc(0, 0, $radius * 2, $radius * 2, 180, 90)
        $pathSquircle.AddArc($dim - $radius * 2, 0, $radius * 2, $radius * 2, 270, 90)
        $pathSquircle.AddArc($dim - $radius * 2, $dim - $radius * 2, $radius * 2, $radius * 2, 0, 90)
        $pathSquircle.AddArc(0, $dim - $radius * 2, $radius * 2, $radius * 2, 90, 90)
        $pathSquircle.CloseFigure()
        
        $brushBg = New-Object System.Drawing.Drawing2D.PathGradientBrush($pathSquircle)
        $brushBg.CenterColor = [System.Drawing.Color]::FromArgb(255, 13, 38, 51) # #0D2633
        $brushBg.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 3, 12, 19)) # #030C13
        $gS.FillPath($brushBg, $pathSquircle)
        
        # Draw Emblem
        $embSize = [int]($dim * 0.82)
        $embOffset = [int](($dim - $embSize) / 2)
        $gS.DrawImage($emblem, $embOffset, $embOffset, $embSize, $embSize)
        $gS.Dispose()
        $bmpSquare.Save((Join-Path $folder "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $bmpSquare.Dispose()
        
        # --- Round Icon ---
        $bmpRound = New-Object System.Drawing.Bitmap($dim, $dim, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $gR = [System.Drawing.Graphics]::FromImage($bmpRound)
        $gR.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $gR.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        
        $pathRound = New-Object System.Drawing.Drawing2D.GraphicsPath
        $pathRound.AddEllipse(0, 0, $dim, $dim)
        
        $brushRound = New-Object System.Drawing.Drawing2D.PathGradientBrush($pathRound)
        $brushRound.CenterColor = [System.Drawing.Color]::FromArgb(255, 13, 38, 51)
        $brushRound.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 3, 12, 19))
        $gR.FillPath($brushRound, $pathRound)
        
        $gR.DrawImage($emblem, $embOffset, $embOffset, $embSize, $embSize)
        $gR.Dispose()
        $bmpRound.Save((Join-Path $folder "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $bmpRound.Dispose()
    }
}

$emblem.Dispose()
$fg1080.Dispose()
Write-Output "All adaptive and mipmap launcher icons successfully generated and deployed!"
