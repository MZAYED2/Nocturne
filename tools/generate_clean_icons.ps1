Add-Type -AssemblyName System.Drawing

$srcFile = "$PSScriptRoot\..\design-assets\artwork\nocturne_eclipse_layers_original.png"
$bmp = [System.Drawing.Bitmap]::FromFile($srcFile)

# The center of the symbol is at (512, 512)
# Let's inspect the exact outer boundary of the crescent layers.
# The outermost crescent edge on top is at around y = 135 (dy = -377)
# On the left at around x = 135 (dx = -377)
# On the bottom right, the silver crescent tip ends at r ≈ 375
# Let's create a high-resolution foreground bitmap with transparent background.

$fgSize = 1080
$fg = New-Object System.Drawing.Bitmap $fgSize, $fgSize, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gFg = [System.Drawing.Graphics]::FromImage($fg)
$gFg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gFg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gFg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

# We crop the circle from the original image containing the crescents and dial
# Center is (512, 512), radius is ~382 pixels
$cropR = 382.0
$cropBmp = New-Object System.Drawing.Bitmap ([int]($cropR * 2)), ([int]($cropR * 2)), ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

for ($y = 0; $y -lt $cropBmp.Height; $y++) {
    $srcY = [int](512 - $cropR + $y)
    $dy = $y + 0.5 - $cropR
    for ($x = 0; $x -lt $cropBmp.Width; $x++) {
        $srcX = [int](512 - $cropR + $x)
        $dx = $x + 0.5 - $cropR
        $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)

        if ($srcX -ge 0 -and $srcX -lt 1024 -and $srcY -ge 0 -and $srcY -lt 1024) {
            $p = $bmp.GetPixel($srcX, $srcY)

            # Anti-aliasing at outer edge of crescents ($cropR)
            if ($dist -gt $cropR) {
                $cropBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            } elseif ($dist -gt ($cropR - 2.0)) {
                $alpha = [int](($cropR - $dist) / 2.0 * 255.0)
                $cropBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $p.R, $p.G, $p.B))
            } else {
                $cropBmp.SetPixel($x, $y, $p)
            }
        }
    }
}

# The safe zone for 1080x1080 canvas is 720x720 (circle diameter 720).
# Let's size the symbol to diameter 700 pixels (safe, majestic, well-proportioned).
$targetDiameter = 700
$targetX = [int]((1080 - $targetDiameter) / 2)
$targetY = [int]((1080 - $targetDiameter) / 2)

$gFg.DrawImage($cropBmp, $targetX, $targetY, $targetDiameter, $targetDiameter)
$gFg.Dispose()
$cropBmp.Dispose()

# Save Foreground
$fgPath = "$PSScriptRoot\..\design-assets\artwork\ic_launcher_foreground_1080.png"
$fg.Save($fgPath, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "Generated Foreground: $fgPath"

# Background: Full-bleed Midnight Navy gradient matching the image
$bg = New-Object System.Drawing.Bitmap $fgSize, $fgSize, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gBg = [System.Drawing.Graphics]::FromImage($bg)
$gBg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gBg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

$rect = New-Object System.Drawing.Rectangle 0, 0, $fgSize, $fgSize
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$path.AddEllipse(0, 0, $fgSize, $fgSize)
$pgb = New-Object System.Drawing.Drawing2D.PathGradientBrush $path
$pgb.CenterPoint = New-Object System.Drawing.PointF 540, 500
$pgb.CenterColor = [System.Drawing.Color]::FromArgb(255, 12, 34, 46) # Midnight Navy with subtle cyan depth
$pgb.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 4, 13, 20)) # Deep pitch navy edge

$gBg.FillRectangle($pgb, $rect)
$pgb.Dispose()
$path.Dispose()
$gBg.Dispose()

$bgPath = "$PSScriptRoot\..\design-assets\artwork\ic_launcher_background_1080.png"
$bg.Save($bgPath, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "Generated Background: $bgPath"

$bmp.Dispose()
$fg.Dispose()
$bg.Dispose()
