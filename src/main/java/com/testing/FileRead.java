package com.testing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileRead {
	private static final String FILE_DIRECTORY = "C:\\test\\";

	public static void main(String[] args) {

		String mailCode = "AL001";

		String filePath = FILE_DIRECTORY + mailCode + ".txt";
		File file = new File(filePath);

		if (!file.exists()) {
			System.err.println("Notepad file not found for mailCode: " + mailCode);

		}

		try {
			List<String> lines = Files.readAllLines(Paths.get(filePath));

			System.out.println(String.join("\n", lines));

		} catch (IOException e) {
			System.err.println("Error reading file: " + filePath);
			e.printStackTrace();

		}

	}
}
