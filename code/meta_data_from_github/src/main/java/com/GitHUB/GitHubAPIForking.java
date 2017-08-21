package com.GitHUBProjectSelection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlListItem;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class GitHubAPIForking {

	//LoggerFactory: The LoggerFactory is a utility class producing Loggers for various logging APIs, most notably for log4j, logback and JDK 1.4 logging. 
	//Other implementations such as NOPLogger and SimpleLogger are also supported.
    static final Logger LOG = LoggerFactory.getLogger(GitHubAPIForking.class);

    private static final String host = "https://api.github.com";
    private static final String searchPath = "/search/repositories";

    private String username, password;
    private WebClient webClient;

    private int minSleep = 1, maxSleep = 5; 
    
    //ObjectMapper: ObjectMapper is the main actor class of Jackson library. ObjectMapper class provides functionalities to
    //convert Java objects to matching JSON constructs and vice versa. 
    //It uses instances of JsonParser and JsonGenerator for implementing actual reading/writing of JSON.
    private ObjectMapper mapper = new ObjectMapper();
    private List<Map> results;
    private List<Map> filteredResults;
    private  int totalCommits =0; 
    public static Fork [] Forkslist;
    public static int  index;
   // public static int  logestdepth;
    
    public GitHubAPIForking(String username, String password, boolean proxy) {
        this.username = username;
        this.password = password;
        

       //WebClient: this class simulates a web browser.
        webClient = new WebClient(BrowserVersion.FIREFOX_45);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setTimeout(120 * 1000);
        if (proxy) {
            webClient.getOptions().setProxyConfig(new ProxyConfig("127.0.0.1", 8888));      //Fiddler / Charles default proxy settings
        }
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            String base64encodedUsernameAndPassword = base64Encode(username + ":" + password);
            webClient.addRequestHeader("Authorization", "Basic " + base64encodedUsernameAndPassword);
        }
    }

    private static String base64Encode(String stringToEncode) {
        return DatatypeConverter.printBase64Binary(stringToEncode.getBytes());
    }
    
  //----------------------------------searchRepos Method  ----------------------------------------------------
    //(q, "updated", "desc")
   //Map<K,V>
  // An object that maps keys to values. A map cannot contain duplicate keys; each key can map to at most one value.

    public List<Map<String, Object>> searchRepos(String q, String sort, String order) throws Exception {
        results = new ArrayList<>();
        filteredResults = new ArrayList<>();

        
        // WebRequest : A request sent to a web server.
        //Instantiates a WebRequest for the specified URL.
        WebRequest webRequest = new WebRequest(new URL(host + searchPath), HttpMethod.GET);
        //NameValuePair: A name / value pair parameter used as an element of HTTP messages.
        ////Define name-value pairs to set into the QueryString
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        //Sets the request parameters to use.
        webRequest.setRequestParameters(nameValuePairs);
        nameValuePairs.add(new NameValuePair("q", q));
        //sort on these properties: 1 Number of stars, 2 Number of forks, 3 Date of last update
        nameValuePairs.add(new NameValuePair("sort", sort));
        // nameValuePairs.add(new NameValuePair("sort", "stars"));
        //nameValuePairs.add(new NameValuePair("stars", "10..20"));
        //Order: desc; sorting by stars in descending order, so that the most popular repositories appear first in the search results.
        nameValuePairs.add(new NameValuePair("order", order));
        nameValuePairs.add(new NameValuePair("per_page", "100"));
      
        //You can choose to include forked repositories by adding fork:true to your search
        //nameValuePairs.add(new NameValuePair("fork", true ));
        int currentPage = 1;
        LOG.info("Opening page " + currentPage + "  " + q);
        //A response to a web request from a web server.
        WebResponse webResponse = webClient.loadWebResponse(webRequest);
         //System.out.println(webResponse);//com.gargoylesoftware.htmlunit.WebResponse@11dee337
        
        do {
            String response = webResponse.getContentAsString();
         // System.out.println(response); all repos the content in the page 
            //parsing convert the data to structure 
            //JsonNode : Method to deserialize JSON content as tree expressed using set of JsonNode instances.
            JsonNode root = mapper.readTree(response);
          //System.out.println(root); array of items" which have content of reprstories 
            for (JsonNode repo : root.get("items")) {
            	//System.out.println(repo); one repo in an array// loop on the represtores on the response one by one
                Thread.sleep(randomSleepSeconds(minSleep, maxSleep));      // call randomSleepSeconds Method in the same class 
                processRepo(repo);// call  processRepo method in the same class 
            }
            String nextPageUrl = nextPageUrl(webResponse);
            if (nextPageUrl == null) {
                break;
            } else {
//                break;        
                LOG.info("Opening page " + ++currentPage + " " + nextPageUrl);
                webResponse = webClient.loadWebResponse(new WebRequest(new URL(nextPageUrl)));
            }
        } while (true);

        

       
        LOG.info("Writing filtered results to CSV : " + filteredResults.size());
        writeToCSV(filteredResults, "results.csv");// call method in the same class 

        return null;
    }
  //----------------------------------ProcessRepo Method  ----------------------------------------------------
    public void processRepo(JsonNode repo) throws Exception {
        String commitsUrl = repo.get("commits_url").asText().replace("{/sha}", "");
       // System.out.println(" Print commitsUrl:"+ commitsUrl);// test 
        //trim(): A String, representing the string with removed whitespace from both ends
        String repoHtmlurl = repo.get("html_url").asText().trim();
       // String contributorsHtmlurl = repo.get("contributors_url").asText().trim();
        String forksHtmlurl = repo.get("forks_url").asText().trim();
     //   String PullHtmlurl = repo.get("pulls_url").asText().replace("{/number}", "");
      //  String branchesHtmlurl = repo.get("branches_url").asText().replace("{/branch}", "");
        String pushed_at = repo.get("pushed_at").asText();
        String all_forksNum = repo.get("forks_count").asText();
        Fork ForkObject = null;  
        int depthnumber =0;
        //System.out.println(" Print pushed_at:"+ pushed_at);// test 
       // System.out.println(" Print repoHtmlurl:"+ repoHtmlurl);// test 
        LOG.info("\nRepo : " + repoHtmlurl);
        
        
// call getFirstCommit method in the same class 
        String first_commit = getFirstCommit(repoHtmlurl, commitsUrl); // call getFirstCommit in the same class 
       // System.out.println("first_commit:" +first_commit); //first_commit:2009-01-25T15:38:46Z
        if (first_commit == null) {
            LOG.info("Repository is blank (0 commits ) : " + repoHtmlurl);
            return;
        }
        ((ObjectNode) repo).put("first_commit", first_commit);
        LOG.info("First Commit : " + first_commit);

        webClient.close();
        
 // call getLastCommit method in the same class 
        String last_commit = getLastCommit(commitsUrl);
        ((ObjectNode) repo).put("last_commit", last_commit);
        LOG.info("Last Commit : " + last_commit);

        DateTime created_atDT = new DateTime(repo.get("created_at").asText());
        DateTime last_commitDT = new DateTime(last_commit);
        Duration duration = new Duration(created_atDT, last_commitDT);
        ((ObjectNode) repo).put("SM", duration.getStandardMinutes());
        LOG.info("Created - LastCommit : " + duration.getStandardMinutes());

        DateTime first_commitDT = new DateTime(first_commit);
        duration = new Duration(first_commitDT, last_commitDT);
        ((ObjectNode) repo).put("SS", duration.getStandardMinutes());
        LOG.info("FirstCommit - LastCommit (Mins): " + duration.getStandardMinutes());
        // In Days
        ((ObjectNode) repo).put("SSDays", duration.getStandardDays());
        LOG.info("FirstCommit - LastCommit (Days): " + duration.getStandardDays());
//https://www.mkyong.com/java/java-convert-object-to-map-example/
        //example output
        //{name=mkyong, age=34, skills=[java, node]}

        
        ((ObjectNode) repo).put("totalCommits", totalCommits);
       // LOG.info("totalCommitsTocheck: " + totalCommits);
        
       
    
       //=====================================processCommites call===========================
       
       
     //  Commit commit = processCommites(repoHtmlurl, commitsUrl);
    // int meanDaysForEachRepo = commit.mean(); 
    // ((ObjectNode) repo).put("IntervalBtweenCommits", meanDaysForEachRepo);
    // LOG.info("IntervalBtweenCommits: " + meanDaysForEachRepo);
       
     
     
     //=====================================processForks call===========================
     
     
     Fork[] allforkslist = processF1Forks(repoHtmlurl, forksHtmlurl, all_forksNum, last_commitDT);
   //int meanDaysForEachRepo = commit.mean(); 
   //((ObjectNode) repo).put("IntervalBtweenCommits", meanDaysForEachRepo);
  // LOG.info("IntervalBtweenCommits: " + meanDaysForEachRepo);
     
//    Fork longestRepo =  processSubForks (forkF1);
       //====================================compareforks call============================
       
      int allforksnum =  Integer.parseInt(all_forksNum);
     if (allforksnum > 0)
     {
    	  ForkObject = compareforks(); 
     }
      //===========================
      
      DateTime lastcommitF= last_commitDT; 
      String forkLevelF= "F0";
      boolean islongerF= false;
      boolean isforkF= false; 
      //============================
      if (ForkObject != null){
      // the longest commit
        lastcommitF= ForkObject.getlastcommit(); 
        forkLevelF= ForkObject.getforkLevel();
        islongerF= ForkObject.getislonger();
         isforkF= ForkObject.getisfork(); 
      }
    
    	     ((ObjectNode) repo).put("lastcommitInTheRepo", lastcommitF+"");
    	     LOG.info("lastcommitInTheRepo: " + lastcommitF);
    	     
    	     ((ObjectNode) repo).put("ForkLevel", forkLevelF);
    	     LOG.info("ForkLevel: " + forkLevelF);
    	     
    	     ((ObjectNode) repo).put("islongerThanDefaultBranch", islongerF);
    	     LOG.info("islongerThanDefaultBranch: " + islongerF);
    	      
    	     ((ObjectNode) repo).put("isfork", isforkF);
    	     LOG.info("isfork: " + isforkF);
    	     
    	     
    	     if (allforksnum > 0){
    	     depthnumber = depths();}
    	     else {depthnumber = 0;}
    	     ((ObjectNode) repo).put("Depth Of Forks", depthnumber);
    	     LOG.info("Depth Of Forks: " + depthnumber);
    	
      
       //===============================write to CSV call ======================================

        results.add(mapper.convertValue(repo, Map.class)); // add all the reprstorey 

       
        if (first_commitDT.getYear() > 2008 && first_commitDT.getYear() < 2010) {
            filteredResults.add(mapper.convertValue(repo, Map.class));
        }
    }
    
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //----------------------------------processF1Forks Method ----------------------------------------------------
   private Fork [] processF1Forks(String repoUrl, String forksUrl, String TotalRepoForks, DateTime latcommitInDefualt) throws Exception {
   
	   System.out.println("Total number of forks i want to compare to:"+TotalRepoForks);
	   int TotalRepoForksnum =  Integer.parseInt(TotalRepoForks);
   if (TotalRepoForksnum == 0) {
       return null;
   }
  int size = (TotalRepoForksnum*10);
  if (size> 2147483647){
	  size = 2147483647;
  }
	  
   Forkslist = new Fork[size]; 
     int ForkNum = 0;
	 int  traceforkcount = 0; 
     int numOfPages  = TotalRepoForksnum/30;
     index = 0;
     
     
   //============set the first element as the defualt branch
     Forkslist[index] = new Fork(); 
     Forkslist[index].setforkLevel("F0");
     Forkslist[index].setisfork(false);
     Forkslist[index].setlastcommit(latcommitInDefualt);
     Forkslist[index].setchildForksNo(TotalRepoForksnum);
     Forkslist[index].setforksurl(forksUrl);
     index++;
     //============loop============
    do{
    	
    	
    	WebRequest webRequestNext = new WebRequest(new URL(forksUrl));
    //  System.out.println(forksUrl);
         List<NameValuePair> params = new ArrayList<>();// empty 
         params.add(new NameValuePair("page", numOfPages + ""));
         webRequestNext.setRequestParameters(params);
         WebResponse webResponseNext = webClient.getPage(webRequestNext).getWebResponse();
         String responseNext = webResponseNext.getContentAsString();
         
    try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseNext);
        //System.out.println( "Start test size array " + root.size());
        if (root != null){
        	//-1 for i+1
        for ( int i = 0; i < root.size(); i++ ) {
           //System.out.println( "i" + i + "page"+numOfPages);
        	 ForkNum = Integer.parseInt(root.get(i).get("forks_count").asText());
           //  System.out.println( ForkNum);

        	//traceforkcount=  traceforkcount +ForkNum; 
             Forkslist [index]= new Fork(); 
         	
         	String Isfork= root.get(i).get("fork").asText();
         	Forkslist [index].setisfork(Boolean.parseBoolean(Isfork));
         	
             String pushed_atfork= root.get(i).get("pushed_at").asText();
             DateTime lastcommitonfork = new DateTime(pushed_atfork);
         	Forkslist [index].setlastcommit(lastcommitonfork);

         	//set level
         	Forkslist [index].setforkLevel("F1");

         	//set child forks 
          	String NumforkPF=  root.get(i).get("forks_count").asText();
          	Forkslist [index].setchildForksNo(Integer.parseInt(NumforkPF));
          	
          	//set  forks url
          	String UrlforkPF= root.get(i).get("forks_url").asText().trim();
          	Forkslist [index].setforksurl(UrlforkPF);
 		 
			//   System.out.println( index+"---------"+Forkslist[i].getforksurl()+"---------"+Forkslist[i].childForksNo()+"---------"+Forkslist[i].getforkLevel());

          	index++;
        	    }//end for loop
        numOfPages--; 
      // System.out.println( "Start test page " + numOfPages);
        }
    } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
    }
    } while (numOfPages >-1 );
    
