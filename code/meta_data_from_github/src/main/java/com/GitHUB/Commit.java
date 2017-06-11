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

public class Commit {
	private int duratinbetweenTowcommitsInDays [] = null;
	
	// size = number of commits -1 
	public Commit (int size){
		duratinbetweenTowcommitsInDays = new int [size];
		// for (int i = 0; i < size; i++) {
		 //   duratinbetweenTowcommitsInDays[i] = -1;
		   // 	}
	}
	
	//===============================================
	public int [] duratinbetweenTowcommitsInDays(){
		return duratinbetweenTowcommitsInDays ;
	}
	
	//===============================================

	public void setdurationarray(int [] array){
		duratinbetweenTowcommitsInDays= array;
	}
	
	//===============================================

	public void setOneInterval(int x, int interval){
		duratinbetweenTowcommitsInDays[x]= interval;
	}
	//===============================================

	public int mean() {
	    int sum = 0;
	    for (int i = 0; i < duratinbetweenTowcommitsInDays.length; i++) {
	     //  if (duratinbetweenTowcommitsInDays[i] != -1){
	    	sum += duratinbetweenTowcommitsInDays[i];
           // System.out.println( "Start test mean elements " + duratinbetweenTowcommitsInDays[i]);

	    	//}
	    }
	    return sum / duratinbetweenTowcommitsInDays.length;
	}
	//===============================================
public int getDuratinbetweenTwocommitsInDays (DateTime FirstCommmit, DateTime secondCommit){
  //  System.out.println( "Start test FirstCommmit" + FirstCommmit);
  //  System.out.println( "Start test secondCommit" + secondCommit);

	long DuratinInDays= 0;
	
	Duration Gduratin = new Duration(FirstCommmit, secondCommit);
	DuratinInDays = Gduratin.getStandardDays();
	String format = ""+DuratinInDays;
	String format2 = format.replace("-", "");
	int DuratinInDaysInt =  Integer.parseInt(format2);
   // System.out.println( "Start test  (int)DuratinInDays" +  DuratinInDaysInt);
	return DuratinInDaysInt;
}
	
	
}
