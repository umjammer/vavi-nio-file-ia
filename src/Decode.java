import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class Decode {
	enum Days{Sunday,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday};
    public static void main(String[] args) throws Exception {
    	
        
       // BufferedReader in = new BufferedReader(new FileReader("D:\\workspace\\inst.txt"));
        String line = "add eax,ebx";

       // while ((line = in.readLine()) != null) {
        //	System.out.println(line);
            String tmp[] = line.split(" ");
            //String[] tmp = pdfName.split(".");
            String val1 = tmp[0];
            String tmp1[] = tmp[1].split(",");
            
            String val2 = tmp1[0];
           String val3 = tmp1[1];
            
            System.out.println(val1);
            System.out.println(val2);
            System.out.println(val3);
            System.out.println(tmp1.length);
            
            
        }
    }