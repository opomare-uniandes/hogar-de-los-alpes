variable "repo_prefix" {
  description = "Prefix/namespace for ECR repository names (mirrors the local Docker image prefix, e.g. hda/trabajos-service)"
  type        = string
}

variable "app_names" {
  description = "Application names that need an ECR repository, matching the local Docker image names"
  type        = list(string)
}

variable "tags" {
  description = "Common tags applied to all repositories"
  type        = map(string)
  default     = {}
}
