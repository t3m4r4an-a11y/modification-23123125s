# Collect all source files from GitHub repository with full content
# Usage: .\collect-raw-sources.ps1

param(
    [string]$Owner = "t3m4r4an-a11y",
    [string]$Repo = "modification-23123125s",
    [string]$Branch = "main",
    [string]$OutputFile = "all-sources.txt"
)

$ErrorActionPreference = "SilentlyContinue"

$token = $env:GITHUB_TOKEN
if (-not $token) {
    Write-Host "⚠️  GITHUB_TOKEN не установлен!" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "token $token"
    "Accept" = "application/vnd.github.v3+json"
}

# Расширения, которые НЕ читаем
$excludeExtensions = @(
    ".ogg", ".mp3", ".wav", ".flac", ".aac",
    ".ttf", ".otf", ".woff", ".woff2",
    ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".svg",
    ".zip", ".rar", ".7z", ".tar", ".gz",
    ".dll", ".exe", ".so", ".o", ".a",
    ".class", ".pyc", ".pyo",
    ".pdf", ".doc", ".docx", ".xls", ".xlsx"
)

function Get-AllFiles {
    param(
        [string]$Path = "",
        [string]$Owner,
        [string]$Repo,
        [string]$Branch
    )
    
    $url = "https://api.github.com/repos/$Owner/$Repo/contents/$Path`?ref=$Branch"
    
    try {
        $response = Invoke-RestMethod -Uri $url -Headers $headers -ErrorAction Stop
        
        $files = @()
        foreach ($item in $response) {
            if ($item.type -eq "file") {
                $files += @{
                    path = $item.path
                    download_url = $item.download_url
                    size = $item.size
                }
            }
            elseif ($item.type -eq "dir") {
                $files += Get-AllFiles -Path $item.path -Owner $Owner -Repo $Repo -Branch $Branch
            }
        }
        return $files
    }
    catch {
        Write-Host "❌ Ошибка при запросе $url" -ForegroundColor Red
        return @()
    }
}

function Should-IncludeFile {
    param([string]$FilePath)
    
    foreach ($ext in $excludeExtensions) {
        if ($FilePath.EndsWith($ext, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $false
        }
    }
    return $true
}

function Get-FileContent {
    param([string]$Url)
    
    try {
        $content = Invoke-RestMethod -Uri $Url -Headers @{"Accept" = "application/vnd.github.v3.raw"} -ErrorAction Stop
        return $content
    }
    catch {
        return $null
    }
}

Write-Host "🔍 Извлекаю все файлы из $Owner/$Repo..." -ForegroundColor Cyan

$allFiles = @(Get-AllFiles -Owner $Owner -Repo $Repo -Branch $Branch | Where-Object { Should-IncludeFile $_.path })

Write-Host "📦 Найдено файлов: $($allFiles.Count)" -ForegroundColor Green
Write-Host "💾 Сохраняю контент в $OutputFile..." -ForegroundColor Cyan

# Используем StreamWriter для надежного сохранения
$stream = [System.IO.StreamWriter]::new($OutputFile, $false, [System.Text.Encoding]::UTF8)

$stream.WriteLine("=== ALL SOURCES FROM $Owner/$Repo ===")
$stream.WriteLine("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$stream.WriteLine("Total files: $($allFiles.Count)")
$stream.WriteLine("")

$counter = 0
foreach ($file in $allFiles) {
    $counter++
    Write-Progress -Activity "Обработка файлов" -Status "$counter/$($allFiles.Count)" -PercentComplete ($counter / $allFiles.Count * 100)
    
    Write-Host "  [$counter/$($allFiles.Count)] $($file.path)" -ForegroundColor Gray
    
    $stream.WriteLine("")
    $stream.WriteLine("════════════════════════════════════════════════════════════════")
    $stream.WriteLine("FILE: $($file.path)")
    $stream.WriteLine("SOURCE: https://github.com/$Owner/$Repo/blob/$Branch/$($file.path)")
    $stream.WriteLine("RAW: $($file.download_url)")
    $stream.WriteLine("════════════════════════════════════════════════════════════════")
    $stream.WriteLine("")
    
    $content = Get-FileContent -Url $file.download_url
    if ($content) {
        $stream.WriteLine($content)
    }
    else {
        $stream.WriteLine("[⚠️  Не удалось загрузить содержимое]")
    }
    $stream.WriteLine("")
}

$stream.Close()
$stream.Dispose()

Write-Progress -Completed

Write-Host "`n✅ Готово!" -ForegroundColor Green
Write-Host "📄 Результат сохранен в: $OutputFile" -ForegroundColor Yellow
$fileInfo = Get-Item $OutputFile
Write-Host "📊 Размер файла: $('{0:N2}' -f ($fileInfo.Length / 1MB)) МБ"
