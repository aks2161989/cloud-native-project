packer {
  required_plugins {
    amazon = {
      version = ">= 1.3.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "demo_account_id" {
  type    = string
  default = "422783588667"
}

variable "source_ami_owner" {
  type    = string
  default = "099720109477"
}

variable "instance_type" {
  type    = string
  default = "t2.micro"
}

variable "ami_name" {
  type    = string
  default = "productcatalog-api-{{timestamp}}"
}

source "amazon-ebs" "ubuntu" {
  region        = var.aws_region
  instance_type = var.instance_type
  ssh_username  = "ubuntu"

  ami_name = var.ami_name

  ami_users = [
    var.demo_account_id
  ]

  source_ami_filter {
    filters = {
      name                = "ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }

    most_recent = true
    owners      = [var.source_ami_owner]
  }
}

build {
  sources = [
    "source.amazon-ebs.ubuntu"
  ]

  provisioner "file" {
    source      = "target/productcatalog-api-0.0.1-SNAPSHOT.jar"
    destination = "/tmp/productcatalog-api.jar"
    generated   = true
  }

  provisioner "file" {
    source      = "productcatalog.service"
    destination = "/tmp/productcatalog.service"
  }

  provisioner "file" {
    source      = "amazon-cloudwatch-agent.json"
    destination = "/tmp/amazon-cloudwatch-agent.json"
  }

  provisioner "file" {
    source      = "setup.sh"
    destination = "/tmp/setup.sh"
  }

  provisioner "shell" {
    inline = [
      "chmod +x /tmp/setup.sh",
      "sudo /tmp/setup.sh /tmp/productcatalog-api.jar /tmp/productcatalog.service /tmp/amazon-cloudwatch-agent.json"
    ]
  }
}
