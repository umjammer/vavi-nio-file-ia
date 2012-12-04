package json;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;
import org.json.*;

public class JSONExtractor {

	static StringBuffer value= new StringBuffer(
			//"{\"Head\":{\"Title\":\"Atlantic Records Home Page\"}}";
	"{\"Links\":[{\"path\":\"BODY@/background\",\"url\":\"/img/art3.0/stripe_home.gif\"}," +
	"{\"target\":\"_top\",\"path\":\"FORM@/action\",\"method\":\"GET\",\"url\":\"http://home.netscape.com:80/comprod/mirror/\"}]	}");
	public static void json() throws JSONException, MalformedURLException{
		JSONObject myjson = new JSONObject(value.toString());	
        UrlValidator u = new UrlValidator();
        Pattern r = Pattern.compile("(.*://)([^:^/]*)(:\\d*)?(.*)?");	//.*\\//.*?\\/      //".*//(.*?)/"
        Matcher m =null;
		JSONArray nameArray = myjson.getJSONArray("Links");
		for (int i = 0; i < nameArray.length(); i++) {  // **line 2**
		     JSONObject childJSONObject = nameArray.getJSONObject(i);
		     if(childJSONObject.has("url")){
		    	 String temp = childJSONObject.getString("url");
		    	 if(u.isValid(temp)){
		    		 m = r.matcher(temp);
		    		 if(m.find())
		    			 System.out.println(m.group(2)+"\n" );
		    	 }
		     }
		}
	//	JSONArray valArray = myjson.toJSONArray(nameArray);
        
		//System.out.println(valArray.toString());
		/*for(int i=0;i<valArray.length();i++)
        {
    
            String p = nameArray.getString(i) + "," + valArray.getString(i);
            System.out.println(p);
        
        }       
*/
		
	}
	
	public static void main(String[] args) throws JSONException,Exception{
		JSONExtractor.json();
	}
	
	
	
}
