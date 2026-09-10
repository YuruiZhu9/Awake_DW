# Prepare the four user-supplied 2026-09-10 theme backgrounds for runtime use.
# Source PNG files stay untouched; APK assets are resized and JPEG-compressed.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path $PSScriptRoot -Parent
$sourceRoot = Join-Path $root 'images/Lolita/new'
$outputRoot = Join-Path $root 'app/src/main/assets/lolita'
$jpeg = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
$quality = New-Object System.Drawing.Imaging.EncoderParameters 1
$quality.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), ([long]86)

function Convert-ThemeArt([string]$sourceName, [string]$outputName) {
    $sourcePath = Join-Path $sourceRoot $sourceName
    if (-not (Test-Path -LiteralPath $sourcePath)) { throw "Missing source theme art: $sourcePath" }

    $source = [System.Drawing.Bitmap]::FromFile($sourcePath)
    $bitmap = New-Object System.Drawing.Bitmap 810,1440
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.DrawImage($source, 0, 0, 810, 1440)
        $outputPath = Join-Path $outputRoot $outputName
        $bitmap.Save($outputPath, $jpeg, $quality)
        $item = Get-Item -LiteralPath $outputPath
        if ($item.Length -ge 350000) { throw "$outputName exceeds the 350KB runtime budget: $($item.Length) bytes" }
        [PSCustomObject]@{ Name = $outputName; Width = 810; Height = 1440; Bytes = $item.Length }
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
        $source.Dispose()
    }
}

try {
    Convert-ThemeArt '晨雾蓝瓷.png' 'morning_blue_porcelain.jpg'
    Convert-ThemeArt '午后藕荷.png' 'afternoon_lotus.jpg'
    Convert-ThemeArt '黄昏奶茶.png' 'twilight_milk_tea.jpg'
    Convert-ThemeArt '雾紫玫瑰.png' 'mist_lavender_rose.jpg'
} finally {
    $quality.Dispose()
}