//if //(traceforkcount == TotalRepoForksnum)
//	System.out.println( "Number of "+"Index:"+index +"Orginal:"+TotalRepoForksnum );
//else 
	//System.out.println( "Not equal"+"Trace:"+index +"Orginal:"+TotalRepoForksnum );
	  /* for ( int i = 0; i < Forkslist.length; i++ ) {
		   if (Forkslist[i] == null) {
               continue;}
		   System.out.println( i+"---------"+Forkslist[i].getforksurl()+"---------"+Forkslist[i].childForksNo());
		   
	   }*/
	   Forkslist =  processSubForks();
   return Forkslist;
    
    
    }
    
    
   
   ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   //----------------------------------processSubForks Method ----------------------------------------------------
   
   private Fork[] processSubForks() throws Exception {
	   
	  // Fork [] SubForkslist = Forkslist;
	   int numOfPages2 = 0;
	   int FRForkNum = 0;
	   String parentforklevel = null;
	   int numberlevel= -1; 
	   int currentcountforks = -1;
	   // if F1 forks > 0 check sub sequence forks of F1 !!
	   if (Forkslist[0].childForksNo()>0){
		   
	   
	   for ( int j = 1; j < Forkslist.length; j++ ) {
		//   System.out.println( "Forkslist.length" + Forkslist.length);
		   if (Forkslist[j] == null) {
               continue;}
		   
		   if (Forkslist[j].gettested() == true) {
               continue;}
		    currentcountforks= Forkslist[j].childForksNo();
		   if (currentcountforks >0){
			   parentforklevel= Forkslist[j].getforkLevel().substring(1); 
			   numberlevel = Integer.parseInt(parentforklevel);
			   numOfPages2  = currentcountforks/30;

			 //-------------------------------------------------------
			 String currentforkUrl  =  Forkslist[j].getforksurl();
			 
			 do{
			    	

			    	WebRequest webRequestNext = new WebRequest(new URL(currentforkUrl));
			    //  System.out.println(forksUrl);
			         List<NameValuePair> params = new ArrayList<>();// empty 
			         params.add(new NameValuePair("page", numOfPages2 + ""));
			         webRequestNext.setRequestParameters(params);
			         WebResponse webResponseNext = webClient.getPage(webRequestNext).getWebResponse();
			         String responseNext = webResponseNext.getContentAsString();
			         
			    try {
			        ObjectMapper mapper2 = new ObjectMapper();
			        JsonNode root2 = mapper2.readTree(responseNext);
			        //System.out.println( "Start test size array " + root.size());
			        if (root2 != null){
			        for ( int x = 0; x < root2.size(); x++ ) {
			         //  System.out.println( "i" + i + "page"+numOfPages);
			           
			             Forkslist [index]= new Fork(); 
			         	
			           //set child forks 
			        	 FRForkNum = Integer.parseInt(root2.get(x).get("forks_count").asText());
				          	Forkslist [index].setchildForksNo(FRForkNum);
				          	
				          	
			         	String Isfork= root2.get(x).get("fork").asText();
			         	Forkslist [index].setisfork(Boolean.parseBoolean(Isfork));
			         	
			             String pushed_atfork= root2.get(x).get("pushed_at").asText();
			             DateTime lastcommitonfork = new DateTime(pushed_atfork);
			         	Forkslist [index].setlastcommit(lastcommitonfork);

			         	//set level// current level +1 
			         	Forkslist [index].setforkLevel("F"+(numberlevel+1));

			         	//set child forks 
			          //	String NumforkFR=  root2.get(x).get("forks_count").asText();
			        //  	Forkslist [index].setchildForksNo(Integer.parseInt(NumforkFR));
			          	
			          	//set  forks url
			          	String UrlforkFR= root2.get(x).get("forks_url").asText().trim();
			          	Forkslist [index].setforksurl(UrlforkFR);
						//   System.out.println( index+"---------"+Forkslist[index].getforksurl()+"---------"+Forkslist[index].childForksNo()+"---------"+Forkslist[index].getforkLevel());

			          	index++;
			        	    }//end for loop
			        numOfPages2--; 
			      // System.out.println( "Start test page " + numOfPages);
			        }
			    } catch (Exception ex) {
			        LOG.error(ex.getMessage(), ex);
			    }
			    } while (numOfPages2 >-1 );
			 Forkslist[j].settested(true);
		   }//end in if 
		   //----------------------------------------------------------------
	   }//end for loop 
	   }//end out if 
		System.out.println( "Number of "+"Index:"+index );

   return Forkslist;
    
    
    }
   
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   //----------------------------------compareforks Method ----------------------------------------------------
   private Fork  compareforks() throws Exception {
   	
	       
	            
	        int   longest = 0; 
	        int whichelement = 0;
	        DateTime   longestcommit=  Forkslist[0].getlastcommit();
	        try {
	        
	        for ( int x = 0; x < index+1; x++ ){
	        	if (Forkslist[x] == null) {
	        		///System.out.println( x+"---------null");
	                continue;}
				//  System.out.println( x+"---------"+Forkslist[x].getforksurl()+"---------"+Forkslist[x].childForksNo()+"---------"+Forkslist[x].getforkLevel());
				 
				  //if (Forkslist[x].childForksNo()>0 && Forkslist[x].gettested() == false) {
	        		// System.out.println("-----This fork has not been tested yet----");}
	        	DateTime testcommit = Forkslist[x].getlastcommit();
             longest = longestcommit.compareTo(testcommit);
       	if (longest < 0  ){
       		longestcommit =testcommit;
       		whichelement = x;
       	}
	        }//end for loop
	        } catch (Exception ex) {
	            LOG.error(ex.getMessage(), ex);
	        }
  return Forkslist[whichelement];
  }
   
   
 //----------------------------------depths Method ----------------------------------------------------
   private int  depths() throws Exception {
   	
	       
	            
	        int depthlevel,logestdepth = 0; 
	        try {
	        	if ( Forkslist[0].childForksNo() == 0){
	                return 0;}
	        for ( int x = 0; x < index+1; x++ ){
	        	if (Forkslist[x] == null) {
	                continue;}
	        	
	  		  
				  depthlevel= Integer.parseInt(Forkslist[x].getforkLevel().substring(1)); 
				 if (logestdepth <depthlevel) 
				 {logestdepth =depthlevel;}
				  //if (Forkslist[x].childForksNo()>0 && Forkslist[x].gettested() == false) {
	        		// System.out.println("-----This fork has not been tested yet----");}
	   
	        }//end for loop
	        } catch (Exception ex) {
	            LOG.error(ex.getMessage(), ex);
	        }
	  //  System.out.println( logestdepth+"-depth");
  return logestdepth;
  }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //----------------------------------processCommites Method ----------------------------------------------------
   private Commit processCommites(String repoUrl, String commitsUrl) throws Exception {
    //private void processCommites(String repoUrl, String commitsUrl) throws Exception {
    WebRequest webRequest = new WebRequest(new URL(repoUrl), HttpMethod.GET);
    HtmlPage page = webClient.getPage(webRequest);    //HtmlListItem: The HTMLListItem class represents items within a HTMLList. The items within the list can either be ordered or unordered.
    HtmlListItem listItem = page.querySelector(".numbers-summary .commits");// Returns the first element within the document that matches the specified group of selectors.
    
   if (listItem == null) {
       return null;
   }
    
    //System.out.println("listItem:"+listItem.asText()); output: istItem:5,830 commits
   // WebRequest webRequestCommit = new WebRequest(new URL(commitsUrl));
   // webRequestCommit.setRequestParameters(params);
   // WebResponse webResponseCommit = webClient.getPage(webRequestCommit).getWebResponse();
   // String responseCommit = webResponseCommit.getContentAsString();
    totalCommits = Integer.parseInt(listItem.asText().replace(",", "").replace("commits", "").replace("commit", "").trim());
    Commit commit = new Commit(totalCommits);
    // System.out.println( "totalCommits " + totalCommits);
    if (totalCommits == 0) {
        return null;
    }
     
    int index=0;
    int numOfPages  = totalCommits/30 ;
    do{
    	
    	
    	WebRequest webRequestNext = new WebRequest(new URL(commitsUrl));
       // System.out.println(commitsUrl);
         List<NameValuePair> params = new ArrayList<>();// empty 
         params.add(new NameValuePair("page", numOfPages + ""));
         webRequestNext.setRequestParameters(params);
         WebResponse webResponseNext = webClient.getPage(webRequestNext).getWebResponse();
         String responseNext = webResponseNext.getContentAsString();
         
    try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseNext);
        //System.out.println( "Start test size array " + root.size());
        if (root != null){
        	//-1 for i+1
        for ( int i = 0; i < root.size()-1; i++ ) {
          //  System.out.println( "Start test size " + root.size());

        	String Fcommit= root.get(i).get("commit").get("author").get("date").asText();
            DateTime firstcommit = new DateTime(Fcommit);
            
            String Scommit= root.get(i+1).get("commit").get("author").get("date").asText();
            DateTime secondcommit = new DateTime(Scommit);
            int interval= commit.getDuratinbetweenTwocommitsInDays(firstcommit, secondcommit); 
            //System.out.println( "Start test index " + index);
            if (index >= totalCommits ){
            	break;}
            commit.setOneInterval(index, interval);
            index++; 
        	    }
        numOfPages--; 
      // System.out.println( "Start test page " + numOfPages);
        }
    } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
    }
    if (index >= totalCommits ){
    	break;}
    } while (numOfPages >-1 );
    

   return commit;
    
    
    }
  

    
 //----------------------------------writeToCSV Method  ----------------------------------------------------
    private void writeToCSV(List<Map> lstMap, String fileName) throws Exception {
        ICsvMapWriter mapWriter = null;
        try {
            File file = new File(fileName);
            boolean fileExists = file.exists();

            mapWriter = new CsvMapWriter(new FileWriter(file, true),
                    CsvPreference.STANDARD_PREFERENCE);

           
            for (Map m : lstMap) {
                LOG.info("Writing Repo To CSV : " + ToStringBuilder.reflectionToString(m, ToStringStyle.JSON_STYLE));
                Map ownerMap = (Map) m.get("owner");
                Set<Map.Entry> entrySet = ownerMap.entrySet();
                for (Map.Entry entry : entrySet) {
                    m.put("owner." + entry.getKey(), entry.getValue());
                }
                m.remove("owner");
            }

            String[] headers = (String[]) lstMap.get(0).keySet().toArray(new String[]{});
            
            if (!fileExists) {
                mapWriter.writeHeader(headers);
            }
            for (Map m : lstMap) {
                mapWriter.write(m, headers);
            }
        } finally {
            if (mapWriter != null) {
                mapWriter.close();
            }
        }
    }

 //----------------------------------getLastCommit Method  ----------------------------------------------------

    private String getLastCommit(String url) throws Exception {
        return getCommitPage(url, 1, false);
    }


    /**
     * https://developer.github.com/v3/repos/commits/
     *
     * @param commitsUrl
     * @param pageNo
     * @return
     * @throws Exception
     */
    
  //----------------------------------getCommitPage Method ----------------------------------------------------
    private String getCommitPage(String commitsUrl, int pageNo, boolean lastRecord) throws Exception {
        WebRequest webRequest = new WebRequest(new URL(commitsUrl));
       //System.out.println(commitsUrl);
        List<NameValuePair> params = new ArrayList<>();// empty 
        params.add(new NameValuePair("page", pageNo + ""));
        webRequest.setRequestParameters(params);
        WebResponse webResponse = webClient.getPage(webRequest).getWebResponse();
        String response = webResponse.getContentAsString();
        //System.out.println( "Start test ");
        //System.out.println( response ); // that the page I want *********** For example first commit: this is the last page of commits
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
          
            if (lastRecord) {            	
                return root.get(root.size() - 1).get("commit").get("author").get("date").asText();
            } else {
                return root.get(0).get("commit").get("author").get("date").asText();
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

  //----------------------------------getFirstCommit Method ----------------------------------------------------
    private String getFirstCommit(String repoUrl, String commitsUrl) throws Exception {
        WebRequest webRequest = new WebRequest(new URL(repoUrl), HttpMethod.GET);
     //   System.out.println("heretest:"+repoUrl); 
        HtmlPage page = webClient.getPage(webRequest);
        //HtmlListItem: The HTMLListItem class represents items within a HTMLList. The items within the list can either be ordered or unordered.
        HtmlListItem listItem = page.querySelector(".numbers-summary .commits");// Returns the first element within the document that matches the specified group of selectors.
        //System.out.println("listItem:"+listItem.asText()); output: istItem:5,830 commits
        
    
        if (listItem == null) {
            return null;
        }
        totalCommits = Integer.parseInt(listItem.asText().replace(",", "").replace("commits", "").replace("commit", "").trim());
       //Requests that return multiple items will be paginated to 30 items by default.
        //You can specify further pages with the ?page parameter. For some resources, 
        //you can also set a custom page size up to 100 with the ?per_page parameter. 
        //Note that for technical reasons not all endpoints respect the ?per_page parameter
        int per_page_commits = 30; // or 100
        int last_page = totalCommits / per_page_commits;
        if (totalCommits % per_page_commits > 0) {
            last_page++;
        }
        LOG.info("Total Commits : " + totalCommits); // apear in the output 
        LOG.info("Last Page of commits : " + last_page);    // apear in the output    
        //commits are returned in reverse chronological order, that means the last page of commits has the first commit.
        return getCommitPage(commitsUrl, last_page, true);// call getCommitPage Method in the same class 
    }

    /**
     * https://developer.github.com/guides/traversing-with-pagination/
     *
     * @param webResponse
     * @return
     */
  //----------------------------------nextPageUrl Method ----------------------------------------------------
    private String nextPageUrl(WebResponse webResponse) {
        String hValue = webResponse.getResponseHeaderValue("Link");
        //A compiled representation of a regular expression.
       // A regular expression, specified as a string, must first be compiled into an instance of this class.
       // The resulting pattern can then be used to create a Matcher object that can match arbitrary character sequences against the regular expression. All of the state involved in performing a match resides in the matcher, so many matchers can share the same pattern.


        Pattern pattern = Pattern.compile("<(.*?)>; rel=\"next\"");
        if (hValue == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(hValue);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
//----------------------------------RandomSleepSeconds Method ----------------------------------------------------
    public static int randomSleepSeconds(int minSleep, int maxSleep) {
        Random rn = new Random();
        int result = rn.nextInt(maxSleep - minSleep + 1) + minSleep;
        LOG.info("Sleeping for " + result + " seconds");
        return result * 1000;       //milliseconds
    }
}
