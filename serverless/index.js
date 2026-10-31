const { SESClient, SendEmailCommand } = require("@aws-sdk/client-ses");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, GetCommand, PutCommand } = require("@aws-sdk/lib-dynamodb");

const region = process.env.AWS_REGION || "us-east-1";
const sesClient = new SESClient({ region });
const ddbClient = new DynamoDBClient({ region });
const docClient = DynamoDBDocumentClient.from(ddbClient);

const TABLE_NAME = process.env.DYNAMODB_TABLE_NAME || "email-tracking";

exports.handler = async (event) => {
    console.log("Received event:", JSON.stringify(event, null, 2));

    for (const record of event.Records || []) {
        try {
            const snsMessageRaw = record.Sns ? record.Sns.Message : record.body;
            console.log("Raw SNS Message:", snsMessageRaw);

            const payload = typeof snsMessageRaw === "string" ? JSON.parse(snsMessageRaw) : snsMessageRaw;
            const { email, firstName, lastName, token, domain } = payload;

            if (!email || !token) {
                console.warn("Invalid payload, missing email or token:", payload);
                continue;
            }

            // Deduplication Check in DynamoDB
            const getParams = {
                TableName: TABLE_NAME,
                Key: {
                    email: email,
                    token: token
                }
            };

            const existingRecord = await docClient.send(new GetCommand(getParams));
            if (existingRecord.Item && existingRecord.Item.status === "SENT") {
                console.log(`Duplicate message detected for ${email} with token ${token}. Skipping email send.`);
                continue;
            }

            // Record into DynamoDB with 2-minute TTL
            const ttl = Math.floor(Date.now() / 1000) + 120;
            const putParams = {
                TableName: TABLE_NAME,
                Item: {
                    email: email,
                    token: token,
                    firstName: firstName || "",
                    lastName: lastName || "",
                    status: "SENT",
                    sentAt: new Date().toISOString(),
                    ttl: ttl
                }
            };
            await docClient.send(new PutCommand(putParams));
            console.log(`DynamoDB record saved for ${email}`);

            // Build verification URL
            const appDomain = domain || process.env.APP_DOMAIN || "dev.akshaymchavan.me";
            const verificationUrl = `http://${appDomain}/validateEmail?email=${encodeURIComponent(email)}&token=${encodeURIComponent(token)}`;

            // Send Email via SES
            const fromAddress = process.env.SOURCE_EMAIL || `noreply@${appDomain}`;
            const emailParams = {
                Source: fromAddress,
                Destination: {
                    ToAddresses: [email]
                },
                Message: {
                    Subject: {
                        Data: "Verify Your Email Address - CSYE6225",
                        Charset: "UTF-8"
                    },
                    Body: {
                        Text: {
                            Data: `Hello ${firstName || "User"},\n\nPlease verify your email address by clicking the link below:\n${verificationUrl}\n\nNote: This link will expire in 1 minute.\n\nThank you,\nCSYE6225 Team`,
                            Charset: "UTF-8"
                        },
                        Html: {
                            Data: `<html>
<body>
  <h2>Welcome, ${firstName || "User"}!</h2>
  <p>Thank you for signing up. Please verify your email address by clicking the button below:</p>
  <p><a href="${verificationUrl}" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;">Verify Email</a></p>
  <p>Or copy and paste this URL into your browser:</p>
  <p><a href="${verificationUrl}">${verificationUrl}</a></p>
  <p><em>Note: This link is valid for 1 minute only.</em></p>
</body>
</html>`,
                            Charset: "UTF-8"
                        }
                    }
                }
            };

            const sendResult = await sesClient.send(new SendEmailCommand(emailParams));
            console.log(`Email successfully sent to ${email}, messageId: ${sendResult.MessageId}`);

        } catch (err) {
            console.error("Error processing record:", err);
            throw err;
        }
    }

    return { statusCode: 200, body: JSON.stringify({ message: "Processed successfully" }) };
};
