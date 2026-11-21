Add-Type -AssemblyName System.Drawing

$fgBmp = [System.Drawing.Bitmap]::FromFile("F:\Progects-Android-Studio\Nocturne\design-assets\ic_launcher_foreground_1080.png")
$bgBmp = [System.Drawing.Bitmap]::FromFile("F:\Progects-Android-Studio\Nocturne\design-assets\ic_launcher_background_1080.png")

# Densities for legacy mipmaps (size of square icon)
$densities = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

# Function to generate rounded rectangle path
function Get-RoundedRectPath($rect, $radius) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $radius * 2
    $path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
    $path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
    $path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

$targets = @(
    "F:\Progects-Android-Studio\Nocturne\app\src\main\res",
    "F:\Progects-Android-Studio\Nocturne\wear-bridge\src\main\res"
)

foreach ($target in $targets) {
    # 1. Copy full-res foreground to drawable
    $drawDir = Join-Path $target "drawable"
    if (!(Test-Path $drawDir)) { New-Item -ItemType Directory -Force -Path $drawDir | Out-Null }

    # Save 432x432 (xxxhdpi adaptive foreground) and copy
    $fg432 = New-Object System.Drawing.Bitmap 432, 432, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g432 = [System.Drawing.Graphics]::FromImage($fg432)
    $g432.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g432.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g432.DrawImage($fgBmp, 0, 0, 432, 432)
    $g432.Dispose()
    $fg432.Save((Join-Path $drawDir "ic_launcher_foreground.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $fg432.Dispose()
    Write-Host "Updated ic_launcher_foreground.png in $drawDir"

    # 2. Generate legacy mipmaps: standard (squircle/rounded) and round (circle)
    foreach ($dens in $densities.Keys) {
        $size = $densities[$dens]
        $folder = Join-Path $target $dens
        if (!(Test-Path $folder)) { New-Item -ItemType Directory -Force -Path $folder | Out-Null }

        # --- Standard Legacy Icon (Subtle elegant rounded squircle, corner radius = 22% of size) ---
        $stdBmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $gStd = [System.Drawing.Graphics]::FromImage($stdBmp)
        $gStd.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $gStd.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $gStd.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

        $margin = [Math]::Max(1, [int]($size * 0.04))
        $innerSize = $size - 2 * $margin
        $rect = New-Object System.Drawing.Rectangle $margin, $margin, $innerSize, $innerSize
        $clipPath = Get-RoundedRectPath $rect ([int]($innerSize * 0.22))

        $gStd.SetClip($clipPath)
        $gStd.DrawImage($bgBmp, $rect, 0, 0, 1080, 1080, [System.Drawing.GraphicsUnit]::Pixel)
        # In adaptive, symbol is 700/1080 of canvas. In cropped legacy icon, draw symbol nicely centered
        $symScale = $innerSize / 1080.0 * 1.35
        $symW = [int](1080 * $symScale)
        $symX = [int](($size - $symW) / 2)
        $symY = [int](($size - $symW) / 2)
        $gStd.DrawImage($fgBmp, $symX, $symY, $symW, $symW)
        $gStd.ResetClip()
        $clipPath.Dispose()
        $gStd.Dispose()

        $stdPath = Join-Path $folder "ic_launcher.png"
        $stdBmp.Save($stdPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $stdBmp.Dispose()

        # --- Round Legacy Icon (Circle) ---
        $rndBmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $gRnd = [System.Drawing.Graphics]::FromImage($rndBmp)
        $gRnd.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $gRnd.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $gRnd.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

        $cMargin = [Math]::Max(1, [int]($size * 0.03))
        $cInner = $size - 2 * $cMargin
        $cRect = New-Object System.Drawing.Rectangle $cMargin, $cMargin, $cInner, $cInner
        $cPath = New-Object System.Drawing.Drawing2D.GraphicsPath
        $cPath.AddEllipse($cRect)

        $gRnd.SetClip($cPath)
        $gRnd.DrawImage($bgBmp, $cRect, 0, 0, 1080, 1080, [System.Drawing.GraphicsUnit]::Pixel)
        $gRnd.DrawImage($fgBmp, $symX, $symY, $symW, $symW)
        $gRnd.ResetClip()
        $cPath.Dispose()
        $gRnd.Dispose()

        $rndPath = Join-Path $folder "ic_launcher_round.png"
        $rndBmp.Save($rndPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $rndBmp.Dispose()
    }
}

$fgBmp.Dispose()
$bgBmp.Dispose()
Write-Host "All launcher icon densities generated successfully!"
