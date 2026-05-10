param(
    [string]$OutputDirectory = $PSScriptRoot
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
}

Add-Type -AssemblyName System.Drawing

function New-RoundedRectanglePath {
    param(
        [System.Drawing.RectangleF]$Rectangle,
        [float]$Radius
    )

    $diameter = $Radius * 2
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc($Rectangle.X, $Rectangle.Y, $diameter, $diameter, 180, 90)
    $path.AddArc($Rectangle.Right - $diameter, $Rectangle.Y, $diameter, $diameter, 270, 90)
    $path.AddArc($Rectangle.Right - $diameter, $Rectangle.Bottom - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($Rectangle.X, $Rectangle.Bottom - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-JavaPosIcon {
    param(
        [string]$IcoPath,
        [string]$PreviewPath
    )

    $size = 256
    $bitmap = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    $background = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Rectangle(0, 0, $size, $size)),
        [System.Drawing.Color]::FromArgb(18, 65, 86),
        [System.Drawing.Color]::FromArgb(32, 111, 100),
        45
    )
    $graphics.FillRectangle($background, 0, 0, $size, $size)

    $tilePath = New-RoundedRectanglePath -Rectangle (New-Object System.Drawing.RectangleF(38, 42, 180, 172)) -Radius 30
    $tileBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(246, 248, 241))
    $graphics.FillPath($tileBrush, $tilePath)

    $accentBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(232, 164, 63))
    $screenPath = New-RoundedRectanglePath -Rectangle (New-Object System.Drawing.RectangleF(64, 66, 128, 50)) -Radius 13
    $graphics.FillPath($accentBrush, $screenPath)

    $linePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(18, 65, 86), 9)
    $graphics.DrawLine($linePen, 74, 146, 182, 146)
    $graphics.DrawLine($linePen, 74, 172, 182, 172)

    $font = New-Object System.Drawing.Font("Segoe UI", 34, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(18, 65, 86))
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $graphics.DrawString("POS", $font, $textBrush, (New-Object System.Drawing.RectangleF(58, 65, 140, 50)), $format)

    $bitmap.Save($PreviewPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $pngStream = New-Object System.IO.MemoryStream
    $bitmap.Save($pngStream, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngBytes = $pngStream.ToArray()

    $fileStream = [System.IO.File]::Create($IcoPath)
    $writer = New-Object System.IO.BinaryWriter($fileStream)
    $writer.Write([UInt16]0)
    $writer.Write([UInt16]1)
    $writer.Write([UInt16]1)
    $writer.Write([byte]0)
    $writer.Write([byte]0)
    $writer.Write([byte]0)
    $writer.Write([byte]0)
    $writer.Write([UInt16]1)
    $writer.Write([UInt16]32)
    $writer.Write([UInt32]$pngBytes.Length)
    $writer.Write([UInt32]22)
    $writer.Write($pngBytes)
    $writer.Close()

    $format.Dispose()
    $font.Dispose()
    $linePen.Dispose()
    $accentBrush.Dispose()
    $textBrush.Dispose()
    $tileBrush.Dispose()
    $tilePath.Dispose()
    $screenPath.Dispose()
    $background.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
    $pngStream.Dispose()
}

New-JavaPosIcon `
    -IcoPath (Join-Path $OutputDirectory "JavaPOS.ico") `
    -PreviewPath (Join-Path $OutputDirectory "JavaPOS-icon-preview.png")

Write-Host "Installer assets generated in $OutputDirectory"
