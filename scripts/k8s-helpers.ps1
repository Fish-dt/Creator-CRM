# k8s-helpers.ps1
# Run individual functions as needed from PowerShell
# Example: . .\scripts\k8s-helpers.ps1; Watch-Pods

$NS = "creator-crm"

function Watch-Pods {
    kubectl get pods -n $NS -w
}

function Get-Logs($service, $lines = 100) {
    kubectl logs -n $NS -l app=$service --tail=$lines -f
}

function Forward-Gateway {
    Write-Host "Forwarding api-gateway to http://localhost:8080 ..."
    kubectl port-forward svc/api-gateway 8080:8080 -n $NS
}

function Forward-DealService {
    Write-Host "Forwarding deal-service to http://localhost:8081 ..."
    kubectl port-forward svc/deal-service 8081:8081 -n $NS
}

function Forward-Postgres {
    Write-Host "Forwarding postgres to localhost:5432 ..."
    kubectl port-forward svc/postgres 5432:5432 -n $NS
}

function Describe-Pod($service) {
    $pod = kubectl get pod -n $NS -l app=$service -o jsonpath="{.items[0].metadata.name}"
    kubectl describe pod $pod -n $NS
}

function Exec-Pod($service) {
    $pod = kubectl get pod -n $NS -l app=$service -o jsonpath="{.items[0].metadata.name}"
    kubectl exec -it $pod -n $NS -- sh
}

function Restart-Service($service) {
    kubectl rollout restart deployment/$service -n $NS
}

function Status {
    Write-Host "`n--- Pods ---" -ForegroundColor Cyan
    kubectl get pods -n $NS
    Write-Host "`n--- Services ---" -ForegroundColor Cyan
    kubectl get svc -n $NS
    Write-Host "`n--- Recent Events ---" -ForegroundColor Cyan
    kubectl get events -n $NS --sort-by='.lastTimestamp' | Select-Object -Last 15
}

Write-Host "Helper functions loaded:" -ForegroundColor Green
Write-Host "  Watch-Pods | Get-Logs <svc> | Forward-Gateway | Forward-DealService"
Write-Host "  Forward-Postgres | Describe-Pod <svc> | Exec-Pod <svc>"
Write-Host "  Restart-Service <svc> | Status"
