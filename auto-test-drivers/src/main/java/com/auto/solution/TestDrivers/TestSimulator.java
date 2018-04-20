package com.auto.solution.TestDrivers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.util.SystemOutLogger;

import com.auto.solution.Common.EmailNotificationHandler;
import com.auto.solution.Common.Property;
import com.auto.solution.Common.ResourceManager;
import com.auto.solution.Common.Utility;
import com.auto.solution.Common.Property.ERROR_MESSAGES;
import com.auto.solution.Common.RecoveryObjectsMapper;
import com.auto.solution.DatabaseManager.AccessResultsetData;
import com.auto.solution.DatabaseManager.ConnectDatabase;
import com.auto.solution.FileManager.CsvManager;

public class TestSimulator {

 	private HashMap<String, String> objDefInfo = new HashMap<String, String>();
 	
 	private String[] testDataContents = null;
 	
 	private  TestDrivers testSimulator = null;
 	
 	private CsvManager csvManager = null;
 	
 	private ResourceManager rm;
 	
 	private InvokeAPI api_caller = null;
 	
 	private RecoveryObjectsMapper objMapper = null;
 	
 	private EmailNotificationHandler mailHandler = null;
 	
 	public TestSimulator(ResourceManager rmanager){
 		
 		this.rm = rmanager;
 		
 		this.csvManager = new CsvManager(this.rm);
 		
 		api_caller = new InvokeAPI(rmanager);	
 		
 		mailHandler = new EmailNotificationHandler(rm);
 	}
 	
 	public void setTestObjectInfo(HashMap<String, String> currentTestObjectInfo){
 		this.objDefInfo = null;
 		
 		this.objDefInfo = currentTestObjectInfo;
 	}
 	
 	public void consumeTestObjectsNameToSkipDuringRecovery(RecoveryObjectsMapper objMapper){
		this.objMapper = new RecoveryObjectsMapper();
		this.objMapper = objMapper;
	}

 	
 	public void enableTestDriver(String testDriverKey){
 		
 		testSimulator = null;
 		
 		if(testDriverKey.contains(Property.DESKTOP_WEB_TESTDRIVER_KEYWORD)){
 			testSimulator = new DesktopWebTestDriverImpl(this.rm);
 			((DesktopWebTestDriverImpl)testSimulator).setRecoveryObjectMapper(this.objMapper);
 	    }
 	    else if(testDriverKey.contains(Property.MOBILE_APP_ANDRIOD_TESTDRIVER_KEYWORD)){
 	    	testSimulator = new MobileAndriodTestDriverImpl(this.rm);
 	    }
 	    else if(testDriverKey.contains(Property.MOBILE_WEB_TESTDRIVER_KEYWORD)){
 	    	testSimulator = new MobileWebTestDriverImpl(this.rm);
 	    	((MobileWebTestDriverImpl)testSimulator).setRecoveryObjectMapper(this.objMapper);
 	    }
 	    else if(testDriverKey.contains(Property.MOBILE_IOS_TESTDRIVER_KEYWORD)){
 	    	testSimulator = new MobileIOSTestDriverImpl(this.rm);
 	    }
 	}
 	
