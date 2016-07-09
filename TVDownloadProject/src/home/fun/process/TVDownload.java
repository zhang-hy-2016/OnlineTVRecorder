package home.fun.process;
import home.fun.mail.GeneralClientPop;



import java.io.*; 
import java.util.Properties;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




import org.apache.log4j.Logger;

/**
 * class to download the OnlineTV from GMAIL
 *  
 * @author zha
 * @version 1.0 (15.04.2011)
 * 
 * @version 1.1 (27.05.2012)
 * - save the topic downloads in separate folder
 * 
 * @version 1.2 (10.10.2013)
 * - updated due to the new mail format from bong.tv
 * 
 * @version 1.3 (10.01.2015)
 * - use GeneralClientPop instead of GmailClient 
 *
 * @version 2.0 (17.04.2014)
 * - extract download link from yourtv.de
 *
 * @version 2.1 (09.06.2016)
 * - enable quality check in mp4 link filtering
 */
public class TVDownload {
	
	private static Logger logger = Logger.getLogger(TVDownload.class.getName());
	private Properties configure;
	
	private Hashtable<String,String> topicTable;  
	private static String TOPIC = "topic_";

	public TVDownload(String configureXML) throws Exception {
		/* load the parameter from configuration file */		
		ClassLoader classLoader  = Thread.currentThread().getContextClassLoader();
		URL confUrl = classLoader.getResource(configureXML);
		configure = new Properties();
		configure.loadFromXML(confUrl.openStream());
		
		/* new in version 1.1,  save the topic table 
		 * A topic table save the download file name match string and folder name  
		 * key - match string 
		 * value - folder name  
		 * 
		 * the mapping is defined in parameter.xml as following, all the key start with "topic_" is topic defintion  
		 * 
		 *  <entry key="topic_FolderName">match string</entry> 
		 * */ 
		topicTable = new Hashtable<String,String>();
		for (Enumeration e = configure.propertyNames(); e.hasMoreElements(); ){
		    String key = (String) e.nextElement();
		    
		    if (key.startsWith(TOPIC)){
		        String value = configure.getProperty(key);  		        
		        topicTable.put(value,key);		        
		        logger.info("save topic folder with m:" + value + ", f:" + key);    		        
		    }    
		        
		}
		
		
	}
	
	public void startProcess() throws Exception{
		List<String> linkList = extractMainLink();
		
		

		
		if (linkList.isEmpty()){
			logger.info("there is no useful link for download job!");
			return;
		}


		String downloadFolderRoot = configure.getProperty("downloadFolder");
        File rootFolder = new File(downloadFolderRoot);
        
		for (Iterator<String> it = linkList.iterator(); it.hasNext();){
		    
		    boolean downloadStarted = false; 
			String downloadLink = it.next();
			//logger.info("get download link " + downloadLink);
			
			// downloadFile(downloadLink);
			
			/* new in version 1.1 , save the defined topic in topic folder , for other in default position  */
			for (Enumeration<String> e = topicTable.keys(); e.hasMoreElements(); ){
			    String topStr =  e.nextElement();  
			    logger.debug("Start to check link topic, t:" + topStr + ", link:" + downloadLink); 
			    
	            if (downloadLink.contains(topStr)){
	                // create the topic folder when it is not existed 
	                String topicF = topicTable.get(topStr); 
	    
	                if (rootFolder.isDirectory()){
	                    File topFolder = new File (rootFolder,topicF); 
	                    if (!topFolder.exists()) {
	                        topFolder.mkdir();  
	                    }
	                    downloadFile(downloadLink,topFolder);
	                    downloadStarted = true;
	                }
	                
	                break;
	                
	            } // end of topic check      
			    
			} // end of topic scan
			
			if (!downloadStarted){
			    downloadFile(downloadLink,rootFolder);    
			}
			
		}// end of download link scan  
				
	}
	
