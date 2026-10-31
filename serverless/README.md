# Serverless Email Verification Lambda Function

This repository contains the AWS Lambda function responsible for sending email verification links to users upon account creation, triggered by an Amazon SNS notification.

---

## Prerequisites for Building and Deploying Locally

1. **Node.js**: Version `20.x` or higher installed.
2. **AWS CLI**: Version `2.x+` installed and configured with credentials for AWS DEV or DEMO account.
3. **AWS Permissions**:
   - Access to Amazon SNS (subscribe / publish).
   - Access to Amazon SES (`ses:SendEmail`).
   - Access to Amazon DynamoDB (`dynamodb:GetItem`, `dynamodb:PutItem`).
   - Access to AWS Lambda (`lambda:UpdateFunctionCode`).

---

## Build and Deploy Instructions

### 1. Install Dependencies
```bash
npm install
```

### 2. Package Lambda Function
Create a deployment zip bundle:
```bash
# On Linux / macOS / Git Bash:
zip -r lambda-function.zip index.js node_modules package.json

# On Windows PowerShell:
Compress-Archive -Path index.js, package.json -DestinationPath lambda-function.zip -Force
```

### 3. Deploy to AWS Lambda
Update the existing Lambda function code created by Terraform:
```bash
aws lambda update-function-code \
  --function-name dev-vpc-user-verification-lambda \
  --zip-file fileb://lambda-function.zip \
  --region us-east-1
```

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DYNAMODB_TABLE_NAME` | DynamoDB table used for email deduplication | `dev-vpc-email-tracking` |
| `AWS_REGION` | AWS Region for SES, SNS, and DynamoDB | `us-east-1` |
| `APP_DOMAIN` | Web application domain name | `dev.akshaymchavan.me` |
| `SOURCE_EMAIL` | From email address verified in SES | `noreply@dev.akshaymchavan.me` |
