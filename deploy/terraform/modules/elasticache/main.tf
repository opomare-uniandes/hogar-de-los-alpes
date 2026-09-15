resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.project}-redis"
  subnet_ids = var.private_subnet_ids
  tags       = var.tags
}

resource "aws_security_group" "this" {
  name_prefix = "${var.project}-redis-"
  description = "Allow Redis/Valkey from EKS nodes"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Redis/Valkey from EKS nodes"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [var.allowed_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name = "${var.project}-redis-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# A single-node aws_elasticache_cluster only validates engine as memcached/redis;
# valkey requires aws_elasticache_replication_group, even for one node.
resource "aws_elasticache_replication_group" "this" {
  replication_group_id = "${var.project}-redis"
  description          = "${var.project} cache"
  engine               = var.engine
  engine_version       = var.engine_version
  node_type            = var.node_type
  num_cache_clusters   = 1
  port                 = 6379

  subnet_group_name          = aws_elasticache_subnet_group.this.name
  security_group_ids         = [aws_security_group.this.id]
  automatic_failover_enabled = false

  tags = merge(var.tags, {
    Name = "${var.project}-redis"
  })
}