	/**
	 * Download the file over extend OS commad and save in default folder 
	 * @param link     download linke   
	 */
	private void downloadFile(String link){
		String downloadFolder = configure.getProperty("downloadFolder");
		String downloadApp = configure.getProperty("downloadApp");
		
		
		File folder = new File(downloadFolder);
		String cmd = downloadApp + "  " + link;
		try {
			logger.info("Start to download file : " + link); 
			Runtime.getRuntime().exec(cmd, null, folder);	
		} catch (Exception e){
			logger.error("Error occured when execute OS command :" + cmd, e);
		}		 		
	}
    
	
    /**
     * Download the file over extend OS commad  and save in defined folder 
     * 
     * @param link
     * @param download folder 
     */
    private void downloadFile(String link, File downloadFolder){
        String downloadApp = configure.getProperty("downloadApp");        
        String cmd = downloadApp + "  " + link;
        try {
            logger.info("Start to download file : " + link + " in " + downloadFolder.getAbsolutePath()); 
            Runtime.getRuntime().exec(cmd, null, downloadFolder);   
        } catch (Exception e){
            logger.error("Error occured when execute OS command :" + cmd, e);
        }               
    }	
	
	
	/**
	 * Check the new Gmail and return download link list  
	 * @return an empty list for not matched query  
	 * @throws Exception
	 */
	private List<String> extractMainLink() throws Exception{
				
		String mailName = configure.get("mailUserName").toString();
		String mailPass = configure.get("mailPassword").toString();
		String mailFolder = configure.get("mailFolder").toString();
		String mailServer = configure.get("mailHost").toString();
		String mailServerPort = configure.get("mailServerPort").toString();
		
		
		

		logger.info("Start to check the gmail account " + mailName );
		
		
		GeneralClientPop gmail = new GeneralClientPop();
        gmail.setUserPass(mailName, mailPass);
        gmail.setMailServer(mailServer);
        gmail.setMailServerPort(Integer.parseInt(mailServerPort));
        
        gmail.connect();                    
        gmail.openFolder(mailFolder);        
        String[] newMailBodys = gmail.getNewMessages(); 
        gmail.disconnect();

        List<String> linkList = new ArrayList<String>();
        
        if (newMailBodys == null){
        	logger.info("There is no new mail!");
        	return linkList;
        }
        
        logger.info("New mail count = " + newMailBodys.length );
        
        for (int i=0; i<newMailBodys.length; i++){
        	try {
        		String link = getDownloadLink(newMailBodys[i]);
            	if (link != null){
            		linkList.add(link);
            	}	
        	} catch(Exception e){
        		logger.error("Error occured when parse email",e);
        	}
        	
        }
		return linkList; 
	}
	
