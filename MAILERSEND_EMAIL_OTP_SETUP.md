# MailerSend email OTP setup

The backend sends registration OTPs through MailerSend SMTP. Configuration is already set for `smtp.mailersend.net` on port `587` with STARTTLS. Do not put credentials in `application.yaml` or commit them to Git.

## 1. Create SMTP credentials

1. In MailerSend, select **SMTP relay** from the dashboard, or open **Email > Domains** and manage the test/verified sending domain.
2. In its **SMTP** section, choose **Generate new user** and name it `cou-bus-tracker-backend`.
3. Copy the generated SMTP username and password immediately. The password is shown only once.

## 2. Choose the sender address

For a first test, use the sender/from address supplied by MailerSend's test domain. For deployment, add a domain that you own in **Email > Domains**, publish its DNS verification records, wait until the domain is verified, then use an address under that domain (for example `noreply@yourdomain.com`). A personal Gmail address is not a valid production From address unless MailerSend explicitly verifies it as a sender identity.

## 3. Configure environment variables

Set these values wherever the Spring Boot backend runs:

```text
MAILERSEND_SMTP_USERNAME=generated-smtp-username
MAILERSEND_SMTP_PASSWORD=generated-smtp-password
MAILERSEND_FROM_EMAIL=sender@your-verified-domain.com
MAILERSEND_FROM_NAME=CoU Bus Tracker
```

For local PowerShell development only:

```powershell
$env:MAILERSEND_SMTP_USERNAME = "generated-smtp-username"
$env:MAILERSEND_SMTP_PASSWORD = "generated-smtp-password"
$env:MAILERSEND_FROM_EMAIL = "sender@your-verified-domain.com"
$env:MAILERSEND_FROM_NAME = "CoU Bus Tracker"
mvn spring-boot:run
```

For Render or another host, add the same four values in its Environment Variables / Secrets page, then redeploy. Never expose the SMTP password in Flutter, source code, screenshots, or Git.

## 4. Test the complete app flow

1. Register a student or teacher with a password.
2. The response has `isEmailVerified: false` and no JWT; the OTP is sent by MailerSend.
3. Verify the received code through `POST /api/auth/email-verification/verify`.
4. The success response contains a JWT and `isEmailVerified: true`.

The OTP expires in five minutes. Resend is available once per minute, and five wrong OTP attempts invalidate the code. Existing accounts remain email-verified after the database migration, so they are not locked out.
