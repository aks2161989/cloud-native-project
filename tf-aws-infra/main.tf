# --- Current AWS Caller Identity ---
data "aws_caller_identity" "current" {}

# --- VPC and Networking ---
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = var.vpc_name
  }
}

resource "aws_subnet" "public" {
  count = length(var.public_subnet_cidrs)

  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.vpc_name}-public-${count.index + 1}"
  }
}

resource "aws_subnet" "private" {
  count = length(var.private_subnet_cidrs)

  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name = "${var.vpc_name}-private-${count.index + 1}"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.vpc_name}-igw"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.vpc_name}-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.vpc_name}-private-rt"
  }
}

resource "aws_route_table_association" "private" {
  count = length(aws_subnet.private)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# --- AWS Key Management Service (KMS) Customer Managed Keys ---

# 1. EC2 Key for EBS Volume Encryption
resource "aws_kms_key" "ec2" {
  description             = "KMS customer managed key for EC2 EBS volume encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  rotation_period_in_days = 90

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      }
    ]
  })

  tags = {
    Name = "${var.vpc_name}-ec2-kms-key"
  }
}

resource "aws_kms_alias" "ec2" {
  name          = "alias/${var.vpc_name}-ec2-key"
  target_key_id = aws_kms_key.ec2.key_id
}

# 2. RDS Key for Database Storage Encryption
resource "aws_kms_key" "rds" {
  description             = "KMS customer managed key for RDS database encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  rotation_period_in_days = 90

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      }
    ]
  })

  tags = {
    Name = "${var.vpc_name}-rds-kms-key"
  }
}

resource "aws_kms_alias" "rds" {
  name          = "alias/${var.vpc_name}-rds-key"
  target_key_id = aws_kms_key.rds.key_id
}

# 3. S3 Key for S3 Bucket Server-Side Encryption
resource "aws_kms_key" "s3" {
  description             = "KMS customer managed key for S3 bucket encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  rotation_period_in_days = 90

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      }
    ]
  })

  tags = {
    Name = "${var.vpc_name}-s3-kms-key"
  }
}

resource "aws_kms_alias" "s3" {
  name          = "alias/${var.vpc_name}-s3-key"
  target_key_id = aws_kms_key.s3.key_id
}

# 4. Secrets Manager Key for Database Credentials Encryption
resource "aws_kms_key" "secrets" {
  description             = "KMS customer managed key for Secrets Manager database credentials"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  rotation_period_in_days = 90

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      }
    ]
  })

  tags = {
    Name = "${var.vpc_name}-secrets-kms-key"
  }
}

resource "aws_kms_alias" "secrets" {
  name          = "alias/${var.vpc_name}-secrets-key"
  target_key_id = aws_kms_key.secrets.key_id
}

# --- AWS Secrets Manager for RDS Credentials ---
resource "random_string" "secret_suffix" {
  length  = 6
  special = false
  upper   = false
}

resource "aws_secretsmanager_secret" "db_secret" {
  name                    = "${var.vpc_name}-db-credentials-${random_string.secret_suffix.result}"
  description             = "Database credentials for RDS PostgreSQL instance"
  kms_key_id              = aws_kms_key.secrets.arn
  recovery_window_in_days = 0

  tags = {
    Name = "${var.vpc_name}-db-secret"
  }
}

resource "aws_secretsmanager_secret_version" "db_secret_version" {
  secret_id = aws_secretsmanager_secret.db_secret.id
  secret_string = jsonencode({
    username = "csye6225"
    password = random_password.db_password.result
    host     = aws_db_instance.rds.address
    port     = 5432
    database = aws_db_instance.rds.db_name
  })
}

# --- Security Groups ---

# Load Balancer Security Group
resource "aws_security_group" "load_balancer" {
  name        = "${var.vpc_name}-load-balancer-sg"
  description = "Security group for the Application Load Balancer"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS from anywhere"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.vpc_name}-load-balancer-sg"
  }
}

# Application Security Group
resource "aws_security_group" "application" {
  name        = "${var.vpc_name}-application-sg"
  description = "Security group for web application EC2 instances"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "SSH from anywhere"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description     = "Application traffic from Load Balancer SG only"
    from_port       = var.app_port
    to_port         = var.app_port
    protocol        = "tcp"
    security_groups = [aws_security_group.load_balancer.id]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.vpc_name}-application-sg"
  }
}

# Database Security Group
resource "aws_security_group" "database" {
  name        = "${var.vpc_name}-database-sg"
  description = "Database security group for RDS instance"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL access from application security group"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.application.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.vpc_name}-database-sg"
  }
}

# --- S3 Bucket for Attachments ---
resource "random_uuid" "bucket_name" {}

