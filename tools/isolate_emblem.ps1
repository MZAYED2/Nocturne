Add-Type -AssemblyName System.Drawing

$srcPath = "F:\Progects-Android-Studio\Nocturne\design-assets\nocturne_eclipse_layers_original.png"
$src = [System.Drawing.Bitmap]::FromFile($srcPath)

$w = $src.Width
$h = $src.Height
$cx = 512.0
$cy = 512.0

# 1. Create cleanly isolated 3D emblem (remove outer squircle and turquoise halo)
$isolated = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        $dx = $x - $cx
        $dy = $y - $cy
        $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)
        
        # Dial crescent radius extends up to ~402px
        # Fade out between 396 and 406 to remove the turquoise squircle rim
        $alphaFactor = 1.0
        if ($dist -gt 406.0) {
            $alphaFactor = 0.0
        } elseif ($dist -gt 394.0) {
            $alphaFactor = 1.0 - (($dist - 394.0) / 12.0)
        }
        
        if ($alphaFactor -gt 0.001) {
            $c = $src.GetPixel($x, $y)
            $newA = [int]([Math]::Round($c.A * $alphaFactor))
            if ($newA -gt 255) { $newA = 255 }
            $isolated.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($newA, $c.R, $c.G, $c.B))
        } else {
            $isolated.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        }
    }
}

$previewPath = "C:\Users\mohamed zayed\.gemini\antigravity-acp\brain\2d4c81b1-82e2-4031-b244-001e9e90dc8f\clean_emblem_preview.png"
$isolated.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output "Clean emblem isolated and saved to: $previewPath"

$src.Dispose()
$isolated.Dispose()
