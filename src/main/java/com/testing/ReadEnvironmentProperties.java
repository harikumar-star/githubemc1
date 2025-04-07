package com.testing;

import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

public class ReadEnvironmentProperties {

	public static void main(String[] args) {
		
	
		Map<String, String> envVars = System.getenv();

        // Print all environment variables
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }

	}

}
