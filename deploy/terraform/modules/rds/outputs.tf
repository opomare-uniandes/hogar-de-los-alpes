output "endpoint" {
  description = "RDS instance address (hostname, no port)"
  value       = aws_db_instance.this.address
}

output "port" {
  value = aws_db_instance.this.port
}

output "db_name" {
  value = aws_db_instance.this.db_name
}

output "master_username" {
  value = aws_db_instance.this.username
}

output "master_password" {
  description = "Generated master password; sensitive"
  value       = random_password.master.result
  sensitive   = true
}

output "security_group_id" {
  value = aws_security_group.this.id
}
