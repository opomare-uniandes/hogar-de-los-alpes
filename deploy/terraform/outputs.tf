output "aws_profile" {
  description = "AWS CLI profile used for this deployment (so scripts like deploy/k8s-cloud/apply.sh don't need to hardcode it)"
  value       = var.aws_profile
}

output "aws_region" {
  description = "AWS region used for this deployment"
  value       = var.aws_region
}

output "vpc_id" {
  description = "ID of the created VPC"
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = module.vpc.vpc_cidr_block
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = module.vpc.private_subnet_ids
}

output "nat_gateway_ids" {
  description = "IDs of the NAT gateways"
  value       = module.vpc.nat_gateway_ids
}

output "availability_zones" {
  description = "AZs used by the VPC"
  value       = module.vpc.availability_zones
}

output "ecr_repository_urls" {
  description = "ECR repository URLs keyed by app name"
  value       = module.ecr.repository_urls
}

output "eks_cluster_name" {
  description = "Name of the EKS cluster"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS cluster API endpoint"
  value       = module.eks.cluster_endpoint
}

output "eks_oidc_issuer_url" {
  description = "EKS OIDC issuer URL"
  value       = module.eks.oidc_issuer_url
}

output "rds_endpoint" {
  description = "RDS Postgres endpoint (hostname)"
  value       = module.rds.endpoint
}

output "rds_master_username" {
  description = "RDS master username"
  value       = module.rds.master_username
}

output "rds_master_password" {
  description = "RDS generated master password"
  value       = module.rds.master_password
  sensitive   = true
}

output "redis_endpoint" {
  description = "ElastiCache (Valkey) endpoint (hostname)"
  value       = module.elasticache.endpoint
}

output "lbc_role_arn" {
  description = "IAM role ARN used by the AWS Load Balancer Controller's service account"
  value       = module.addons.lbc_role_arn
}

output "gatewayclass_controller_name" {
  description = "GatewayClass controllerName to reference from app manifests for ALB-backed Gateways"
  value       = module.addons.lbc_gatewayclass_controller_name
}

output "otel_collector_host" {
  description = "OTLP/HTTP base endpoint the services export to (rendered into hda-endpoints by apply.sh)"
  value       = var.otel_collector_host
}

output "otel_exporter_otlp_headers_authorization" {
  description = "OTLP Authorization header (rendered into the otel-credentials Secret by apply.sh)"
  value       = var.otel_exporter_otlp_headers_authorization
  sensitive   = true
}
