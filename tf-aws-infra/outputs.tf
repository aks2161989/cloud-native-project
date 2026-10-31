output "vpc_id" {
  description = "ID of the created VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = aws_subnet.private[*].id
}

output "internet_gateway_id" {
  description = "ID of the Internet Gateway"
  value       = aws_internet_gateway.main.id
}

output "public_route_table_id" {
  description = "ID of the public route table"
  value       = aws_route_table.public.id
}

output "private_route_table_id" {
  description = "ID of the private route table"
  value       = aws_route_table.private.id
}

output "s3_bucket_name" {
  description = "Name of the S3 bucket created for attachments"
  value       = aws_s3_bucket.attachments.bucket
}

output "rds_endpoint" {
  description = "Endpoint of the RDS PostgreSQL instance"
  value       = aws_db_instance.rds.endpoint
}

output "rds_address" {
  description = "Hostname of the RDS PostgreSQL instance"
  value       = aws_db_instance.rds.address
}

output "database_security_group_id" {
  description = "ID of the database security group"
  value       = aws_security_group.database.id
}

output "load_balancer_sg_id" {
  description = "ID of the Load Balancer security group"
  value       = aws_security_group.load_balancer.id
}

output "application_sg_id" {
  description = "ID of the Application security group"
  value       = aws_security_group.application.id
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.app_alb.dns_name
}

output "alb_zone_id" {
  description = "Canonical hosted zone ID of the Application Load Balancer"
  value       = aws_lb.app_alb.zone_id
}

output "route53_app_fqdn" {
  description = "FQDN of the Route 53 A record alias pointing to ALB"
  value       = aws_route53_record.app_alias.fqdn
}

output "autoscaling_group_name" {
  description = "Name of the Auto Scaling Group"
  value       = aws_autoscaling_group.app_asg.name
}

output "launch_template_name" {
  description = "Name of the Launch Template"
  value       = aws_launch_template.asg_launch_template.name
}
