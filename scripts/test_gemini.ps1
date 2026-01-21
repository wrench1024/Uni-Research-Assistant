$url = "http://localhost:8000/api/v1/chat/stream"
$body = @{
    message = "Hello, tell me a short joke."
    history = @()
} | ConvertTo-Json

Write-Host "Sending request to $url..."
try {
    Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json"
} catch {
    Write-Error "Request failed: $_"
}
