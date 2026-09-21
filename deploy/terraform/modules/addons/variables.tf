variable "project" {
  description = "Project name; prefix for resource names"
  type        = string
}

variable "aws_region" {
  description = "AWS region (passed to the AWS Load Balancer Controller chart)"
  type        = string
}

variable "aws_profile" {
  description = "AWS CLI profile used by the local kubectl/aws eks update-kubeconfig step that applies the Gateway API CRDs"
  type        = string
}

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID of the EKS cluster"
  type        = string
}

variable "oidc_provider_arn" {
  description = "EKS OIDC provider ARN, for the AWS Load Balancer Controller's IRSA role"
  type        = string
}

variable "oidc_issuer_url" {
  description = "EKS OIDC issuer URL (https://oidc.eks.<region>.amazonaws.com/id/<id>)"
  type        = string
}

variable "iam_permissions_boundary_arn" {
  description = "IAM permissions boundary ARN required by the account's governance policy for any role this module creates. Leave null if the account has no such requirement."
  type        = string
  default     = null
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

variable "metrics_server_chart_version" {
  description = "metrics-server Helm chart version"
  type        = string
  default     = "3.12.2"
}

variable "tags" {
  description = "Common tags applied to IAM resources"
  type        = map(string)
  default     = {}
}
