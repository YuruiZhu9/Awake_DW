# Build additional local theme background candidates from the user-provided source images.
# Originals are never modified; the APK receives only small, deterministic JPEG derivatives.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent
$sourceRoot = Join-Path $root 'images/Lolita'
$outputRoot = Join-Path $root 'app/src/main/assets/lolita'
$jpeg = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
$quality = New-Object System.Drawing.Imaging.EncoderParameters 1
$quality.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), ([long]88)

function Find-Source([string]$suffix) {
    $files = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -File | Where-Object Name -Like "*$suffix.png")
    if ($files.Count -ne 1) { throw "Expected exactly one source for $suffix, found $($files.Count)" }
    return $files[0]
}

function New-FittedJpeg([string]$sourceSuffix, [string]$outputName, [string]$paperColor) {
    $sourceFile = Find-Source $sourceSuffix
    $source = [System.Drawing.Bitmap]::FromFile($sourceFile.FullName)
    $bitmap = New-Object System.Drawing.Bitmap 810,1440
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml($paperColor))
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $scale = [Math]::Max(810.0 / $source.Width, 1440.0 / $source.Height)
        $drawWidth = [int][Math]::Ceiling($source.Width * $scale)
        $drawHeight = [int][Math]::Ceiling($source.Height * $scale)
        $left = [int][Math]::Floor((810 - $drawWidth) / 2.0)
        $top = [int][Math]::Floor((1440 - $drawHeight) / 2.0)
        $graphics.DrawImage($source, $left, $top, $drawWidth, $drawHeight)
        $outputPath = Join-Path $outputRoot $outputName
        $bitmap.Save($outputPath, $jpeg, $quality)
        Write-Output ([PSCustomObject]@{ Name = $outputName; Source = $sourceFile.Name; Bytes = (Get-Item $outputPath).Length })
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
        $source.Dispose()
    }
}

# These are the extra frames selected from the already supplied pale blue, rose and warm paper art.
New-FittedJpeg '15_29_13' 'blue_alt.jpg' '#F7FAFD'
New-FittedJpeg '15_26_38' 'blue_soft.jpg' '#F7FAFD'
New-FittedJpeg '15_08_41' 'rose_soft.jpg' '#FEF9FA'
New-FittedJpeg '15_12_09' 'rose_alt.jpg' '#FEF9FA'
New-FittedJpeg '15_14_15' 'warm_alt.jpg' '#FBF8F4'
New-FittedJpeg '15_26_38' 'lavender.jpg' '#F8F6FB'
$quality.Dispose()
