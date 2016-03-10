package com.yayhi.tags;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.Ostermiller.util.ExcelCSVParser;
import com.yayhi.utils.YLogger;
import com.yayhi.utils.YProperties;

/**
 * -----------------------------------------------------------------------------
 * @version 1.0
 * @author  Mark Gaither
 * @date	Aug 19, 2010
 * -----------------------------------------------------------------------------
 */

public class Mapper {

	private static YProperties iProps				= null;
	private static Properties appProps				= null;
    private static String logFilePath				= null;
    private static Boolean logIt					= null;
    private static String inputCSVFilePath			= null;
    private static String outputCSVFilePath			= null;
    private static boolean debug 					= false;
    private static boolean test						= false;
    private static boolean verbose					= false;
    private static YLogger logger					= null;
    private static boolean hasErrors				= false;
    
    // constructor
    Mapper() {
    	
    }
    
	// check if string is not empty
	public static boolean isEmpty(String s) {
    	
    	boolean empty = false;
    	
    	Pattern p = Pattern.compile("^[ ]*$");
		
		Matcher m = p.matcher(s);

		if (m.find()) {
			empty = true;
		}
		
		return empty;
		
    }

    /**
     * Sole entry point to the class and application.
     * @param args Array of String arguments.
     * @exception java.lang.InterruptedException
     *            Thrown from the Thread class.
     * @throws SQLException 
     */
    public static void main(String[] args) throws Exception {
    	
    	//*********************************************************************************************
        //* Get Command Line Arguments - overwrites the properties file value, if any
        //*********************************************************************************************
    	
    	String usage = "Usage:\n" + "java -jar tagmapper.jar [-i INPUT_CSV] [-t] (TEST and validate the input file but don't database inserts - optional) [-v] (VERBOSE mode - optional) [-d] (DEBUG - optional)\n";
    	String example = "Example:\n" + "java -jar tagmapper.jar -i /tmp/tags.csv -c 123456\n" +
    	"java -jar tagmapper.jar -i /tmp/tags.csv -d\n" +
    	"java -jar tagmapper.jar -i /tmp/tags.csv -v -d\n";

        // get command line arguments
    	if (args.length >= 2) {
    		
	    	for (int optind = 0; optind < args.length; optind++) {
	    	    
	    		if (args[optind].equals("-i")) {
	    			inputCSVFilePath = args[++optind];
				} else if (args[optind].equals("-d")) {
					debug = true; 
				} else if (args[optind].equals("-v")) {
					verbose = true; 
		    	} else if (args[optind].equals("-t")) {
					test = true;
		    	}
	    		
	    	}
        }
        else {
        	
        	System.err.println(usage);
        	System.err.println(example);
            System.exit(1);
            
        }

    	//*********************************************************************************************
        //* Get Properties File Data
        //*********************************************************************************************
    	iProps = new YProperties();
    	appProps = iProps.loadProperties();
    	
    	// set log file path
    	logFilePath = appProps.getProperty("logFilePath");
    	logIt = Boolean.valueOf(appProps.getProperty("logIt"));
    	
    	// allow command line setting of debug to over rule the properties setting
    	if (!debug)
    		debug = Boolean.parseBoolean(appProps.getProperty("debug"));
    	
    	if (debug) {
    		
			System.out.println("logFilePath: " + logFilePath);
			System.out.println("logIt: " + logIt);
			System.out.println("input CSV file: " + inputCSVFilePath);
			System.out.println("output CSV file: " + outputCSVFilePath);
			System.out.println("debug: " + debug);
			System.out.println("test: " + test);
			
    	}
    	
    	//*********************************************************************************************
        //* Set up logging
        //*********************************************************************************************
    	// create string of todays date and time
    	Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_hhmmss");

        String loggerFilePath = appProps.getProperty("logFilePath") +  "/tagmapper_" + sdf.format(cal.getTime()) + ".log";
        // log data, if true
        if (logIt.booleanValue()) {
            	
        	// open log file
    	    try {
    	    	logger = new YLogger(loggerFilePath);
    	    } catch (IOException e) {
    	    	System.out.println("exception: " + e.getMessage());
    	    }
    	    
        }
        
        // output run information to stdout
        if (verbose) {
        	if (test) {
	    		System.out.println("\tRUN MODE =\t\tTest and Validate");
	    	}
	    	else {
	    		System.out.println("\tRUN MODE =\t\tInsert");
	    	}
        }
        
        // log run information   
	    try {
	    	
	    	if (test) {
	    		logger.write("\tRUN MODE =\t\tTest and Validate");
	    	}
	    	else {
	    		logger.write("\tRUN MODE =\t\tInsert");
	    	}
	    	logger.write("\tINPUT CSV =\t\t" + inputCSVFilePath);

			
  	    } catch (IOException e) {
  	    	e.printStackTrace();
  	    }	    
	    
	   	//*********************************************************************************************
        //* Read input CSV
        //*********************************************************************************************
    	
    	// Create the csv input file
		//File input fileFile = new File(inputCSVFilePath);

		String[][] lines = null; // lines of input csv file
		
		// Read contents of the input file file
		BufferedReader br = null;
		String fileContents = "";
		
		try {
 
			String sCurrentLine;
 
			br = new BufferedReader(new FileReader(inputCSVFilePath));
			int lineCount = 0;
			
			while ((sCurrentLine = br.readLine()) != null) {

				if (lineCount == 0 && sCurrentLine.contains("ocabulary")) { // skip header line if it exists
					if (debug)
						System.out.println("Skipping header ...");
				}
				else {
					
					if (debug) {
						System.out.println("current input file line " + sCurrentLine);
					}

					// build file contents string to give to the parser
					if (lineCount == 0)
						fileContents = sCurrentLine;
					else
						fileContents = fileContents + "\n" + sCurrentLine;
					
				}
				lineCount++;
				
			}
 
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
				System.exit(1);
			}
		}
		
