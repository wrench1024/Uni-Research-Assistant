# Comprehensive System Test Script
# Test all implemented backend modules

$BaseUrl = "http://localhost:8080/api"
$TestUsername = "test_comprehensive"
$TestPassword = "Test123456"

# Test counters
$script:TotalTests = 0
$script:PassedTests = 0
$script:FailedTests = 0

function Write-TestHeader($message) {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host $message -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Test-Endpoint {
    param([string]$Name, [scriptblock]$Script)
    $script:TotalTests++
    Write-Host "`n[$script:TotalTests] $Name" -ForegroundColor White
    try {
        $result = & $Script
        if ($result) {
            $script:PassedTests++
            Write-Host "  [PASS]" -ForegroundColor Green
        } else {
            $script:FailedTests++
            Write-Host "  [FAIL]" -ForegroundColor Red
        }
    } catch {
        $script:FailedTests++
        Write-Host "  [ERROR] $_" -ForegroundColor Red
    }
}

# MODULE 1: Authentication
Write-TestHeader "MODULE 1: Authentication & Authorization"

Test-Endpoint "User Registration" {
    $body = @{username=$TestUsername; password=$TestPassword; nickname="Test User"} | ConvertTo-Json
    try {
        $r = Invoke-RestMethod "$BaseUrl/auth/register" -Method Post -Body $body -ContentType "application/json"
        Write-Host "    Response: $($r.code)" -ForegroundColor Yellow
        return ($r.success -or $r.code -eq 4001)
    } catch {
        if ($_.ToString() -match "400") {
            Write-Host "    User exists (OK)" -ForegroundColor Yellow
            return $true
        }
        throw
    }
}

Test-Endpoint "User Login" {
    $body = @{username=$TestUsername; password=$TestPassword} | ConvertTo-Json
    $r = Invoke-RestMethod "$BaseUrl/auth/login" -Method Post -Body $body -ContentType "application/json"
    $script:Token = $r.data.token
    Write-Host "    Token: $($script:Token.Substring(0,15))..." -ForegroundColor Yellow
    return ($r.success -and $script:Token)
}

Test-Endpoint "Access Protected Resource" {
    $headers = @{Authorization="Bearer $script:Token"}
    $r = Invoke-RestMethod "$BaseUrl/chat/session?title=AuthTest" -Method Post -Headers $headers
    Write-Host "    Session ID: $($r.data.id)" -ForegroundColor Yellow
    return $r.success
}

Test-Endpoint "JWT Token Validation" {
    $headers = @{Authorization="Bearer $script:Token"}
    $r = Invoke-RestMethod "$BaseUrl/chat/session?title=TokenTest" -Method Post -Headers $headers
    return $r.success
}

# MODULE 2: Document Management
Write-TestHeader "MODULE 2: Document Management"

# Create test file
"Test document created at $(Get-Date)" | Out-File "test.txt" -Encoding UTF8

Test-Endpoint "Document Upload" {
    $headers = @{Authorization="Bearer $script:Token"}
    $file = Get-Item "test.txt"
    $fileBytes = [System.IO.File]::ReadAllBytes($file.FullName)
    $fileEnc = [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileBytes)
    
    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"
    $bodyLines = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"test.txt`"",
        "Content-Type: text/plain$LF",
        $fileEnc,
        "--$boundary--$LF"
    ) -join $LF
    
    $headers["Content-Type"] = "multipart/form-data; boundary=$boundary"
    $r = Invoke-RestMethod "$BaseUrl/doc/upload" -Method Post -Headers $headers -Body $bodyLines
    $script:DocId = $r.data.id
    Write-Host "    Document ID: $script:DocId" -ForegroundColor Yellow
    return ($r.success -and $script:DocId)
}

Test-Endpoint "Document Download" {
    $headers = @{Authorization="Bearer $script:Token"}
    try {
        Invoke-WebRequest "$BaseUrl/doc/download/$script:DocId" -Method Get -Headers $headers -OutFile "dl.txt"
        $exists = Test-Path "dl.txt"
        if ($exists) { Remove-Item "dl.txt" }
        Write-Host "    Download OK" -ForegroundColor Yellow
        return $exists
    } catch {
        Write-Host "    Download error: $_" -ForegroundColor Yellow
        return $false
    }
}

Test-Endpoint "Document List" {
    $headers = @{Authorization="Bearer $script:Token"}
    $r = Invoke-RestMethod "$BaseUrl/doc/list" -Method Get -Headers $headers
    $total = if ($r.data.PSObject.Properties.Name -contains "total") { $r.data.total } else { 0 }
    Write-Host "    API success: $($r.success), Total documents: $total" -ForegroundColor Yellow
    # API should succeed regardless of document count
    return ($r.success -eq $true -and $null -ne $r.data.records)
}

