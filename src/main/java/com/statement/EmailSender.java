package com.statement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailSender {
	private final BlockingQueue<byte[]> queue;
	private final JavaMailSender mailSender;
	private final ExecutorService executorService;
	private volatile boolean running = true;

	private static final String FILE_DIRECTORY = "C:\\test\\";
	private static final String FILE_DIRECTORY_pdf = "C:\\test\\pdf\\";

	public EmailSender(BlockingQueue<byte[]> queue, JavaMailSender mailSender) {
		this.queue = queue;
		this.mailSender = mailSender;
		this.executorService = Executors.newFixedThreadPool(2);
		new Thread(this::processQueue).start();
	}

	private void processQueue() {
		while (running) {
			try {
				byte[] serializedData = queue.take();
				Vector<String> records = deserialize(serializedData);

				for (String record : records) {
					executorService.submit(() -> {
						try {
							processRecord(record);
						} catch (Exception e) {
							System.err.println("❌ Error processing record: " + record);
							throw new RuntimeException(e); // Allow the exception to propagate
						}
					});
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.out.println("Email processing thread interrupted, shutting down...");
				break;
			} catch (Exception e) {
				System.err.println("Error processing email queue: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private void processRecord(String record) {

		try {
			String[] parts = record.split(" - ");
			if (parts.length < 3) {
				System.err.println("Invalid record format: " + record);
				return;
			}

			String name = parts[0].trim();
			String email = parts[1].trim();
			String mailCode = parts[2].trim();
			try {
				System.out.println("The processRecord=========== " + record);
				String notepadContent = readNotepadContent(mailCode);
				byte[] pdfData = createPDF(notepadContent, name);
				System.out.println("PDF---" + pdfData);

				try (FileOutputStream fos = new FileOutputStream(FILE_DIRECTORY_pdf + name + ".pdf")) {
					fos.write(pdfData);
				}

				sendEmailWithAttachment(name, email, pdfData, mailCode);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Use username as password for encryption

		} catch (Exception e) {
			System.err.println("Error processing record: " + record);
			e.printStackTrace();
		}
	}

	private String readNotepadContent(String mailCode) {
		String filePath = FILE_DIRECTORY + mailCode + ".txt";
		File file = new File(filePath);

		if (!file.exists()) {
			System.err.println("Notepad file not found for mailCode: " + mailCode);
			return "No content available for mailCode: " + mailCode;
		}

		try {
			List<String> lines = Files.readAllLines(Paths.get(filePath));
			return String.join("\n", lines);
		} catch (IOException e) {
			System.err.println("Error reading file: " + filePath);
			e.printStackTrace();
			return "Error reading file.";
		}
	}

	private byte[] createPDF(String content, String password) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Document document = new Document();
			PdfWriter writer = PdfWriter.getInstance(document, outputStream);

			// Encrypt the PDF with the username as password
			writer.setEncryption(password.getBytes(), password.getBytes(), PdfWriter.ALLOW_PRINTING,
					PdfWriter.ENCRYPTION_AES_128);

			document.open();
			document.add(new Paragraph(content));
			document.close();

			return outputStream.toByteArray();
		} catch (Exception e) {
			System.err.println("Error creating encrypted PDF");
			e.printStackTrace();
			return new byte[0];
		}
	}

	private void sendEmailWithAttachment(String name, String email, byte[] pdfData, String mailCode) {
	    System.out.println("⚡ Sending email to: " + email);
	    System.out.println("🔑 Email subject: Hello " + name);
	    System.out.println("📎 Attachment size: " + pdfData.length + " bytes");
	    
	    int maxRetries = 3; // Number of retry attempts
	    int retryCount = 0;
	    long waitTime = 500; // Initial wait time in milliseconds (0.5 seconds)
	    
	    System.out.println("sendEmail attachment===================");
	    
	    while (retryCount < maxRetries) {
	        try {
	            System.out.println("🔄 Retry Attempt: " + (retryCount + 1) + " for email: " + email);
	            
	            MimeMessage message = mailSender.createMimeMessage();
	            MimeMessageHelper helper = new MimeMessageHelper(message, true);
	            
	            helper.setTo(email);
	            helper.setSubject("Hello " + name);
	            helper.setText("Dear " + name + ",\n\nPlease find the attached password-protected document for mailCode: "
	                    + mailCode + ".\n\n" + "🔒 **Password:** Your username (" + name + ")\n\n" + "Regards,\nYour Company");
	            
	            if (pdfData.length > 0) {
	                helper.addAttachment(mailCode + ".pdf", new ByteArrayResource(pdfData));
	            }
	            
	            mailSender.send(message);
	            System.out.println("✅ Email sent successfully to: " + email);
	            return; // Exit loop if successful
	            
	        } catch (MailAuthenticationException e) {
	            System.err.println("❌ Authentication failed: Check SMTP credentials.");
	            break; // No need to retry on authentication failure
	            // if return added no auth checks done
	        } catch (MailSendException e) {
	            System.err.println("⚠️ Mail server unreachable, retrying...");
	        } catch (MessagingException e) {
	            System.err.println("📧 Email sending issue: " + e.getMessage());
	        } catch (Exception e) {
	            System.err.println("⚠️ Unexpected error: " + e.getMessage());
	        }
	        
	        retryCount++;
	        System.err.println("⚠️ Attempt " + retryCount + " failed for: " + maskEmail(email));
	        
	        if (retryCount < maxRetries) {
	            try {
	                System.out.println("⏳ Waiting " + waitTime + "ms before retry...");
	                Thread.sleep(waitTime);
	                waitTime = Math.min(waitTime * 2, 5000); // Exponential backoff, max 5 sec
	            } catch (InterruptedException ie) {
	                Thread.currentThread().interrupt();
	                System.err.println("⛔ Retry interrupted");
	                break;
	            }
	        } else {
	            System.err.println("❌ Email sending failed after " + maxRetries + " attempts for: " + maskEmail(email));
	        }
	    }
	}

	private String maskEmail(String email) {
	    int atIndex = email.indexOf("@");
	    if (atIndex > 2) {
	        return email.substring(0, 2) + "*****" + email.substring(atIndex);
	    }
	    return "*****";
	}


	private Vector<String> deserialize(byte[] data) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
				ObjectInputStream ois = new ObjectInputStream(bis)) {
			return (Vector<String>) ois.readObject();
		}
	}

	@PreDestroy
	public void shutdown() {
		System.out.println("Shutting down EmailSender...");
		running = false;
		executorService.shutdown();
	}
}