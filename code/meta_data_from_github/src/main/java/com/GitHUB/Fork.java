package com.GitHUB;

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


public class Fork {

	private DateTime lastcommit;
	private boolean isfork; 
	private String forkLevel; 
	private boolean islonger; 

	public Fork (){
		lastcommit = null;
		isfork= false;
		forkLevel= null;
		islonger= false;// from the upper level i.e: is SF longer than PF
	}
	
	public DateTime getlastcommit(){
		return lastcommit;
	}
	
	public String getforkLevel(){
		return forkLevel;
	}
	
	public boolean getisfork(){
		return isfork;
	}
	
	public boolean getislonger(){
		return isfork;
	}
	
	public void setforkLevel(String SforkLevel){
		forkLevel = SforkLevel;
	}
	
	public void setisfork(boolean fork){
		isfork = fork;
	}
	
	public void setislonger(boolean longer){
		islonger = longer;
	}
	public void setlastcommit(DateTime lastcommitdate){
		 lastcommit= lastcommitdate;
	}
	
}
