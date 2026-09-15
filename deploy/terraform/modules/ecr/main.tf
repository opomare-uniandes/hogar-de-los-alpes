resource "aws_ecr_repository" "this" {
  for_each = toset(var.app_names)

  name                 = "${var.repo_prefix}/${each.value}"
  image_tag_mutability = "MUTABLE"

  # Lets `terraform destroy` tear down the PoC without manually emptying repos first.
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = merge(var.tags, {
    Name = "${var.repo_prefix}/${each.value}"
  })
}
