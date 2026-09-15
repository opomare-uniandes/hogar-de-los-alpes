variable "project" {
  description = "Project name; prefix for IAM role and node group names"
  type        = string
}

variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

variable "cluster_version" {
  description = "Kubernetes version for the EKS cluster"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the cluster and node group are created"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs for the cluster control plane ENIs"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for the managed node group"
  type        = list(string)
}

variable "node_instance_types" {
  description = "Instance types for the managed node group"
  type        = list(string)
}

variable "node_desired_size" {
  description = "Desired number of nodes"
  type        = number
}

variable "node_min_size" {
  description = "Minimum number of nodes"
  type        = number
}

variable "node_max_size" {
  description = "Maximum number of nodes"
  type        = number
}

variable "node_disk_size" {
  description = "Root volume size (GiB) for worker nodes"
  type        = number
  default     = 20
}

variable "iam_permissions_boundary_arn" {
  description = "IAM permissions boundary ARN required by the account's governance policy for any role this module creates. Leave null if the account has no such requirement."
  type        = string
  default     = null
}

variable "tags" {
  description = "Common tags applied to all resources"
  type        = map(string)
  default     = {}
}
