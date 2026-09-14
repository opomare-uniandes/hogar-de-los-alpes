variable "aws_region" {
  description = "AWS region to deploy the PoC into"
  type        = string
  default     = "us-east-1"
}

variable "aws_profile" {
  description = "AWS CLI (SSO) profile to use."
  type        = string
}

variable "project" {
  description = "Project name; prefix for resource names and the Project tag"
  type        = string
  default     = "hda-poc"
}

variable "environment" {
  description = "Environment tag"
  type        = string
  default     = "poc"
}

variable "owner" {
  description = "Owner tag"
  type        = string
  default     = "opomare"
}

variable "cluster_name" {
  description = "EKS cluster name; already used on subnets for discovery tags (kubernetes.io/cluster/<name>)"
  type        = string
  default     = "hda-poc"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.42.0.0/16"
}

variable "az_count" {
  description = "Number of Availability Zones to use"
  type        = number
  default     = 2
}

variable "single_nat_gateway" {
  description = "If true, creates a single shared NAT gateway (cost savings) instead of one per AZ"
  type        = bool
  default     = true
}

variable "nat_eip_allocation_id" {
  description = "Existing, unassociated EIP allocation ID to reuse for the NAT gateway instead of allocating a new one."
  type        = string
  default     = null
}

variable "ecr_repo_prefix" {
  description = "Prefix/namespace for ECR repository names (mirrors the local Docker image prefix, e.g. hda/trabajos-service)"
  type        = string
  default     = "hda"
}

variable "app_names" {
  description = "Application names that need an ECR repository, matching the local Docker image names"
  type        = list(string)
  default     = ["trabajos-service", "integracion-service", "notificaciones-service", "usuarios-service"]
}

variable "eks_version" {
  description = "Kubernetes version for the EKS cluster"
  type        = string
  default     = "1.36"
}

variable "node_instance_types" {
  description = "Instance types for the EKS managed node group"
  type        = list(string)
  default     = ["t3.large"]
}

variable "node_desired_size" {
  description = "Desired number of nodes in the managed node group"
  type        = number
  default     = 1
}

variable "node_min_size" {
  description = "Minimum number of nodes in the managed node group"
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "Maximum number of nodes in the managed node group"
  type        = number
  default     = 2
}

variable "iam_permissions_boundary_arn" {
  description = "IAM permissions boundary ARN required by the account's landing-zone governance policy, if any."
  type        = string
  default     = null
}

variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "rds_engine_version" {
  description = "PostgreSQL engine version, matching local postgres:18-alpine where RDS supports it"
  type        = string
  default     = "18.6"
}

variable "rds_allocated_storage" {
  description = "RDS allocated storage (GiB)"
  type        = number
  default     = 20
}

variable "db_name_trabajos" {
  description = "Primary database created by RDS at instance creation, matching local's POSTGRES_DB"
  type        = string
  default     = "hda_trabajos"
}

variable "db_master_username" {
  description = "RDS master username, matching local's POSTGRES_USER"
  type        = string
  default     = "hda"
}

variable "redis_node_type" {
  description = "ElastiCache node instance type"
  type        = string
  default     = "cache.t3.micro"
}

variable "redis_engine" {
  description = "ElastiCache engine (valkey, the actively maintained Redis-compatible fork)"
  type        = string
  default     = "valkey"
}

variable "redis_engine_version" {
  description = "ElastiCache engine version"
  type        = string
  default     = "8.1"
}

variable "lbc_chart_version" {
  description = "aws-load-balancer-controller Helm chart version"
  type        = string
  default     = "3.5.0"
}

variable "keda_chart_version" {
  description = "KEDA Helm chart version"
  type        = string
  default     = "2.20.2"
}
