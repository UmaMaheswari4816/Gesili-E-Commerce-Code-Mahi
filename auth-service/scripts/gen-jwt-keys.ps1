# Generates an RSA keypair (PKCS#8 PEM) for signing JWTs locally.
# Output goes to auth-service/keys/ which is GITIGNORED — the private key is never committed.
# Requires OpenSSL (ships with Git for Windows: C:\Program Files\Git\usr\bin\openssl.exe).
param(
    [string]$OutDir = "$PSScriptRoot\..\keys"
)
$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

$openssl = "openssl"
if (-not (Get-Command openssl -ErrorAction SilentlyContinue)) {
    $gitSsl = "C:\Program Files\Git\usr\bin\openssl.exe"
    if (Test-Path $gitSsl) { $openssl = $gitSsl } else { throw "OpenSSL not found. Install Git for Windows or OpenSSL." }
}

& $openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$OutDir\jwt-private.pem"
& $openssl rsa -pubout -in "$OutDir\jwt-private.pem" -out "$OutDir\jwt-public.pem"

Write-Host "Keys written to $OutDir"
Write-Host "Set these env vars (or config) before starting auth-service:"
Write-Host "  JWT_PRIVATE_KEY_LOCATION=file:./auth-service/keys/jwt-private.pem"
Write-Host "  JWT_PUBLIC_KEY_LOCATION=file:./auth-service/keys/jwt-public.pem"
Write-Host "  JWT_ACTIVE_KID=key-1"
