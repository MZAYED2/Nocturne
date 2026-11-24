<#
.SYNOPSIS
    Publishes a GitHub release and uploads all assets from a release directory.
.PARAMETER Tag
    The release tag, e.g. v1.0.8-beta.1
#>
param(
    [string]$Tag = "v1.0.8-beta.1"
)

$inputStr = "protocol=https`nhost=github.com`n"
$cred = $inputStr | git credential fill
$token = ($cred | Where-Object { $_ -match "^password=" }).Substring(9).Trim()

$releaseDir = "release-$Tag"
if (-not (Test-Path $releaseDir)) {
    Write-Error "Release directory $releaseDir does not exist."
    exit 1
}

$notes = ""
if (Test-Path "$releaseDir/RELEASE_NOTES.md") {
    $notes = [System.IO.File]::ReadAllText((Resolve-Path "$releaseDir/RELEASE_NOTES.md"), [System.Text.Encoding]::UTF8)
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Accept"        = "application/vnd.github+json"
    "User-Agent"    = "PowerShell-Nocturne-Release"
    "X-GitHub-Api-Version" = "2022-11-28"
}

Write-Host "Checking release for $Tag..."
$release = $null
try {
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/MZAYED2/Nocturne/releases/tags/$Tag" -Headers $headers -Method Get
    Write-Host "Found existing release ID: $($release.id)"
} catch {
    Write-Host "Release does not exist. Creating..."
    $payloadObj = [ordered]@{
        tag_name         = $Tag
        target_commitish = "main"
        name             = "Nocturne $Tag (Beta Release)"
        body             = $notes
        draft            = $false
        prerelease       = $true
    }
    $json = $payloadObj | ConvertTo-Json
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)

    $req = [System.Net.HttpWebRequest]::Create("https://api.github.com/repos/MZAYED2/Nocturne/releases")
    $req.Method = "POST"
    $req.Headers["Authorization"] = "Bearer $token"
    $req.Headers["X-GitHub-Api-Version"] = "2022-11-28"
    $req.Accept = "application/vnd.github+json"
    $req.UserAgent = "PowerShell-Nocturne-Release"
    $req.ContentType = "application/json; charset=utf-8"
    $req.ContentLength = $bytes.Length
    $os = $req.GetRequestStream()
    $os.Write($bytes, 0, $bytes.Length)
    $os.Close()
    $resp = $req.GetResponse()
    $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
    $release = $sr.ReadToEnd() | ConvertFrom-Json
    $resp.Close()
    Write-Host "Created release ID: $($release.id)"
}

# Upload assets
$existingAssetsReq = [System.Net.HttpWebRequest]::Create("https://api.github.com/repos/MZAYED2/Nocturne/releases/$($release.id)/assets")
$existingAssetsReq.Method = "GET"
$existingAssetsReq.Headers["Authorization"] = "Bearer $token"
$existingAssetsReq.Headers["X-GitHub-Api-Version"] = "2022-11-28"
$existingAssetsReq.Accept = "application/vnd.github+json"
$existingAssetsReq.UserAgent = "PowerShell-Nocturne-Release"
$resp = $existingAssetsReq.GetResponse()
$sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
$existingList = $sr.ReadToEnd() | ConvertFrom-Json
$resp.Close()

$existingMap = @{}
foreach ($item in $existingList) { $existingMap[$item.name] = $item.id }

$filesToUpload = Get-ChildItem $releaseDir -File | Where-Object { $_.Name -ne "RELEASE_NOTES.md" }

foreach ($f in $filesToUpload) {
    if ($existingMap.ContainsKey($f.Name)) {
        Write-Host "Asset $($f.Name) already uploaded. Skipping."
        continue
    }

    Write-Host "Uploading $($f.Name) ($($f.Length) bytes)..."
    $contentType = switch ($f.Extension.ToLower()) {
        ".apk"  { "application/vnd.android.package-archive" }
        ".json" { "application/json" }
        ".txt"  { "text/plain" }
        ".md"   { "text/markdown" }
        default { "application/octet-stream" }
    }

    $uploadUri = "https://uploads.github.com/repos/MZAYED2/Nocturne/releases/$($release.id)/assets?name=$($f.Name)"
    $upReq = [System.Net.HttpWebRequest]::Create($uploadUri)
    $upReq.Method = "POST"
    $upReq.Headers["Authorization"] = "Bearer $token"
    $upReq.Headers["X-GitHub-Api-Version"] = "2022-11-28"
    $upReq.Accept = "application/vnd.github+json"
    $upReq.UserAgent = "PowerShell-Nocturne-Release"
    $upReq.ContentType = $contentType
    $upReq.ContentLength = $f.Length
    $upReq.Timeout = 600000
    $upReq.ReadWriteTimeout = 600000

    $fileStream = [System.IO.File]::OpenRead($f.FullName)
    $reqStream = $upReq.GetRequestStream()
    $buffer = New-Object byte[] 65536
    while (($bytesRead = $fileStream.Read($buffer, 0, $buffer.Length)) -gt 0) {
        $reqStream.Write($buffer, 0, $bytesRead)
    }
    $fileStream.Close()
    $reqStream.Close()

    $upResp = $upReq.GetResponse()
    $upResp.Close()
    Write-Host "Uploaded $($f.Name) successfully!"
}

Write-Host "Done! Release URL: $($release.html_url)"
