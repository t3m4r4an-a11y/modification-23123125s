# Extract all file links from GitHub repository
# Usage: .\extract-links.ps1 -Owner "t3m4r4an-a11y" -Repo "modification-23123125s" -Branch "main"

param(
    [string]$Owner = "t3m4r4an-a11y",
    [string]$Repo = "modification-23123125s",
    [string]$Branch = "main"
)

$ErrorActionPreference = "Stop"

# GitHub API token (optional, set via environment variable)
$token = $env:GITHUB_TOKEN
$headers = @{ "Accept" = "application/vnd.github.v3+json" }
if ($token) { $headers["Authorization"] = "token $token" }

# Function to recursively get all files
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
                    html_url = $item.html_url
                    download_url = $item.download_url
                }
            }
            elseif ($item.type -eq "dir") {
                $files += Get-AllFiles -Path $item.path -Owner $Owner -Repo $Repo -Branch $Branch
            }
        }
        return $files
    }
    catch {
        Write-Error "Error fetching $url : $_"
        return @()
    }
}

Write-Host "Extracting all file links from $Owner/$Repo (branch: $Branch)..." -ForegroundColor Cyan

$allFiles = Get-AllFiles -Owner $Owner -Repo $Repo -Branch $Branch

Write-Host "`n=== RAW LINKS ===" -ForegroundColor Green
foreach ($file in $allFiles) {
    Write-Host $file.download_url
}

Write-Host "`n=== HTML LINKS ===" -ForegroundColor Green
foreach ($file in $allFiles) {
    Write-Host $file.html_url
}

Write-Host "`n=== SUMMARY ===" -ForegroundColor Cyan
Write-Host "Total files found: $($allFiles.Count)"
