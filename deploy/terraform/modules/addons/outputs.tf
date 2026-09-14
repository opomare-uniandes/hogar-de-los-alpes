output "lbc_role_arn" {
  description = "IAM role ARN used by the AWS Load Balancer Controller's service account"
  value       = aws_iam_role.lbc.arn
}

output "lbc_gatewayclass_controller_name" {
  description = "GatewayClass controllerName to reference from app manifests for ALB-backed Gateways"
  value       = "gateway.k8s.aws/alb"
}
