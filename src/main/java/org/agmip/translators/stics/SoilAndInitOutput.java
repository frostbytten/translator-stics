package org.agmip.translators.stics;

import static org.agmip.translators.stics.util.SticsUtil.convertFirstLevelRecords;
import static org.agmip.translators.stics.util.SticsUtil.convertNestedRecords;
import static org.agmip.translators.stics.util.SticsUtil.newFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.stics.util.SticsUtil;
import org.agmip.translators.stics.util.VelocityUtil;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

public class SoilAndInitOutput implements TranslatorOutput {

	public static String SOIL_TEMPLATE_FILE = "/soil_template.vm";
	public static String INI_TEMPLATE_FILE = "/ini_template.vm";
	public File soilFile;
	public File initFile;

	public void mergeSoilAndInitializationData(ArrayList<LinkedHashMap<String, String>> soilsData, ArrayList<LinkedHashMap<String, String>> initData) {
		int index = 0;
		System.out.println("Init data size : "+initData.size());
		System.out.println("Soil data size : "+soilsData.size());
		for (LinkedHashMap<String, String> soilData : soilsData) {
			if (initData.get(index).get(SoilAggregationTool.ICBL).equals(soilData.get(SoilAggregationTool.SLLB))) {
				soilData.putAll(initData.get(index));
			} else {
				System.err.println("Unable to merge soil information, inconsistent soil information");
			}
			index=index+1;
		}
	}

	public void writeFile(String filePath, Map data) {
		BucketEntry soilBucket;
		BucketEntry iniBucket;
		SoilAggregationTool soilAgg;
		LinkedHashMap<String, String> firstLevelSoilData;
		ArrayList<LinkedHashMap<String, String>> nestedSoilData;
		LinkedHashMap<String, String> firstLevelInitData;
		ArrayList<LinkedHashMap<String, String>> nestedInitData;
		ArrayList<LinkedHashMap<String, String>> aggregatedSoilData;

		soilAgg = new SoilAggregationTool();

		try {
			soilBucket = MapUtil.getBucket(data, "soil");
			iniBucket = MapUtil.getBucket(data, "initial_condition");
			// Soil structure
			firstLevelSoilData = soilBucket.getValues();
			nestedSoilData = soilBucket.getDataList();
			// Initialization structure
			firstLevelInitData = soilBucket.getValues();
			nestedInitData = iniBucket.getDataList();
			// Put soil information in the same map
			mergeSoilAndInitializationData(nestedSoilData, nestedInitData);
			// Merge soil layers
			aggregatedSoilData = soilAgg.mergeSoilLayers(nestedSoilData);
			// Generate initialization file
			String content = generateInitializationFile(firstLevelInitData, aggregatedSoilData);
			String soilId = MapUtil.getValueOr(firstLevelSoilData, "soil_id", SticsUtil.defaultValue("soil_id"));
			initFile = newFile(content, filePath, soilId + "_ini" + ".xml");
			// Generate soil file
			content = generateSoilFile(firstLevelSoilData, aggregatedSoilData);
			soilFile = newFile(content, filePath, "sols.xml");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String generateInitializationFile(LinkedHashMap<String, String> firstLevelInitData, ArrayList<LinkedHashMap<String, String>> aggregatedSoilData) {
		Context velocityContext;
		// Convert and put default values
		// these params have default values : icnh4,icno3,ich2o
		convertFirstLevelRecords(firstLevelInitData);
		//SticsUtil.defaultValueFor(Arrays.asList(, firstLevelInitData);
		convertNestedRecords(aggregatedSoilData, Arrays.asList(new String[]{"icnh4","icno3","ich2o"}));
		velocityContext = VelocityUtil.fillVelocityContext(firstLevelInitData, aggregatedSoilData);
		return VelocityUtil.runVelocity(velocityContext, INI_TEMPLATE_FILE);
	}

	

	public String generateSoilFile(LinkedHashMap<String, String> firstLevelSoilData, ArrayList<LinkedHashMap<String, String>> aggregatedSoilData) {
		Context velocityContext;
		// Convert and put default values
		// these params have default values : slcly, salb, slphw ,sksat, caco3,
		// sloc
		convertFirstLevelRecords(firstLevelSoilData);
		SticsUtil.defaultValueFor(Arrays.asList(new String[]{"slcly", "salb", "slphw" ,"sksat", "caco3"}), firstLevelSoilData);
		convertNestedRecords(aggregatedSoilData, Arrays.asList(new String[]{"sksat"}));
		// for the sloc param we'll use only the first layer parameter
		firstLevelSoilData.put("sloc", SticsUtil.convert("sloc", MapUtil.getValueOr(aggregatedSoilData.get(0), "sloc", "0.0")));
		velocityContext = VelocityUtil.fillVelocityContext(firstLevelSoilData, aggregatedSoilData);
		return VelocityUtil.runVelocity(velocityContext, SOIL_TEMPLATE_FILE);
	}

	public File getSoilFile() {
		return soilFile;
	}

	public File getInitializationFile() {
		return initFile;
	}

	public static void main(String[] args) {
		// new SoilVelocityConverter().perform();
	}
}