		if (debug) {
			System.out.println("input file contents: " + fileContents);
		}
        
		// Parse the CSV data
		try {
			lines = ExcelCSVParser.parse(fileContents);				
		} 
		catch(Exception e){
			System.err.println(e.getMessage()); 
			e.printStackTrace();
			System.exit(1);
		}
		
		//*********************************************************************************************
        //* Insert tag data into database
        //*********************************************************************************************    	
		if (debug) {
			System.out.println("Count of parsed lines: " + lines.length);
		}
		
		int tCount = 1; //    used to count number of operations
		for (int i = 0; i < lines.length; i++) {
	
			hasErrors = false;
			
			// vocabulary
			String vocabularyStr = lines[i][0].toLowerCase();
			
			// parent term
			String parenttermStr = lines[i][1].toLowerCase();
			
			// term 
			String termStr = "";
			try {
				termStr = lines[i][2].toLowerCase();
			} catch (Exception e) {
				;
			}
			
			// synonyms list
			String synonymsStr = lines[i][3];
			
			if (debug) {
	    		
				System.out.println("-----------");
				System.out.println("vocabularyStr: " + vocabularyStr.trim());
				System.out.println("parenttermStr: " + parenttermStr.trim());
				System.out.println("termStr: " + termStr.trim());
				System.out.println("synonymsStr: " + synonymsStr.trim());
		    	
			}
			
			// parse out synonyms list of strings
			String[] synonyms = null;
			if (synonymsStr.contains(",")) {
				
				synonyms = synonymsStr.split(",");
				if (debug) {
					System.out.println("synonyms: " + Arrays.toString(synonyms));
				}
				
				for (String synonym : synonyms) {
					
					if (debug) {
						System.out.println("\nsynonym: " + synonym.trim());
					}
					
					//*********************************************************************************************
		            //* Create the Tag object
		            //*********************************************************************************************
		        	Tag tag = new Tag();
		        	tag.setVocabulary(vocabularyStr.trim());
		        	tag.setParentTerm(parenttermStr.trim());
		        	tag.setTerm(termStr.trim());
		        	tag.setSynonym(synonym.trim());
		        	tag.setLogger(logger);
		        	
		        	if (debug || verbose) {
		        		System.out.println(++tCount + ":\tVOCABULARY: " + tag.getVocabulary() + "\tPARENT TERM: " + tag.getParentTerm() + "\tTERM: " + tag.getTerm() + "\tSYNONYM: " + tag.getSynonym());
		        	}
		        	
		        	if (!test && tag.commit()) {
		        		
		        		//logger.write("\tID: " + tag.getID() + "\tCONCERN: " + tag.getConcern() + "\tTAG: " + tag.getTag() + "\tKEYWORD: " + tag.getKeyword());
		        		
		        	}
					
				}	
				
			}
			
		}
    	
    }
    
}

