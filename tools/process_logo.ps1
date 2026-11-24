Add-Type -AssemblyName System.Drawing

$srcPath = "F:\Projects\Nocturne\a_clean_app_icon_style_logo_on_a_dark_charcoal_rou.png"
if (-not (Test-Path $srcPath)) {
    Write-Error "Source file not found: $srcPath"
    exit 1
}

$img = [System.Drawing.Image]::FromFile($srcPath)
Write-Output "Image format: $($img.RawFormat), Size: $($img.Width) x $($img.Height)"
$img.Dispose()
