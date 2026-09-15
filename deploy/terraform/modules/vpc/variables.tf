variable "project" {
  description = "Project name; prefix for the Name tag on resources"
  type        = string
}

variable "cluster_name" {
  description = "Name of the EKS cluster that will use this VPC (for subnet discovery tags)"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
}

variable "az_count" {
  description = "Number of Availability Zones to use (public + private subnet per AZ)"
  type        = number
}

variable "single_nat_gateway" {
  description = "If true, creates a single shared NAT gateway instead of one per AZ"
  type        = bool
}

variable "nat_eip_allocation_id" {
  description = "Existing, unassociated EIP allocation ID to reuse for the NAT gateway instead of allocating a new one (e.g. when the account's EIP quota is exhausted). Only meaningful when single_nat_gateway is true. Leave null to allocate a new EIP."
  type        = string
  default     = null
}

variable "tags" {
  description = "Common tags applied to all resources"
  type        = map(string)
  default     = {}
}