resource "aws_s3_bucket" "attachments" {
  bucket        = random_uuid.bucket_name.result
  force_destroy = true

  tags = {
    Name = "${var.vpc_name}-attachments-bucket"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "attachments_encryption" {
  bucket = aws_s3_bucket.attachments.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.s3.arn
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "attachments_lifecycle" {
  bucket = aws_s3_bucket.attachments.id

  rule {
    id     = "transition-to-standard-ia"
    status = "Enabled"

    filter {}

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "attachments_public_access_block" {
  bucket = aws_s3_bucket.attachments.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# --- Amazon SNS Topic for User Registration ---
resource "aws_sns_topic" "user_signup" {
  name = "${var.vpc_name}-user-signup-topic"

  tags = {
    Name = "${var.vpc_name}-user-signup-topic"
  }
}

# --- IAM Role and Policy for EC2 S3, Secrets Manager, KMS, SNS, and CloudWatch Access ---
resource "aws_iam_role" "ec2_s3_role" {
  name = "${var.vpc_name}-ec2-s3-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "${var.vpc_name}-ec2-s3-role"
  }
}

resource "aws_iam_policy" "ec2_app_policy" {
  name        = "${var.vpc_name}-ec2-app-policy"
  description = "IAM policy for EC2 instance to access S3, Secrets Manager, KMS, and SNS"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3Access"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.attachments.arn,
          "${aws_s3_bucket.attachments.arn}/*"
        ]
      },
      {
        Sid    = "SecretsManagerAccess"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = aws_secretsmanager_secret.db_secret.arn
      },
      {
        Sid    = "KmsAccess"
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:DescribeKey",
          "kms:Encrypt",
          "kms:GenerateDataKey*"
        ]
        Resource = [
          aws_kms_key.s3.arn,
          aws_kms_key.secrets.arn
        ]
      },
      {
        Sid    = "SnsPublishAccess"
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = aws_sns_topic.user_signup.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_app_policy_attachment" {
  role       = aws_iam_role.ec2_s3_role.name
  policy_arn = aws_iam_policy.ec2_app_policy.arn
}

resource "aws_iam_role_policy_attachment" "ec2_cloudwatch_agent_policy_attachment" {
  role       = aws_iam_role.ec2_s3_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.vpc_name}-ec2-instance-profile"
  role = aws_iam_role.ec2_s3_role.name
}

# --- RDS Parameter Group & DB Subnet Group ---
resource "aws_db_parameter_group" "postgres" {
  name        = "${var.vpc_name}-postgres-parameter-group"
  family      = "postgres16"
  description = "Custom parameter group for PostgreSQL 16"

  tags = {
    Name = "${var.vpc_name}-postgres-parameter-group"
  }
}

resource "aws_db_subnet_group" "private" {
  name        = "${var.vpc_name}-db-subnet-group"
  description = "Database subnet group using private subnets"
  subnet_ids  = aws_subnet.private[*].id

  tags = {
    Name = "${var.vpc_name}-db-subnet-group"
  }
}

# --- Database Credentials & RDS Instance ---
resource "random_password" "db_password" {
  length           = 16
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_db_instance" "rds" {
  identifier             = "csye6225"
  engine                 = "postgres"
  engine_version         = "16.3"
  instance_class         = "db.t4g.micro"
  allocated_storage      = 20
  db_name                = "csye6225"
  username               = "csye6225"
  password               = random_password.db_password.result
  multi_az               = false
  publicly_accessible    = false
  db_subnet_group_name   = aws_db_subnet_group.private.name
  vpc_security_group_ids = [aws_security_group.database.id]
  parameter_group_name   = aws_db_parameter_group.postgres.name
  skip_final_snapshot    = true

  # KMS storage encryption with custom RDS KMS Key
  storage_encrypted = true
  kms_key_id        = aws_kms_key.rds.arn

  tags = {
    Name = "${var.vpc_name}-rds-postgres"
  }
}

# --- Application Load Balancer ---
resource "aws_lb" "app_alb" {
  name               = "${var.vpc_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.load_balancer.id]
  subnets            = aws_subnet.public[*].id

  tags = {
    Name = "${var.vpc_name}-alb"
  }
}

# --- ALB Target Group ---
resource "aws_lb_target_group" "app_tg" {
  name        = "${var.vpc_name}-tg"
  port        = var.app_port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "instance"

  health_check {
    enabled             = true
    path                = "/healthz"
    protocol            = "HTTP"
    port                = "traffic-port"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200"
  }

  tags = {
    Name = "${var.vpc_name}-tg"
  }
}

# --- SSL / TLS Certificate (Dev ACM / Demo Import) ---
resource "aws_acm_certificate" "cert" {
  count             = var.ssl_certificate_arn == null ? 1 : 0
  domain_name       = var.domain_name
  validation_method = "DNS"

  tags = {
    Name = "${var.vpc_name}-acm-cert"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "cert_validation" {
  for_each = var.ssl_certificate_arn == null ? {
    for dvo in aws_acm_certificate.cert[0].domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  } : {}

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = data.aws_route53_zone.primary.zone_id
}

resource "aws_acm_certificate_validation" "cert" {
  count                   = var.ssl_certificate_arn == null ? 1 : 0
  certificate_arn         = aws_acm_certificate.cert[0].arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

# --- ALB Listeners (HTTP 80 and HTTPS 443) ---
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.app_alb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app_tg.arn
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.app_alb.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.ssl_certificate_arn != null ? var.ssl_certificate_arn : aws_acm_certificate_validation.cert[0].certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app_tg.arn
  }
}

# --- Launch Template for Auto Scaling Group ---
resource "aws_launch_template" "asg_launch_template" {
  name          = "csye6225_asg"
  image_id      = var.ami_id
  instance_type = var.instance_type
  key_name      = var.key_name

  iam_instance_profile {
    name = aws_iam_instance_profile.ec2_profile.name
  }

  network_interfaces {
    associate_public_ip_address = true
    security_groups             = [aws_security_group.application.id]
  }

  block_device_mappings {
    device_name = "/dev/sda1"

    ebs {
      volume_size           = 25
      volume_type           = "gp2"
      delete_on_termination = true
      encrypted             = true
      kms_key_id            = aws_kms_key.ec2.arn
    }
  }

  user_data = base64encode(<<-EOF
              #!/bin/bash
              mkdir -p /opt/csye6225

              # Retrieve database credentials from AWS Secrets Manager using AWS CLI and python
              SECRET_JSON=$(aws secretsmanager get-secret-value --secret-id ${aws_secretsmanager_secret.db_secret.arn} --region ${var.aws_region} --query SecretString --output text)
              DB_PASSWORD=$(python3 -c "import json, sys; print(json.loads('''$SECRET_JSON''')['password'])")

              cat <<ENVFILE > /opt/csye6225/application.env
              DB_URL=jdbc:postgresql://${aws_db_instance.rds.address}:5432/${aws_db_instance.rds.db_name}
              DB_USERNAME=${aws_db_instance.rds.username}
              DB_PASSWORD=$DB_PASSWORD
              AWS_REGION=${var.aws_region}
              S3_BUCKET_NAME=${aws_s3_bucket.attachments.bucket}
              SNS_TOPIC_ARN=${aws_sns_topic.user_signup.arn}
              DOMAIN_NAME=${var.domain_name}
              LOG_FILE_PATH=/var/log/csye6225/app.log
              STATSD_HOST=localhost
              STATSD_PORT=8125
              STATSD_PREFIX=csye6225
              ENVFILE
              chown csye6225:csye6225 /opt/csye6225/application.env
              chmod 600 /opt/csye6225/application.env

              # Ensure log directory and log file exist with appropriate permissions
              mkdir -p /var/log/csye6225
              touch /var/log/csye6225/app.log
              chown -R csye6225:csye6225 /var/log/csye6225
              chmod 755 /var/log/csye6225
              chmod 644 /var/log/csye6225/app.log

              # Configure and restart CloudWatch Agent
              /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
                -a fetch-config \
                -m ec2 \
                -s \
                -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

              systemctl daemon-reload
              systemctl restart productcatalog.service
              EOF
  )

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "${var.vpc_name}-asg-instance"
    }
  }

  depends_on = [
    aws_db_instance.rds,
    aws_s3_bucket.attachments,
    aws_secretsmanager_secret_version.db_secret_version,
    aws_sns_topic.user_signup
  ]
}

# --- Auto Scaling Group ---
resource "aws_autoscaling_group" "app_asg" {
  name                = "${var.vpc_name}-asg"
  vpc_zone_identifier = aws_subnet.public[*].id

  min_size         = var.asg_min_size
  max_size         = var.asg_max_size
  desired_capacity = var.asg_desired_capacity
  default_cooldown = 60

  target_group_arns = [aws_lb_target_group.app_tg.arn]

  launch_template {
    id      = aws_launch_template.asg_launch_template.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "${var.vpc_name}-asg-instance"
    propagate_at_launch = true
  }

  depends_on = [
    aws_lb_target_group.app_tg
  ]
}

# --- Auto Scaling Policies & CloudWatch Alarms ---

# Scale Up Policy (+1 instance)
resource "aws_autoscaling_policy" "scale_up" {
  name                   = "${var.vpc_name}-scale-up-policy"
  scaling_adjustment     = 1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = aws_autoscaling_group.app_asg.name
  policy_type            = "SimpleScaling"
}

# Scale Up Alarm: Average CPUUtilization > 5%
resource "aws_cloudwatch_metric_alarm" "cpu_high" {
  alarm_name          = "${var.vpc_name}-cpu-high-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Average"
  threshold           = 5
  alarm_description   = "Scale up when average CPU exceeds 5%"
  alarm_actions       = [aws_autoscaling_policy.scale_up.arn]

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.app_asg.name
  }
}

