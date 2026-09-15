output "repository_urls" {
  description = "ECR repository URLs keyed by app name"
  value       = { for name, repo in aws_ecr_repository.this : name => repo.repository_url }
}

output "repository_arns" {
  description = "ECR repository ARNs keyed by app name"
  value       = { for name, repo in aws_ecr_repository.this : name => repo.arn }
}
