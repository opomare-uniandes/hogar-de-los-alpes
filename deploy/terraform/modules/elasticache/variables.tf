variable "project" {
  description = "Project name; prefix for resource names"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the cache cluster is created"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for the cache subnet group"
  type        = list(string)
}

variable "allowed_security_group_id" {
  description = "Security group ID allowed to reach the cache on its port (the EKS cluster security group)"
  type        = string
}

variable "node_type" {
  description = "Cache node instance type"
  type        = string
}

variable "engine" {
  description = "Cache engine (valkey or redis)"
  type        = string
}

variable "engine_version" {
  description = "Cache engine version"
  type        = string
}

variable "tags" {
  description = "Common tags applied to all resources"
  type        = map(string)
  default     = {}
}
