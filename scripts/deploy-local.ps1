# deploy-local.ps1 (FIXED STABLE VERSION)

param(
    [string]$GhcrUser     = "fish-dt",
    [string]$GhcrToken    = "",
    [string]$JwtSecret    = "local-dev-secret-min-32-chars-here!!",
    [switch]$RecreateCluster = $false
)

$Namespace  = "creator-crm"
$Context    = "kind-creator-crm"
$HelmDir    = "infra\k8s\helm"
$BaseDir    = "infra\k8s\base"

Write-Host "`n=== Creator CRM — Stable Deploy ===" -ForegroundColor Cyan

# ── 0. Cluster ─────────────────────────────────────────────
if ($RecreateCluster) {
    Write-Host "`n[0] Recreating Kind cluster..." -ForegroundColor Yellow
    kind delete cluster --name creator-crm
    kind create cluster --name creator-crm --config kind-config.yaml
}

kubectl config use-context $Context

# ── 1. Namespace ───────────────────────────────────────────
Write-Host "`n[1] Applying base infra..." -ForegroundColor Yellow
kubectl apply -f $BaseDir\namespace.yaml
kubectl apply -f $BaseDir\postgres.yaml
kubectl apply -f $BaseDir\redis.yaml
kubectl apply -f $BaseDir\kafka.yaml

# ── 2. WAIT FOR STATEFUL SERVICES ──────────────────────────
Write-Host "`n[2] Waiting for Postgres..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=postgres -n $Namespace --timeout=180s

Write-Host "`n[3] Waiting for Redis..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=redis -n $Namespace --timeout=180s

Write-Host "`n[4] Waiting for Kafka..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=kafka -n $Namespace --timeout=240s

# ── 3. GHCR SECRET ─────────────────────────────────────────
Write-Host "`n[5] GHCR Secret..." -ForegroundColor Yellow

if ($GhcrToken -ne "") {
    kubectl create secret docker-registry ghcr-secret `
        --docker-server=ghcr.io `
        --docker-username=$GhcrUser `
        --docker-password=$GhcrToken `
        --namespace $Namespace `
        --dry-run=client -o yaml | kubectl apply -f -
}

# ── 4. JWT SECRET ───────────────────────────────────────────
Write-Host "`n[6] JWT Secret..." -ForegroundColor Yellow

kubectl create secret generic jwt-secret `
    --from-literal=JWT_SECRET=$JwtSecret `
    --namespace $Namespace `
    --dry-run=client -o yaml | kubectl apply -f -

# ── 5. HELM DEPLOY (ORDER MATTERS) ──────────────────────────
Write-Host "`n[7] Deploy deal-service..." -ForegroundColor Yellow
helm upgrade --install deal-service $HelmDir\deal-service `
    --namespace $Namespace `
    --atomic --timeout 5m --wait

Write-Host "`n[8] Deploy api-gateway..." -ForegroundColor Yellow
helm upgrade --install api-gateway $HelmDir\api-gateway `
    --namespace $Namespace `
    --atomic --timeout 5m --wait

# ── 6. FINAL STATUS ─────────────────────────────────────────
Write-Host "`n[9] Status:" -ForegroundColor Yellow
kubectl get pods -n $Namespace
kubectl get svc -n $Namespace

Write-Host "`n=== DONE ===" -ForegroundColor Green
Write-Host "API: http://localhost:8080 (if mapped)" -ForegroundColor Cyan