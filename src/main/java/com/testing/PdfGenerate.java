package com.testing;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfGenerate {
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

	public void m1() {
		String notepadContent = "harikumar";
		String name = "hari";
		try {
			byte[] pdfData = createPDF(notepadContent, name);
			String filePath = "C:\\test\\output.pdf";

            // Write the PDF data to the file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(pdfData);
            }


			System.out.println(pdfData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		PdfGenerate p = new PdfGenerate();
		p.m1();

	}
}