 	private void prepareTestData(String testData) throws Exception{
 		this.testDataContents = null;
 		ArrayList<String> datalist = new ArrayList<String>();
 		int count = 0;
 		int datacount = 0;
 		if(Property.StrategyString.contains(Property.STRATEGY_KEYWORD.DATACOUNT.toString().toLowerCase())){
 			try{
 				
 			datacount = Utility.getDataCountFromStrategy(Property.StrategyString);
 			}
 			catch(Exception e){
 				throw new Exception(Property.ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage() + "--" +e.getMessage()); 
 			}
 		}
 		String[] tempDataList = testData.split(Property.TESTDATA_SEPERATOR);
 		if(datacount != 0){
 			String ignoredHashData = "";
 			while(count != datacount-1){
 				datalist.add(tempDataList[count]);
 				count++;
 			}
 			for(int index = count;index < tempDataList.length;index++){
 				ignoredHashData = ignoredHashData + tempDataList[index] + Property.TESTDATA_SEPERATOR;;
 			}
 			datalist.add(ignoredHashData);
 			this.testDataContents = datalist.toArray(new String[0]);
 		}
 		else{
 			this.testDataContents = tempDataList;
 		}
 		
 		 		
 	}
 	
 	
 	public void simulateTestStep(String stepAction,String testData,String testObject, String strategyModifier,boolean isReusableTestKeyword){
 		
 		if(isReusableTestKeyword){
 			return;
 		}
 		Property.CURRENT_TESTSTEP = stepAction;
 		
 		//prepareTestData(testData);
 		
 		TestObjectDetails objCurrentObjectDetails = new TestObjectDetails(this.objDefInfo);
 		
 		testSimulator.injectTestObjectDetail(objCurrentObjectDetails); // Setting up the object definition.
 		
 		Property.StepExecutionTime = Utility.getCurrentDateAndTimeInStringFormat();
 		
 		//If Ignore option is enabled then Ignore the test step.
 		if(Property.LIST_STRATEGY_KEYWORD.contains(Property.STRATEGY_KEYWORD.IGNORE_STEP.toString())){
 			Property.Remarks = Property.ERROR_MESSAGES.STEP_IGNORED.getErrorMessage();
 			Property.StepStatus =  Property.PASS;
 			return;
 		}
 		
 		try{
 			prepareTestData(testData);
 			if(stepAction.toLowerCase().equals("initializeapp")){
 				
 				//TODO : Replace the STep description with actual test data used. 
 					String URL = "";
 					try{
 						URL = this.testDataContents[0];
 						if(URL.equals("")){
 							URL = Property.ApplicationURL;
 						}
 					}
 				catch(ArrayIndexOutOfBoundsException ae){
 					throw new Exception(Property.ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 				}
 					testSimulator.initializeApp(URL);
 			}
 			else if(stepAction.toLowerCase().equals("check")){
 				testSimulator.check();				
 			}
 			else if(stepAction.toLowerCase().equals("uncheck")){
 				testSimulator.uncheck();
 			}
 			else if(stepAction.toLowerCase().equals("sendkey") ){
 				String textToType = "";
 				try{
 					textToType = this.testDataContents[0];					      
 				}
 			catch(ArrayIndexOutOfBoundsException ae){
 				throw new Exception(Property.ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 			}
 				Utility.setKeyValueToGlobalVarMap(testObject, textToType);
 				
 				testSimulator.sendKey(textToType);	
 			}
 			
 			else if(stepAction.toLowerCase().equals("uploadfile")){
 				String fileToUpload = "";
 				try{
 					fileToUpload = this.testDataContents[0];					      
 				}
 			catch(ArrayIndexOutOfBoundsException ae){
 				throw new Exception(Property.ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 			}
 				Utility.setKeyValueToGlobalVarMap(testObject, fileToUpload);
 				
 				testSimulator.uploadFile(fileToUpload);
 			}
 			
 			else if(stepAction.toLowerCase().equals("navigateurl")){
 				String URL = "";
 				
 				try{
 					
 					URL = this.testDataContents[0];
 					
 					if(URL.equals("")){
 						throw new Exception(Property.ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 					}
 				}
 			catch(ArrayIndexOutOfBoundsException ae){
 				throw new Exception(Property.ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 			}
 				
 				testSimulator.navigateURL(URL);
 			}
 			else if(stepAction.toLowerCase().equals("isresourceloaded")){
 				testSimulator.isResourceLoaded();
 			}
 			else if(stepAction.toLowerCase().equals("istextpresent")){
 				String textToVerify = "";
 				try{
 					textToVerify = this.testDataContents[0];		      
 					if(textToVerify.equals("")){
 						throw new Exception(Property.ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 					}
 				}
 			catch(ArrayIndexOutOfBoundsException ae){
 				throw new Exception(Property.ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 			}
 								
 				testSimulator.isTextPresent(textToVerify);
 			}
 			else if(stepAction.toLowerCase().equals("click")){
 				testSimulator.click();
 			}
 			else if(stepAction.toLowerCase().equals("shutdown")){
 				testSimulator.shutdown();
 			}
 			else if(stepAction.toLowerCase().equals("isobjectthere")){
 				testSimulator.isObjectThere();
 			}
 			else if(stepAction.toLowerCase().equals("fireevents")){
 				String codeSnippet = this.testDataContents[0];
 				if(codeSnippet.equals("")){
 					throw new Exception(ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 				}
 				String eventResult = testSimulator.fireEvents(codeSnippet);
 				Utility.setKeyValueToGlobalVarMap(testObject, eventResult);
 			}
 			else if(stepAction.toLowerCase().equals("selectitemfromdropdown")){
 				String optionToSelect = this.testDataContents[0];
 				if(optionToSelect == null || optionToSelect == ""){
 					throw new Exception(ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 				}
 				String selectedOption = testSimulator.select(optionToSelect);
 				Utility.setKeyValueToGlobalVarMap(testObject, selectedOption);
 			}
 			else if(stepAction.toLowerCase().equals("gettestelementattribute")){
 				String propertyToFetch = testDataContents[0];
 				if(propertyToFetch == null || propertyToFetch == ""){
 					throw new Exception(ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 				}
 				String attributeValue = testSimulator.getTestElementAttribute(propertyToFetch);
 				Utility.setKeyValueToGlobalVarMap(testObject, attributeValue);
 			}
 			else if(stepAction.equalsIgnoreCase("presskeyboardkey")){
 				String key = this.testDataContents[0];
 				if(key == null || key == ""){
 					throw new Exception(ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 				}
 				testSimulator.pressKeyboardKey(key);
 			}
 			else if(stepAction.equalsIgnoreCase("verifytestelementproperty")){
 				try{
 					if(testDataContents.length < 2){
 						throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 					}
 				String propertyToVerify = testDataContents[0];
 				if(propertyToVerify == null || propertyToVerify == ""){
 					throw new Exception(ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 				}
 				String expectedTestElementAttribute = testDataContents[1];
 				testSimulator.verifyTestElementAttribute(propertyToVerify, expectedTestElementAttribute);
 				
 				}
 				catch(Exception e){
 					throw e;
 				}
 				
 			}
 			else if(stepAction.equalsIgnoreCase("verifytestelementattributenotpresent")){
 				try{
 					if(testDataContents.length < 2){
 						throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 					}
 				String propertyToVerify = testDataContents[0];
 				if(propertyToVerify == null || propertyToVerify == ""){
 					throw new Exception(ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 				}
 				String expectedTestElementAttribute = testDataContents[1];
 				testSimulator.verifyTestElementAttributeNotPresent(propertyToVerify, expectedTestElementAttribute);
 				
 				}
 				catch(Exception e){
 					throw e;
 				}
 				
 			}
 			else if(stepAction.equalsIgnoreCase("hover")){
 				testSimulator.hover();
 			}
 			else if(stepAction.equalsIgnoreCase("sleep")){
 				long timeToSleepInMilliSeconds;
 				try{
 				timeToSleepInMilliSeconds = Long.parseLong(testDataContents[0]);}
 				catch(Exception e){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage() + "--" +   e.getMessage());
 				}
 				testSimulator.sleep(timeToSleepInMilliSeconds);
 			}
 			else if(stepAction.equalsIgnoreCase("isobjectnotthere")){
 				testSimulator.isObjectNotThere();
 			}
 			else if(stepAction.equalsIgnoreCase("waitforobjectnotpresent")){
 				testSimulator.waitUntilObjectIsThere();
 			}else if (stepAction.equalsIgnoreCase("swipeToElementVisible")){
 				String swipeType = this.testDataContents[0];
 				if(swipeType == null || swipeType == ""){
 					throw new Exception(ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 				}
 				testSimulator.swipetoElementVisible(testDataContents[0]);
 			}
 			else if(stepAction.equalsIgnoreCase("gettestobjectcount")){
 				String testObjectCount = testSimulator.getTestObjectCount();
 				Utility.setKeyValueToGlobalVarMap(testObject, testObjectCount);
 			}
 			else if(stepAction.equalsIgnoreCase("docalculation")){
 				if(testDataContents.length < 2){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				int index  = testData.indexOf("#");
 				testData = testData.substring(index+1);
 				String scriptResult = Utility.executeJava(testData);
 				Utility.setKeyValueToGlobalVarMap(testDataContents[0].trim(), scriptResult);
 			}
 			else if(stepAction.equalsIgnoreCase("replaceall")){
 				if(testDataContents.length < 3){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 			String outputValue	= Utility.replaceAll(testDataContents[1], testDataContents[2]);
 			Utility.setKeyValueToGlobalVarMap(testDataContents[0], outputValue);		
 			}
 			else if(stepAction.equalsIgnoreCase("fetchValueFromVariable")){
 				if(testDataContents.length < 4){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 			String outputValue	= Utility.splitAndReturnIndexedValue(testDataContents[1], testDataContents[2], testDataContents[3]);
 			Utility.setKeyValueToGlobalVarMap(testDataContents[0], outputValue);		
 			}
 			else if(stepAction.equalsIgnoreCase("assertoninputvalue")){
 				if(testDataContents.length != 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				else{
 					boolean bFlag ;
 					bFlag = Utility.assertOnInputValue(testDataContents[0]);
 					if(!bFlag){
 						String errMessage = ERROR_MESSAGES.ERR_TESTDATA_MATCH.getErrorMessage().replace("{ACTUAL_STRING}", testDataContents[0]);					
 						throw new Exception(errMessage);
 					}
 				}
 			}
 			else if(stepAction.equalsIgnoreCase("verifyPdfText")){
 				if(testDataContents.length < 2){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}	
 				String tempFile = null; 
 				try{
 				tempFile =	rm.getLocationForExternalFilesInResources().replace("{EXTERNAL_FILE_NAME}", testDataContents[0]);
 				tempFile =  tempFile.replace("{PROJECT_NAME}", Property.PROJECT_NAME);
 				String filepath = Utility.getAbsolutePath(tempFile);
 					if(testDataContents.length == 3){
 						String[] pageRange = testDataContents[2].split("to");
 						if(pageRange.length==2){
 							Utility.verifyPdfText(filepath,testDataContents[1],Integer.parseInt(pageRange[0].trim()),Integer.parseInt(pageRange[1].trim()));
 						}else{
 	 						Utility.verifyPdfText(filepath,testDataContents[1],Integer.parseInt(testDataContents[2].trim()),Integer.parseInt(testDataContents[2].trim()));
 						}
 					}
 					else{
 						Utility.verifyPdfText(filepath,testDataContents[1],-1,-1);
 					}
 				}catch(Exception ex){
 					throw ex;
 				}
 			}
 			else if(stepAction.equalsIgnoreCase("verifysortbyfeature")){
 				if(testDataContents.length != 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String sortType = testDataContents[0];
 				testSimulator.verifySortByFeature(sortType);
 			}
 			else if(stepAction.equalsIgnoreCase("refresh")){
 				testSimulator.refresh();
 			}
 			else if(stepAction.equalsIgnoreCase("setvariable")){
 				if(testDataContents.length != 2){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String key = testDataContents[0];
 				String value = testDataContents[1];
 				Utility.setKeyValueToGlobalVarMap(key, value);
 			}
 			else if(stepAction.equalsIgnoreCase("changedateformat")){
 				if(testDataContents.length != 3){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String oldDateFormat = testDataContents[1];
 				String newFormat = testDataContents[2];
 				String newDateFormat = Utility.changeDateFormat(oldDateFormat, newFormat);
 				Utility.setKeyValueToGlobalVarMap(testDataContents[0], newDateFormat);
 			}
 			else if(stepAction.equalsIgnoreCase("verifypageproperty")){
 				if(testDataContents.length != 2){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String pageAttribute = testDataContents[0];
 				String expectedValue = testDataContents[1];
 				testSimulator.verifyPageAttribute(pageAttribute, expectedValue);
 				
 			}
 			else if(stepAction.toLowerCase().equals("getpageproperties")){
 				if(testDataContents.length < 2){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String propertyToFetch = testDataContents[1];
 				if(propertyToFetch == null || propertyToFetch == ""){
 					throw new Exception(ERROR_MESSAGES.ER_MISSING_TESTDATA.getErrorMessage());
 				}
 				String propertyValue = testSimulator.getPageProperties(propertyToFetch);
 				Utility.setKeyValueToGlobalVarMap(testDataContents[0], propertyValue);
 			}
 			else if(stepAction.toLowerCase().equals("verifybrokenlinks")){
 				if(testDataContents.length < 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String urlSource = testDataContents[0];
 				testSimulator.verifyAndReportBrokenLinksFromPages(urlSource);
 			}
 			else if(stepAction.toLowerCase().equalsIgnoreCase("browsernavigation")){
 				if(testDataContents.length != 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String navigationOption = testDataContents[0].trim();
 				testSimulator.browserNavigation(navigationOption);
 			}
 			
 			else if(stepAction.equalsIgnoreCase("executequery")){
 				if(testDataContents.length == 1){
 					String query = testDataContents[0].trim();
 					ConnectDatabase.executeQuery(query,null);
 				}
 				else if(testDataContents.length == 2){
 					String query = testDataContents[0].trim();
 					String dbName = testDataContents[1].trim();
 					ConnectDatabase.executeQuery(query,dbName);
 				}
 				else{
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 			}
 			else if(stepAction.equalsIgnoreCase("getqueryresult"))
 			{
 				if(testDataContents.length != 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 
 				String collectionName;
 				String collectionIndex;
 				String globalMapKeyToStoreData;
 				try{
 					String[] queryParameters = testDataContents[0].trim().split(":=");
 					globalMapKeyToStoreData = queryParameters[0].trim();
 					String[] collectionParams = queryParameters[1].split("\\(");
 					collectionName = collectionParams[0].trim();
 					collectionIndex = collectionParams[1].replace(")", "").trim();
 				}
 				catch(ArrayIndexOutOfBoundsException e){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				AccessResultsetData.fetchResultSetData(collectionName, collectionIndex, globalMapKeyToStoreData);
 				
 			}		
 			else if(stepAction.equalsIgnoreCase("verifyscocontents")){
 				if(testDataContents.length != 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String scoUrlSource = testDataContents[0].trim();
 				testSimulator.verifyAndReportSCO(scoUrlSource);
 			}
 			else if(stepAction.toLowerCase().equalsIgnoreCase("verifyandreportsectionlinks")){
 				if(testDataContents.length != 2){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				testSimulator.verifyAndReportSectionLinks(testDataContents[0].trim(),testDataContents[1].trim());
 			}
 			else if(stepAction.toLowerCase().equalsIgnoreCase("alert")){
 				if(testDataContents.length != 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				testSimulator.handleAlert(testDataContents[0].trim());
 			}
 			else if(stepAction.equalsIgnoreCase("verifyRedirectedUrls")){
 				testSimulator.verifyAndReportRedirectdUrls();
 			}
 			else if(stepAction.equalsIgnoreCase("clickOnCo_ordinates")){
 				if(testDataContents.length == 2){
 					int x_axis = Integer.parseInt(testDataContents[0].trim());
 					int y_axis = Integer.parseInt(testDataContents[1].trim());
 					testSimulator.clickOnCo_ordinates(x_axis,y_axis);
 				}
 				else{
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				
 			}
 			
 			else if(stepAction.toLowerCase().equalsIgnoreCase("deleteCompletefile")){
 				if(testDataContents.length > 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				csvManager.deleteWholeCsvFromTheFolder(testDataContents);
 			}
 			
 			else if(stepAction.toLowerCase().equalsIgnoreCase("replaceinfile")){
 				if(testDataContents.length != 4){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				csvManager.replaceAnyValueInCsv(testDataContents);
 			}
 			
 			else if(stepAction.toLowerCase().equalsIgnoreCase("verifyinfile")){
 				if(testDataContents.length != 4){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				csvManager.verifyAnyValueInCsv(testDataContents);
 			}
 
 			else if(stepAction.toLowerCase().equalsIgnoreCase("deleteanyvalueinfile")){
 				if(testDataContents.length != 3){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				csvManager.deleteAnyValueInCsv(testDataContents);
 			}
 			
 			else if(stepAction.toLowerCase().equalsIgnoreCase("appendinfile")){
 				if(testDataContents.length != 2){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				csvManager.appendAnyValueInCsv(testDataContents);
 			}
 			
 			else if(stepAction.toLowerCase().equalsIgnoreCase("getelementdimension")){
 				 				String dimension = testSimulator.getElementDimension();
 				 				Utility.setKeyValueToGlobalVarMap(testObject, dimension);
 				 			}
 			else if(stepAction.toLowerCase().equalsIgnoreCase("invokeapi") ){
 				if(testDataContents.length < 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				HashMap<String, String> propertiesMap = new HashMap<String, String>();
 				String soapui_project_name = testDataContents[0].trim();
 				if(testDataContents.length > 1){
 					try{
 					    //key=value || key=value.
 					
 						String[] key_value_pairs = testDataContents[1].split(Pattern.quote("||"));
 						for (String key_value_pair : key_value_pairs) {
 							String keyValue = key_value_pair.trim();
 							int index = key_value_pair.indexOf("=");
 							String key = keyValue.substring(0, index);
 							String value = keyValue.substring(index+1,keyValue.length());
 							System.out.println(value);
 							propertiesMap.put(key, value);
 							}
 						
 					}
 					catch(Exception e){
 						throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage() + "--"  + e.getMessage());
 					}
 				}
 				
 				api_caller.setAPIProjectReference(soapui_project_name, propertiesMap);
 				Thread api_thread = new Thread(api_caller);
 				api_thread.setDaemon(true);
 				api_thread.start();
 				api_thread.join();
 				List<String> statusDetails = api_caller.getListOfApisTestCaseStatus();
 				if(statusDetails.get(0) == "FAILED"){
 					throw new Exception(statusDetails.get(1));
 				}
 				if(testDataContents.length == 3){
 					String[] out_key_value_pairs = testDataContents[2].split(Pattern.quote("||"));
 					List <String> outProp = new ArrayList<String>();
 					for (String prop : out_key_value_pairs) {
 						outProp.add(prop);
					}
 					
 					HashMap<String, String> outputPropertiesMap = api_caller.getTestCaseProperty(outProp);
 					if(outputPropertiesMap != null)
 					{
 					for (String property_key : outputPropertiesMap.keySet()) {
 						String property_value = outputPropertiesMap.get(property_key);
 						Utility.setKeyValueToGlobalVarMap(property_key, property_value);
 					}
 				}
 				}
 				
 				
 			}
 			
 			// my changes here
 			
 			else if(stepAction.toLowerCase().equalsIgnoreCase("setFilterOn") ){
 				HashMap<String, String> propertiesMap=Utility.getPropertiesFromTestData(testDataContents);
 				String soapui_project_name =Property.Filter_Project_Name ;
 				if (soapui_project_name.equals(""))
 				{
 					throw new Exception(ERROR_MESSAGES.ERR_FILTER_PROJECT_NAME_IS_NULL.getErrorMessage());
 				}
 				api_caller.setAPIProjectReference(soapui_project_name, propertiesMap);
 				Thread api_thread = new Thread(api_caller);
 				api_thread.setDaemon(true);
 				api_thread.start();
 				api_thread.join();
 				List<String> statusDetails = api_caller.getListOfApisTestCaseStatus();
 				if(statusDetails.get(0) == "FAILED"){
 					throw new Exception(statusDetails.get(1));
 				}
 				if(testDataContents.length == 3){
 					String[] out_key_value_pairs = testDataContents[2].split(Pattern.quote("||"));
 					List <String> outProp = new ArrayList<String>();
 					for (String prop : out_key_value_pairs) {
 						outProp.add(prop);
 				}
 					
 					HashMap<String, String> outputPropertiesMap = api_caller.getTestCaseProperty(outProp);
 					if(outputPropertiesMap != null)
 					{
 					for (String property_key : outputPropertiesMap.keySet()) {
 						String property_value = outputPropertiesMap.get(property_key);
 						Utility.setKeyValueToGlobalVarMap(property_key, property_value);
 					}
 				}
 				}
 				
 			}
 		
 		
 			else if(stepAction.toLowerCase().equalsIgnoreCase("resizecurrentwindow")){
 				if(testDataContents.length != 2){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				int x_coordinates = Integer.parseInt(testDataContents[0]);
 				int y_coordinates = Integer.parseInt(testDataContents[1]);
 				
 				testSimulator.resizeCurrentWindow(x_coordinates, y_coordinates);
 				
 			}
 			else if(stepAction.toLowerCase().equalsIgnoreCase("resizetodeafult")){
 				testSimulator.resizeToDeafult();
 			}
 			else if(stepAction.toLowerCase().equalsIgnoreCase("closeBrowserWindow")){
 				testSimulator.shutdown();
 			}
 			else if(stepAction.toLowerCase().equalsIgnoreCase("emailnotificationreceived")){
 				
 				if(testDataContents.length < 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String email_id = Utility.getValueForKeyFromGlobalVarMap("email_server_name"); 				
 				String email_password = Utility.getValueForKeyFromGlobalVarMap("email_server_password"); 				
 				mailHandler.connectToMailServerInbox(email_id, email_password); 				
 				String email_subject_pattern = testDataContents[0]; 				
 				boolean emailReceived = mailHandler.isEmailSentToIndox(email_subject_pattern); 				
 				if(!emailReceived){
 					throw new Exception(Property.ERROR_MESSAGES.ERR_IN_GETTING_EMAIL_CONTENTS_BY_SUBJECT.getErrorMessage() + email_subject_pattern);
 				}
 				
 			}
 			else if(stepAction.toLowerCase().equalsIgnoreCase("iselementinemail")){

 				String cssQuery = objCurrentObjectDetails.getLocationOfTestObject();
 				boolean isElementInMail = !(mailHandler.isElementThereInMailContent(cssQuery));
 				if(!isElementInMail){
 					throw new Exception(Property.ERROR_MESSAGES.ERR_IN_FINIDING_CONTENT_IN_MAIL.getErrorMessage().replace("{CSS_QUERY}",cssQuery));
 				}
 			}
 			else if(stepAction.toLowerCase().equalsIgnoreCase("extractjserrors")){
 				if(testDataContents.length < 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String input_reference_file = testDataContents[0];
 				testSimulator.extractJSErrors(input_reference_file);
 			}
 			else if(stepAction.toLowerCase().equals("verifyinternallinks")){
 				if(testDataContents.length < 1){
 					throw new Exception(ERROR_MESSAGES.ER_SPECIFYING_TESTDATA.getErrorMessage());
 				}
 				String urlSource = testDataContents[0];
 				testSimulator.verifyInternalLinkOnWebPage(urlSource);
 			}
 			else{
 				throw new NoSuchMethodException(Property.ERROR_MESSAGES.ER_NO_STEP_ACTION.getErrorMessage());
 			}
 			Property.StepStatus = Property.PASS;
 		}
 		catch(Exception e){
 			if(Property.LIST_STRATEGY_KEYWORD.contains(Property.STRATEGY_KEYWORD.OPTIONAL.toString())){
 				Property.StepStatus = Property.PASS;
 				Property.Remarks = Property.ERROR_MESSAGES.STEP_MARKED_OPTIONAL.getErrorMessage();
 			}
 			else{
 			Property.Remarks = e.getMessage();
 			Property.StepStatus = Property.FAIL;
 			}
 		}
 		this.takeSnapshots();
 		
 	}
 	
 	private void takeSnapshots(){
 		String snapShotName = "";
 		if(Property.DEBUG_MODE.contains("off") && Property.DEBUG_MODE.contains("strict")){ 	}
 		
 		else if(!Property.DEBUG_MODE.contains("on") && Property.StepStatus == Property.FAIL ){snapShotName = testSimulator.saveSnapshotAndHighlightTarget(false);}
 		
 		else if(Property.DEBUG_MODE.contains("on")){
 			snapShotName = testSimulator.saveSnapshotAndHighlightTarget(true);
 		}
 		Property.StepSnapShot = snapShotName;
 	}
 	
}
