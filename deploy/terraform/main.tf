provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = local.common_tags
  }
}

data "aws_eks_cluster_auth" "this" {
  name = module.eks.cluster_name
}

provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
  token                  = data.aws_eks_cluster_auth.this.token
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
    token                  = data.aws_eks_cluster_auth.this.token
  }
}

locals {
  common_tags = {
    Project     = var.project
    Terraform   = "true"
    Owner       = var.owner
    Environment = var.environment
  }
}

module "vpc" {
  source = "./modules/vpc"

  project               = var.project
  cluster_name          = var.cluster_name
  vpc_cidr              = var.vpc_cidr
  az_count              = var.az_count
  single_nat_gateway    = var.single_nat_gateway
  nat_eip_allocation_id = var.nat_eip_allocation_id
  tags                  = local.common_tags
}

module "ecr" {
  source = "./modules/ecr"

  repo_prefix = var.ecr_repo_prefix
  app_names   = var.app_names
  tags        = local.common_tags
}

module "eks" {
  source = "./modules/eks"

  project         = var.project
  cluster_name    = var.cluster_name
  cluster_version = var.eks_version

  vpc_id              = module.vpc.vpc_id
  public_subnet_ids   = module.vpc.public_subnet_ids
  private_subnet_ids  = module.vpc.private_subnet_ids
  node_instance_types = var.node_instance_types
  node_desired_size   = var.node_desired_size
  node_min_size       = var.node_min_size
  node_max_size       = var.node_max_size

  iam_permissions_boundary_arn = var.iam_permissions_boundary_arn

  tags = local.common_tags
}

module "rds" {
  source = "./modules/rds"

  project                   = var.project
  vpc_id                    = module.vpc.vpc_id
  private_subnet_ids        = module.vpc.private_subnet_ids
  allowed_security_group_id = module.eks.cluster_security_group_id
  instance_class            = var.rds_instance_class
  engine_version            = var.rds_engine_version
  allocated_storage         = var.rds_allocated_storage
  db_name                   = var.db_name_trabajos
  master_username           = var.db_master_username

  tags = local.common_tags
}

module "elasticache" {
  source = "./modules/elasticache"

  project                   = var.project
  vpc_id                    = module.vpc.vpc_id
  private_subnet_ids        = module.vpc.private_subnet_ids
  allowed_security_group_id = module.eks.cluster_security_group_id
  node_type                 = var.redis_node_type
  engine                    = var.redis_engine
  engine_version            = var.redis_engine_version

  tags = local.common_tags
}

module "addons" {
  source = "./modules/addons"

  project      = var.project
  aws_region   = var.aws_region
  aws_profile  = var.aws_profile
  cluster_name = module.eks.cluster_name
  vpc_id       = module.vpc.vpc_id

  oidc_provider_arn = module.eks.oidc_provider_arn
  oidc_issuer_url   = module.eks.oidc_issuer_url

  iam_permissions_boundary_arn = var.iam_permissions_boundary_arn
  lbc_chart_version            = var.lbc_chart_version
  keda_chart_version           = var.keda_chart_version
  metrics_server_chart_version = var.metrics_server_chart_version

  tags = local.common_tags
}