# Scale Down Policy (-1 instance)
resource "aws_autoscaling_policy" "scale_down" {
  name                   = "${var.vpc_name}-scale-down-policy"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = aws_autoscaling_group.app_asg.name
  policy_type            = "SimpleScaling"
}

# Scale Down Alarm: Average CPUUtilization < 3%
resource "aws_cloudwatch_metric_alarm" "cpu_low" {
  alarm_name          = "${var.vpc_name}-cpu-low-alarm"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 1
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Average"
  threshold           = 3
  alarm_description   = "Scale down when average CPU drops below 3%"
  alarm_actions       = [aws_autoscaling_policy.scale_down.arn]

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.app_asg.name
  }
}

# --- Route 53 DNS Configuration ---
data "aws_route53_zone" "primary" {
  name         = var.domain_name
  private_zone = false
}

resource "aws_route53_record" "app_alias" {
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_lb.app_alb.dns_name
    zone_id                = aws_lb.app_alb.zone_id
    evaluate_target_health = true
  }
}

# --- AWS SES & DNS Email Records (DKIM & SPF) ---
resource "aws_ses_domain_identity" "domain" {
  domain = var.domain_name
}

resource "aws_ses_domain_dkim" "dkim" {
  domain = aws_ses_domain_identity.domain.domain
}

