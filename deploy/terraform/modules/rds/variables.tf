variable "project" {
  description = "Project name; prefix for resource names"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the RDS instance is created"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for the DB subnet group"
  type        = list(string)
}

variable "allowed_security_group_id" {
  description = "Security group ID allowed to reach RDS on the Postgres port (the EKS cluster security group)"
  type        = string
}

variable "instance_class" {
  description = "RDS instance class"
  type        = string
}

variable "engine_version" {
  description = "PostgreSQL engine version"
  type        = string
}

variable "allocated_storage" {
  description = "Allocated storage (GiB)"
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Primary database created at instance creation (hda_trabajos); the second database (hda_usuarios) is created later by a Kubernetes bootstrap Job, mirroring the local postgres-init setup"
  type        = string
}

variable "master_username" {
  description = "Master username"
  type        = string
  default     = "hda"
}

variable "tags" {
  description = "Common tags applied to all resources"
  type        = map(string)
  default     = {}
}
