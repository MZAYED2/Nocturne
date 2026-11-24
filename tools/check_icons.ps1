Add-Type -AssemblyName System.Drawing

$files = @(
    "F:\Projects\Nocturne\app\src\main\res\drawable\ic_launcher_foreground.png",
    "F:\Projects\Nocturne\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png",
    "F:\Projects\Nocturne\wear-bridge\src\main\res\drawable\ic_launcher_foreground.png"
)

foreach ($f in $files) {
    if (Test-Path $f) {
        $img = [System.Drawing.Image]::FromFile($f)
        Write-Output "$f -> $($img.Width) x $($img.Height)"
        $img.Dispose()
    } else {
        Write-Output "Missing: $f"
    }
}
