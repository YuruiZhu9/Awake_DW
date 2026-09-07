# Rebuild optimized runtime art from user originals; originals are never modified.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Drawing;
public static class ThemeEdgeFeather {
    public static void Apply(Bitmap image, bool left, bool top) {
        const float edge = 28f;
        for (int y = 0; y < image.Height; y++) {
            for (int x = 0; x < image.Width; x++) {
                float factor = Math.Min(1f, Math.Min((image.Width - 1 - x) / edge, (image.Height - 1 - y) / edge));
                if (left) factor = Math.Min(factor, x / edge);
                if (top) factor = Math.Min(factor, y / edge);
                if (factor >= 1f) continue;
                Color c = image.GetPixel(x,y);
                image.SetPixel(x,y,Color.FromArgb((int)(c.A * Math.Max(0f,factor)),c.R,c.G,c.B));
            }
        }
    }
}
"@
$root = Split-Path $PSScriptRoot -Parent
$source = Join-Path $root 'images/Lolita'
$output = Join-Path $root 'app/src/main/assets/lolita'
$jpeg = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
$quality = New-Object System.Drawing.Imaging.EncoderParameters 1
$quality.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), ([long]88)
function Open-Source([string]$suffix) {
    $files = @(Get-ChildItem -LiteralPath $source -Recurse -File | Where-Object Name -Like "*$suffix.png")
    if ($files.Count -ne 1) { throw "Expected exactly one source for $suffix" }
    return [System.Drawing.Bitmap]::FromFile($files[0].FullName)
}
function New-Art([string]$color, [int]$width = 810, [int]$height = 1440) {
    $bitmap = New-Object System.Drawing.Bitmap $width,$height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml($color))
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    return @($bitmap, $graphics)
}
function Draw-Crop($graphics, $image, [int[]]$src, [int[]]$dst, [single]$opacity = 1) {
    $matrix = New-Object System.Drawing.Imaging.ColorMatrix
    $matrix.Matrix33 = $opacity
    $attrs = New-Object System.Drawing.Imaging.ImageAttributes
    $attrs.SetColorMatrix($matrix)
    $rect = New-Object System.Drawing.Rectangle $dst[0],$dst[1],$dst[2],$dst[3]
    if ($image.PixelFormat -eq [System.Drawing.Imaging.PixelFormat]::Format32bppArgb -and ($src[2] -ne $image.Width -or $src[3] -ne $image.Height)) {
        $crop = $image.Clone(([System.Drawing.Rectangle]::new($src[0],$src[1],$src[2],$src[3])),[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        [ThemeEdgeFeather]::Apply($crop,($src[0] -gt 0),($src[1] -gt 0))
        $graphics.DrawImage($crop,$rect,0,0,$crop.Width,$crop.Height,[System.Drawing.GraphicsUnit]::Pixel,$attrs)
        $crop.Dispose()
    } else {
        $graphics.DrawImage($image,$rect,$src[0],$src[1],$src[2],$src[3],[System.Drawing.GraphicsUnit]::Pixel,$attrs)
    }
    $attrs.Dispose()
}
$mint = Open-Source '17_12_20'
$cleric = Open-Source '17_12_08'
$gothic = Open-Source '17_12_02'
try {
    $bitmap,$g = New-Art '#E5F3EA'
    Draw-Crop $g $mint @(0,0,$mint.Width,$mint.Height) @(0,0,810,1440)
    $bitmap.Save((Join-Path $output 'thin_mint.jpg'),$jpeg,$quality)
    $g.Dispose(); $bitmap.Dispose()

    $bitmap,$g = New-Art '#F5F5F4' 960 1440
    # Preserve border proportions: prepare a full-width frame; runtime uses width-fit in portrait.
    # Transparent center remains cool ivory, not black (JPEG has no alpha).
    Draw-Crop $g $cleric @(0,0,1024,1536) @(0,0,960,1440)
    $bitmap.Save((Join-Path $output 'cleric.jpg'),$jpeg,$quality)
    $g.Dispose(); $bitmap.Dispose()

    $bitmap,$g = New-Art '#101014'
    # A side-lit charcoal fabric surface keeps black petals visible without inverting the art.
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush ([System.Drawing.Point]::new(0,0)),([System.Drawing.Point]::new(810,0)),([System.Drawing.ColorTranslator]::FromHtml('#424047')),([System.Drawing.ColorTranslator]::FromHtml('#17171D'))
    $g.FillRectangle($brush,0,0,810,1440); $brush.Dispose()
    Draw-Crop $g $gothic @(0,0,335,650) @(0,0,248,481) 0.92
    Draw-Crop $g $gothic @(0,650,335,886) @(0,753,260,687) 0.90
    Draw-Crop $g $gothic @(564,65,252,510) @(660,22,126,255) 0.78
    Draw-Crop $g $gothic @(875,640,149,495) @(723,1060,87,289) 0.84
    $bitmap.Save((Join-Path $output 'gothic_frame.jpg'),$jpeg,$quality)
    $g.Dispose(); $bitmap.Dispose()
} finally {
    $mint.Dispose(); $cleric.Dispose(); $gothic.Dispose(); $quality.Dispose()
}
Get-ChildItem -LiteralPath $output -File | Where-Object Name -in @('thin_mint.jpg','cleric.jpg','gothic_frame.jpg') | Select-Object Name,Length

