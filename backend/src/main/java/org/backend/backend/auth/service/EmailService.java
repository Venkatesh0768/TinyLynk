package org.backend.backend.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.backend.auth.exception.EmailSendingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public void sendOTPEmail(String to, String otpCode) {
        String subject = "Your Verification Code – " + otpCode;
        String html = buildOtpHtml(
                "Verify Your Email",
                "You're almost there! Use the code below to verify your email address.",
                otpCode,
                "This code expires in <strong>5 minutes</strong>. Don't share it with anyone.",
                "If you didn't create an account, you can safely ignore this email."
        );
        sendHtml(to, subject, html, "OTP");
    }

    public void sendPasswordResetOTPEmail(String to, String otpCode) {
        String subject = "Password Reset Code – " + otpCode;
        String html = buildOtpHtml(
                "Reset Your Password",
                "We received a request to reset your password. Use the code below to proceed.",
                otpCode,
                "This code expires in <strong>5 minutes</strong>. Don't share it with anyone.",
                "If you didn't request a password reset, please secure your account immediately."
        );
        sendHtml(to, subject, html, "password reset OTP");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String html, String label) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromEmail, "AppName Security");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = isHtml
            mailSender.send(mime);
            log.info("✓ {} email sent to {}", label, to);
        } catch (MessagingException | org.springframework.mail.MailException | java.io.UnsupportedEncodingException e) {
            log.error("✗ SMTP failure sending {} to {}: {} — Cause: {}",
                    label, to, e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "unknown");
            throw new EmailSendingException("Failed to send " + label + " email", e);
        }
    }

    private String buildOtpHtml(String title, String intro, String otp,
                                 String expiry, String disclaimer) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
          <title>%s</title>
        </head>
        <body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',Arial,sans-serif;">

          <!-- Wrapper -->
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0f172a;padding:40px 16px;">
            <tr><td align="center">

              <!-- Card -->
              <table width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%%;background:#1e293b;border-radius:16px;overflow:hidden;box-shadow:0 20px 60px rgba(0,0,0,0.5);">

                <!-- Header gradient -->
                <tr>
                  <td style="background:linear-gradient(135deg,#6366f1 0%%,#8b5cf6 100%%);padding:36px 40px 32px;text-align:center;">
                    <div style="display:inline-block;width:52px;height:52px;background:rgba(255,255,255,0.15);border-radius:14px;line-height:52px;font-size:24px;margin-bottom:16px;">🔐</div>
                    <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.3px;">%s</h1>
                  </td>
                </tr>

                <!-- Body -->
                <tr>
                  <td style="padding:36px 40px;">

                    <!-- Intro -->
                    <p style="margin:0 0 28px;color:#94a3b8;font-size:15px;line-height:1.7;">%s</p>

                    <!-- OTP Box -->
                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                      <tr>
                        <td align="center" style="background:#0f172a;border:1.5px solid #334155;border-radius:12px;padding:28px 20px;">
                          <p style="margin:0 0 8px;color:#64748b;font-size:11px;text-transform:uppercase;letter-spacing:2px;font-weight:600;">Your One-Time Code</p>
                          <p style="margin:0;color:#ffffff;font-size:42px;font-weight:800;letter-spacing:12px;font-variant-numeric:tabular-nums;">%s</p>
                        </td>
                      </tr>
                    </table>

                    <!-- Expiry notice -->
                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:24px;">
                      <tr>
                        <td style="background:#1d2d44;border-left:3px solid #6366f1;border-radius:0 8px 8px 0;padding:12px 16px;">
                          <p style="margin:0;color:#94a3b8;font-size:13px;">⏱ %s</p>
                        </td>
                      </tr>
                    </table>

                    <!-- Divider -->
                    <hr style="border:none;border-top:1px solid #334155;margin:28px 0;"/>

                    <!-- Disclaimer -->
                    <p style="margin:0;color:#475569;font-size:12px;line-height:1.6;">%s</p>

                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="background:#0f172a;padding:20px 40px;text-align:center;border-top:1px solid #1e293b;">
                    <p style="margin:0;color:#334155;font-size:11px;">© 2025 AppName · Sent to you because you made a request on our platform.</p>
                    <p style="margin:6px 0 0;color:#334155;font-size:11px;">This is an automated email — please do not reply.</p>
                  </td>
                </tr>

              </table>
            </td></tr>
          </table>

        </body>
        </html>
        """.formatted(title, title, intro, otp, expiry, disclaimer);
    }
}