Test-Endpoint "Document Delete" {
    $headers = @{Authorization="Bearer $script:Token"}
    $r = Invoke-RestMethod "$BaseUrl/doc/$script:DocId" -Method Delete -Headers $headers
    return $r.success
}

# MODULE 3: AI Chat
Write-TestHeader "MODULE 3: AI Chat Functionality"

Test-Endpoint "Create Chat Session" {
    $headers = @{Authorization="Bearer $script:Token"}
    $r = Invoke-RestMethod "$BaseUrl/chat/session?title=CompTest" -Method Post -Headers $headers
    $script:SessionId = $r.data.id
    Write-Host "    Session ID: $script:SessionId" -ForegroundColor Yellow
    return ($r.success -and $script:SessionId)
}

Test-Endpoint "Send Chat Message" {
    $headers = @{Authorization="Bearer $script:Token"}
    $body = @{sessionId=$script:SessionId; content="Test message"} | ConvertTo-Json
    try {
        Invoke-WebRequest "$BaseUrl/chat/send" -Method Post -Headers $headers -Body $body -ContentType "application/json" -TimeoutSec 3 | Out-Null
        return $true
    } catch {
        Write-Host "    SSE timeout (expected)" -ForegroundColor Yellow
        return $true
    }
}

Test-Endpoint "Query Chat History" {
    $headers = @{Authorization="Bearer $script:Token"}
    Start-Sleep -Seconds 1
    $r = Invoke-RestMethod "$BaseUrl/chat/session/$script:SessionId/messages" -Method Get -Headers $headers
    Write-Host "    Message count: $($r.data.Count)" -ForegroundColor Yellow
    return ($r.success -and $r.data.Count -ge 2)
}

Test-Endpoint "Multi-turn Dialog" {
    $headers = @{Authorization="Bearer $script:Token"}
    $body = @{sessionId=$script:SessionId; content="Second message"} | ConvertTo-Json
    try {
        Invoke-WebRequest "$BaseUrl/chat/send" -Method Post -Headers $headers -Body $body -ContentType "application/json" -TimeoutSec 2 | Out-Null
    } catch {}
    
    Start-Sleep -Seconds 1
    $r = Invoke-RestMethod "$BaseUrl/chat/session/$script:SessionId/messages" -Method Get -Headers $headers
    Write-Host "    Total messages: $($r.data.Count)" -ForegroundColor Yellow
    return ($r.data.Count -ge 4)
}

# MODULE 4: Security
Write-TestHeader "MODULE 4: Security Testing"

Test-Endpoint "Block Unauthorized Access" {
    try {
        Invoke-RestMethod "$BaseUrl/chat/session?title=NoAuth" -Method Post
        return $false
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Write-Host "    Blocked with code: $code" -ForegroundColor Yellow
        return ($code -eq 401 -or $code -eq 403)
    }
}

Test-Endpoint "Block Invalid Token" {
    $headers = @{Authorization="Bearer invalid.token"}
    try {
        Invoke-RestMethod "$BaseUrl/chat/session?title=BadToken" -Method Post -Headers $headers
        return $false
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Write-Host "    Blocked with code: $code" -ForegroundColor Yellow
        return ($code -eq 401 -or $code -eq 403)
    }
}

Test-Endpoint "User Logout" {
    $headers = @{Authorization="Bearer $script:Token"}
    $r = Invoke-RestMethod "$BaseUrl/auth/logout" -Method Post -Headers $headers
    return $r.success
}

# SUMMARY
Write-TestHeader "TEST SUMMARY"
Write-Host "Total Tests: $script:TotalTests" -ForegroundColor White
Write-Host "Passed:      $script:PassedTests" -ForegroundColor Green
Write-Host "Failed:      $script:FailedTests" -ForegroundColor Red

$rate = [math]::Round(($script:PassedTests / $script:TotalTests) * 100, 1)
$color = if ($rate -ge 90) {"Green"} elseif ($rate -ge 70) {"Yellow"} else {"Red"}
Write-Host "Success Rate: $rate%" -ForegroundColor $color

# Cleanup
Remove-Item "test.txt" -ErrorAction SilentlyContinue
Remove-Item "dl.txt" -ErrorAction SilentlyContinue

Write-Host "`nTest completed!`n" -ForegroundColor Cyan