resource "aws_route53_record" "ses_dkim" {
  count   = 3
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = "${aws_ses_domain_dkim.dkim.dkim_tokens[count.index]}._domainkey.${var.domain_name}"
  type    = "CNAME"
  ttl     = 600
  records = ["${aws_ses_domain_dkim.dkim.dkim_tokens[count.index]}.dkim.amazonses.com"]
}

resource "aws_route53_record" "spf" {
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = var.domain_name
  type    = "TXT"
  ttl     = 600
  records = ["v=spf1 include:amazonses.com ~all"]
}

# --- DynamoDB Table for Email Tracking & Deduplication ---
resource "aws_dynamodb_table" "email_tracking" {
  name         = "${var.vpc_name}-email-tracking"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "email"
  range_key    = "token"

  attribute {
    name = "email"
    type = "S"
  }

  attribute {
    name = "token"
    type = "S"
  }

  ttl {
    attribute_name = "ttl"
    enabled        = true
  }

  tags = {
    Name = "${var.vpc_name}-email-tracking"
  }
}

# --- Serverless Lambda Function Package & Deployment ---
data "archive_file" "lambda_zip" {
  type        = "zip"
  source_dir  = "${path.module}/../serverless"
  output_path = "${path.module}/lambda_function.zip"
  excludes    = [".git", "node_modules", ".github", "lambda-function.zip", "*.zip"]
}

resource "aws_iam_role" "lambda_role" {
  name = "${var.vpc_name}-lambda-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "${var.vpc_name}-lambda-exec-role"
  }
}

resource "aws_iam_role_policy_attachment" "lambda_basic_execution" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_policy" "lambda_app_policy" {
  name        = "${var.vpc_name}-lambda-app-policy"
  description = "IAM policy for Lambda to access DynamoDB and SES"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "DynamoDBAccess"
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:Query"
        ]
        Resource = aws_dynamodb_table.email_tracking.arn
      },
      {
        Sid    = "SESAccess"
        Effect = "Allow"
        Action = [
          "ses:SendEmail",
          "ses:SendRawEmail"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_app_policy_attachment" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = aws_iam_policy.lambda_app_policy.arn
}

resource "aws_lambda_function" "user_verification" {
  filename         = data.archive_file.lambda_zip.output_path
  source_code_hash = data.archive_file.lambda_zip.output_base64sha256
  function_name    = "${var.vpc_name}-user-verification-lambda"
  role             = aws_iam_role.lambda_role.arn
  handler          = "index.handler"
  runtime          = "nodejs20.x"
  timeout          = 60
  memory_size      = 256

  environment {
    variables = {
      DYNAMODB_TABLE_NAME = aws_dynamodb_table.email_tracking.name
      AWS_REGION          = var.aws_region
      APP_DOMAIN          = var.domain_name
      SOURCE_EMAIL        = "noreply@${var.domain_name}"
    }
  }

  tags = {
    Name = "${var.vpc_name}-user-verification-lambda"
  }
}

resource "aws_lambda_permission" "sns_invoke" {
  statement_id  = "AllowSNSInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.user_verification.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = aws_sns_topic.user_signup.arn
}

resource "aws_sns_topic_subscription" "lambda_sub" {
  topic_arn = aws_sns_topic.user_signup.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.user_verification.arn
}