	/**
	 * get the download link for body text 
	 * @param bodyText
	 * @return null for not matched body
	 */
	private String getDownloadLink(String bodyText){

		return extractHtmlLink(bodyText); 
	}
	
	
	/**
	 * return the first matched mp4 download link <br>
	 * example   http://edge.cdn.youtv.de/3382357/2016-07-08_22-00_Heute-Journal_zdf_hq.mp4 
	 * @param text
	 * @return
	 */
	private String extractHtmlLink(String text){
		
		
		/**
		 * (		#start of group #1
		 * ?i		#  all checking are case insensive
		 * )		#end of group #1
		 * 
		 * <a       #start with "<a"
		 * (		#  start of group #2
		 * [^>]+	#     anything except (">"), at least one character
		 * )		#  end of group #2
		 * 
		 * >		#     follow by ">"
		 * 
		 * (.+?)	#	match anything 
		 * </a>		#	  end with "<div
		 */
		
        // revision in version 1.2	    
	      String HTML_A_TAG_PATTERN = 
            "(?i)<a ([^>]+)>(.+?)</a>";    
		  
		  
		  
		  /**
		   *\s*			#can start with whitespace
		   *(?i)		# all checking are case insensive     
		   * href		#  follow by "href" word
		   * \s*=\s*	#   allows spaces on either side of the equal sign,
		   * (		    #    start of group #1
		   * "([^"]*")  #      allow string with double quotes enclosed - "string"
		   * |	   		#	..or
		   * '[^']*'	#        allow string with single quotes enclosed - 'string'
		   * |          #	  ..or
		   * ([^'"> \\s]+)  #      can't contains one single quotes, double quotes ">", allow with whitespace
		   * )		   	#    end of group #1   	
		   */

		  String HTML_A_HREF_TAG_PATTERN = 
              "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";


		  
		  /**
		   * (?i)		# all checking are case insensive
		   * (			# start of group 1 
		   * http://	# Start with http:// 
		   * ([^\\s]*)	# allow everything except whitespace
		   * .mp4		# end with .mp4
		   * )			# end of group 1
		   */
		  String MP4_PATTERN = 
              "(?i)(http://([^\\s]*).mp4)";

		  
		  
		  
		  Pattern patternTag, patternLink,patternMp4;
		  Matcher matcherTag, matcherLink,matcherMp4;
		  
		  patternTag = Pattern.compile(HTML_A_TAG_PATTERN);
		  patternLink = Pattern.compile(HTML_A_HREF_TAG_PATTERN);
		  patternMp4 = Pattern.compile(MP4_PATTERN);
		  
		  matcherTag = patternTag.matcher(text);
		  
		  
		  
		  while(matcherTag.find()){
			
		      
		      
			  String href = matcherTag.group(1); //href
			  String linkText = matcherTag.group(2); //link text
			  
			  logger.debug("find href : " + href);
			  logger.debug("find linkText : " + linkText);
			  
			  matcherLink = patternLink.matcher(href);
 
			  while(matcherLink.find()){
 
				  String link = matcherLink.group(1); //link
				  
				  logger.debug("find link " + link + " - " + linkText);
				  
				  // find *.mp4 link 
				  
				  matcherMp4 = patternMp4.matcher(link); 
				  while (matcherMp4.find()){
					  String mp4Link = matcherMp4.group(1);
					  
					  logger.info("find mp4 link " + mp4Link);
					  
					  String quality = configure.getProperty("quality");
					  String receiverName = configure.getProperty("receiverName");
					  
					  
					  if (linkText.indexOf(quality) > -1 
							  	&& mp4Link.indexOf(receiverName) > -1){
						  logger.debug("I got link that I want:"+ mp4Link);
						  return mp4Link;
					  }
					  
					  logger.info("The Mp4 link don't contains [" + quality + "] and ["+ receiverName +"]");
					  
					  /* new in version 1.2
					   * remove the quality check 
					   * 
					   * */
					  /*	
                      if ( mp4Link.indexOf(receiverName) > -1){
                          return mp4Link;
                      }*/

				  
				  } // end of mp4 link search 

			  } // end of href search 
			  logger.debug("----------------");
		  } // end of pattern <a> search 
		  
		  
		  return null;
	}
	
	
    /**
     * 
     * @param text
     * @return
     */
    private String extractHtmlLink_1_0(String text){
        
        /**
         * (        #start of group #1
         * ?i       #  all checking are case insensive
         * )        #end of group #1
         * 
         * <a       #start with "<a"
         * (        #  start of group #2
         * [^>]+    #     anything except (">"), at least one character
         * )        #  end of group #2
         * 
         * >        #     follow by ">"
         * 
         * (.+?)    #   match anything 
         * </a>     #     end with "</a>
         */
        
        
        
          String HTML_A_TAG_PATTERN = 
              "(?i)<a([^>]+)>(.+?)</a>";    
          
          
          
          /**
           *\s*         #can start with whitespace
           *(?i)        # all checking are case insensive     
           * href       #  follow by "href" word
           * \s*=\s*    #   allows spaces on either side of the equal sign,
           * (          #    start of group #1
           * "([^"]*")  #      allow string with double quotes enclosed - "string"
           * |          #   ..or
           * '[^']*'    #        allow string with single quotes enclosed - 'string'
           * |          #     ..or
           * ([^'"> \\s]+)  #      can't contains one single quotes, double quotes ">", allow with whitespace
           * )          #    end of group #1    
           */

          String HTML_A_HREF_TAG_PATTERN = 
              "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";


          
          /**
           * (?i)       # all checking are case insensive
           * (          # start of group 1 
           * http://    # Start with http:// 
           * ([^\\s]*)  # allow everything except whitespace
           * .mp4       # end with .mp4
           * )          # end of group 1
           */
          String MP4_PATTERN = 
              "(?i)(http://([^\\s]*).mp4)";

          
          
          
          Pattern patternTag, patternLink,patternMp4;
          Matcher matcherTag, matcherLink,matcherMp4;
          
          patternTag = Pattern.compile(HTML_A_TAG_PATTERN);
          patternLink = Pattern.compile(HTML_A_HREF_TAG_PATTERN);
          patternMp4 = Pattern.compile(MP4_PATTERN);
          
          matcherTag = patternTag.matcher(text);          
          while(matcherTag.find()){
              
              String href = matcherTag.group(1); //href
              String linkText = matcherTag.group(2); //link text
              
              logger.debug("find href : " + href);
              logger.debug("find linkText : " + linkText);
              
              matcherLink = patternLink.matcher(href);
 
              while(matcherLink.find()){
 
                  String link = matcherLink.group(1); //link
                  
                  logger.debug("find link " + link + " - " + linkText);
                  
                  // find *.mp4 link 
                  
                  matcherMp4 = patternMp4.matcher(link); 
                  while (matcherMp4.find()){
                      String mp4Link = matcherMp4.group(1);
                      
                      logger.info("find mp4 link " + mp4Link);
                      
                      String quality = configure.getProperty("quality");
                      String receiverName = configure.getProperty("receiverName");
                      
                      
                      if (linkText.indexOf(quality) > -1 
                                && mp4Link.indexOf(receiverName) > -1){
                          return mp4Link;
                      }

                  
                  } // end of mp4 link search 

              } // end of href search 
              logger.debug("----------------");
          } // end of pattern <a> search 
          
          
          return null;
    }	
	/**
	 * Main method to start the download process , do not add any debug code in this method 
	 * @param arg
	 * @throws Exception
	 */
	public static void main(String[] arg) throws Exception{
		try {
			TVDownload tvDownload  = new TVDownload("Parameter.xml");
			tvDownload.startProcess();
			logger.info("finish!");	
			Runtime.getRuntime().exit(0);
								
		} catch (Exception e ){
			e.printStackTrace();
			logger.error(e);
		}

	}
	
	public static void main2(String[] arg) throws Exception {
		
		TVDownload tvDownload  = new TVDownload("Parameter.xml");
		
		
		File f = new File("/home/zha/workspace/temp/tvmail.txt"); 

		
		byte[] buffer = new byte[(int) f.length()];
		
		BufferedInputStream fif = new BufferedInputStream(
				 new FileInputStream(f));
	    fif.read(buffer);
	    String testStr = new String(buffer);

		
		
	    //System.out.println(tvDownload.extractHtmlLink(testStr));
	    System.out.println("Final MP4 link : " + tvDownload.getDownloadLink(testStr));
	    
	    
	}
	
	
	public static void main3(String[] arg) throws Exception {
		TVDownload tvDownload  = new TVDownload("Parameter.xml");
		String link = "http://c12006-o.l.core.cdn.streamfarm.net/1000109copo/ondemand/usr/800036611/zhang_hongyu_heute-journal_ZDF_16-04-2011_22-45_450.mp4?fdl=1";
		
		tvDownload.downloadFile(link);
		System.out.println("done");

	}
}
