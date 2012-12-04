import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
//import java.security.KeyStore.Entry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

//import com.example.helloandroid.Enum.Days;

public class ReadToHashmap {
	enum Days{Sunday,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday};
    public static void main(String[] args) throws Exception {
    	
        Map<String, String> map = new HashMap<String, String>();
        Map<Integer, String> map1 = new HashMap<Integer, String>();
        BufferedReader in = new BufferedReader(new FileReader("D:\\workspace\\Pig\\src\\out.txt"));              
		BufferedWriter bw = new BufferedWriter(new FileWriter("D:\\workspace\\Pig\\src\\output.txt"));
		BufferedWriter ou = new BufferedWriter(new FileWriter("D:\\workspace\\Pig\\src\\masterlist.txt"));
        String line = "";
        int i= 1;
        int src=0,dest=0;
        while ((line = in.readLine()) != null) {
        	System.out.println(line);
            String parts[] = line.split("\t");
            if(map1.containsValue(parts[0]))
            {
            	src=getKeyByValue(map1,parts[0]);
            }
            else{
            	map1.put(i, parts[0]);
            	src=i;
            	ou.write(src+"\t"+parts[0]+"\n");
            	i++;
            }
            if(map1.containsValue(parts[1]))
            {
            	dest=getKeyByValue(map1,parts[1]);
            }
            else{
            	map1.put(i, parts[1]);
            	dest=i;
            	ou.write(dest+"\t"+parts[1]+"\n");
            	i++;
            }            
            bw.write(src+"\t"+dest+"\t"+parts[2]+"\n");
            
        }
        System.out.println(map1);
        String s="Sunday";
    	Days a;
    	a=Days.valueOf(s);
		bw.close();
        in.close();
        ou.close();
    }
    
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

}

