Add-Type -AssemblyName System.Drawing

$srcPath = "F:\Projects\Nocturne\a_clean_app_icon_style_logo_on_a_dark_charcoal_rou.png"
if (-not (Test-Path $srcPath)) {
    Write-Error "Source file not found: $srcPath"
    exit 1
}

$srcBmp = [System.Drawing.Bitmap]::FromFile($srcPath)

function Resize-Image {
    param(
        [System.Drawing.Bitmap]$source,
        [int]$targetWidth,
        [int]$targetHeight,
        [string]$outputPath,
        [bool]$makeCircular = $false
    )

    $targetBmp = New-Object System.Drawing.Bitmap($targetWidth, $targetHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($targetBmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    if ($makeCircular) {
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddEllipse(0, 0, $targetWidth, $targetHeight)
        $g.SetClip($path)
    }

    $destRect = New-Object System.Drawing.Rectangle(0, 0, $targetWidth, $targetHeight)
    $g.DrawImage($source, $destRect, 0, 0, $source.Width, $source.Height, [System.Drawing.GraphicsUnit]::Pixel)

    $g.Dispose()

    $dir = [System.IO.Path]::GetDirectoryName($outputPath)
    if (-not (Test-Path $dir)) {
        [System.IO.Directory]::CreateDirectory($dir) | Out-Null
    }

    $targetBmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $targetBmp.Dispose()
    Write-Output "Generated: $outputPath ($($targetWidth)x$($targetHeight))"
}

function Generate-Foreground {
    param(
        [System.Drawing.Bitmap]$source,
        [int]$canvasSize,
        [double]$scaleRatio,
        [string]$outputPath
    )

    $targetBmp = New-Object System.Drawing.Bitmap($canvasSize, $canvasSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($targetBmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $iconSize = [int]($canvasSize * $scaleRatio)
    $offset = [int](($canvasSize - $iconSize) / 2)

    $destRect = New-Object System.Drawing.Rectangle($offset, $offset, $iconSize, $iconSize)
    $g.DrawImage($source, $destRect, 0, 0, $source.Width, $source.Height, [System.Drawing.GraphicsUnit]::Pixel)

    $g.Dispose()

    $dir = [System.IO.Path]::GetDirectoryName($outputPath)
    if (-not (Test-Path $dir)) {
        [System.IO.Directory]::CreateDirectory($dir) | Out-Null
    }

    $targetBmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $targetBmp.Dispose()
    Write-Output "Generated Foreground: $outputPath ($($canvasSize)x$($canvasSize), iconSize=$iconSize)"
}

# 1. Master logo copies
Resize-Image $srcBmp 512 512 "F:\Projects\Nocturne\app\src\main\res\drawable\nocturne_logo.png"
Resize-Image $srcBmp 512 512 "F:\Projects\Nocturne\design-assets\artwork\nocturne_master_logo.png"

# 2. Adaptive Foregrounds (1080x1080 with 0.68 safe-zone scale so it fits standard adaptive circle/squircle)
Generate-Foreground $srcBmp 1080 0.68 "F:\Projects\Nocturne\app\src\main\res\drawable\ic_launcher_foreground.png"
Generate-Foreground $srcBmp 1080 0.68 "F:\Projects\Nocturne\wear-bridge\src\main\res\drawable\ic_launcher_foreground.png"

# 3. App mipmaps
$densities = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

foreach ($density in $densities.Keys) {
    $size = $densities[$density]

    # App
    Resize-Image $srcBmp $size $size "F:\Projects\Nocturne\app\src\main\res\mipmap-$density\ic_launcher.png"
    Resize-Image $srcBmp $size $size "F:\Projects\Nocturne\app\src\main\res\mipmap-$density\ic_launcher_round.png" -makeCircular $true

    # Wear Bridge
    Resize-Image $srcBmp $size $size "F:\Projects\Nocturne\wear-bridge\src\main\res\mipmap-$density\ic_launcher.png"
    Resize-Image $srcBmp $size $size "F:\Projects\Nocturne\wear-bridge\src\main\res\mipmap-$density\ic_launcher_round.png" -makeCircular $true
}

$srcBmp.Dispose()
Write-Output "All icons successfully generated!"
