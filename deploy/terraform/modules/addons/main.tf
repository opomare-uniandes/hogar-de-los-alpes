# --- AWS Load Balancer Controller (IRSA + Helm) ---

resource "aws_iam_policy" "lbc" {
  name   = "${var.project}-aws-lbc"
  policy = file("${path.module}/files/aws-load-balancer-controller-iam-policy.json")
  tags   = var.tags
}

data "aws_iam_policy_document" "lbc_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${replace(var.oidc_issuer_url, "https://", "")}:sub"
      values   = ["system:serviceaccount:kube-system:aws-load-balancer-controller"]
    }

    condition {
      test     = "StringEquals"
      variable = "${replace(var.oidc_issuer_url, "https://", "")}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "lbc" {
  name                 = "${var.project}-aws-lbc"
  assume_role_policy   = data.aws_iam_policy_document.lbc_assume_role.json
  permissions_boundary = var.iam_permissions_boundary_arn
  tags                 = var.tags
}

resource "aws_iam_role_policy_attachment" "lbc" {
  role       = aws_iam_role.lbc.name
  policy_arn = aws_iam_policy.lbc.arn
}

resource "helm_release" "aws_load_balancer_controller" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  version    = var.lbc_chart_version
  namespace  = "kube-system"

  set {
    name  = "clusterName"
    value = var.cluster_name
  }

  set {
    name  = "region"
    value = var.aws_region
  }

  set {
    name  = "vpcId"
    value = var.vpc_id
  }

  set {
    name  = "serviceAccount.create"
    value = "true"
  }

  set {
    name  = "serviceAccount.name"
    value = "aws-load-balancer-controller"
  }

  set {
    name  = "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = aws_iam_role.lbc.arn
  }

  # Gateway API (ALB) support: not an Ingress. See PLAN.md decision on Gateway vs Ingress.
  set {
    name  = "controllerConfig.featureGates.ALBGatewayAPI"
    value = "true"
  }

  depends_on = [aws_iam_role_policy_attachment.lbc, null_resource.gateway_api_crds]
}

# Gateway API CRDs must exist BEFORE the controller starts: it checks for them once at
# startup and permanently disables the ALBGatewayAPI feature gate for that pod's lifetime if
# they're missing (log: "Disabling ALBGatewayAPI: missing required CRDs") -- it does not
# re-check later. So this must run before the helm_release below, not after. Applied via
# kubectl (not kubernetes_manifest) because the bundle is a large multi-document release YAML.
resource "null_resource" "gateway_api_crds" {
  triggers = {
    gateway_api_version = "v1.2.0"
    lbc_version         = var.lbc_chart_version
  }

  provisioner "local-exec" {
    command = <<-EOT
      set -e
      aws eks update-kubeconfig --name ${var.cluster_name} --region ${var.aws_region} --profile ${var.aws_profile}
      kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml
      kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v${var.lbc_chart_version}/config/crd/gateway/gateway-crds.yaml
    EOT
  }
}

# The LBC's admission webhook intercepts every Service/Pod create cluster-wide with
# failurePolicy=Fail. `helm_release.aws_load_balancer_controller` reports complete once the
# Deployment is ready, but the webhook Service's endpoints can lag a few seconds behind that,
# and any resource created in that gap (e.g. KEDA's own Services) gets rejected with
# "no endpoints available for service aws-load-balancer-webhook-service". This buffer avoids
# racing that propagation delay.
resource "time_sleep" "wait_for_lbc_webhook" {
  depends_on      = [helm_release.aws_load_balancer_controller]
  create_duration = "30s"
}

# --- KEDA (Helm only; no AWS IAM needed, the Pulsar trigger talks to the in-cluster admin API over HTTP) ---

resource "kubernetes_namespace" "keda" {
  metadata {
    name = "keda"
  }
}

resource "helm_release" "keda" {
  name       = "keda"
  repository = "https://kedacore.github.io/charts"
  chart      = "keda"
  version    = var.keda_chart_version
  namespace  = kubernetes_namespace.keda.metadata[0].name

  depends_on = [time_sleep.wait_for_lbc_webhook]
}

resource "helm_release" "metrics_server" {
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  version    = var.metrics_server_chart_version
  namespace  = "kube-system"

  depends_on = [time_sleep.wait_for_lbc_webhook]
}
