package utils;

// import java.io.FileInputStream;
import java.util.Properties;
import java.io.InputStream;



public class ConfigReader {
	
	    private static Properties prop = new Properties();

	    static {
	        try {
	        	InputStream fis = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties");
	        	//FileInputStream fis =
	            // new FileInputStream("src/main/resources/config.properties");
	            prop.load(fis);
	        } catch (Exception e) {
	            throw new RuntimeException("Config file not found");
	        }
	    }

	    public static String get(String key) {
	        // System property (-Dkey=value from CLI) takes priority over config file.
	        // This lets you switch environments at run time without touching config.properties.
	        String sysProp = System.getProperty(key);
	        return sysProp != null ? sysProp : prop.getProperty(key);
	    }
	}


