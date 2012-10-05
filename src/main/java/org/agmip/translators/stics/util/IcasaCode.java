package org.agmip.translators.stics.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.agmip.translators.stics.WeatherOutput;

public class IcasaCode {
	static String CODE_UNKNOWN = "code_unknown";
	static Properties codeMap;
	static{
		InputStream inputStream = WeatherOutput.class.getResourceAsStream("/code-mapping.properties");
		codeMap = new Properties();
		try {
			codeMap.load(inputStream);
			System.out.println(codeMap);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static String toSticsCode(String icasaCode){
		return (codeMap.containsKey(icasaCode) ? (String) codeMap.get(icasaCode) : CODE_UNKNOWN); 
	}
}
