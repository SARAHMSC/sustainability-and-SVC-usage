package com.GitHUB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    static final Logger LOG = LoggerFactory.getLogger(Github.class);

    static String username = "";        
    static String password = "";        

    public static void main(String[] args) {
        try {
            Github github = new Github(username, password, false);     // object from the GithubApi class in the same pakage    
            String qArr[] = new String[]{
           		// " created:\"2009-01-01\""
            		 "created:\"2009-01-01 .. 2009-01-05\"",
                     "created:\"2009-02-06 .. 2009-02-10\"",
                     "created:\"2009-03-11 .. 2009-03-15\"",
                     "created:\"2009-04-16 .. 2009-04-21\"",
                     "created:\"2009-05-22 .. 2009-05-26\"",
                     "created:\"2009-06-27 .. 2009-06-30\"",
                     "created:\"2009-07-01 .. 2009-07-05\"",
                     "created:\"2009-08-06 .. 2009-08-10\"",
                     "created:\"2009-09-11 .. 2009-09-15\"",
                     "created:\"2009-10-16 .. 2009-10-21\"",
                     "created:\"2009-11-22 .. 2009-11-26\"",
                     "created:\"2009-12-27 .. 2009-12-31\""
              };
                        for (String q : qArr) {
                        	
                      	System.out.println("the beginnig of loop");
                github.searchRepos(q, "updated", "desc");
               System.out.println("the end of loop");
            }
            LOG.info("Result is Completed");
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
