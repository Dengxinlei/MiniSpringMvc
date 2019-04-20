import java.io.InputStream;
import java.util.Properties;

public class Test {
    public static void main(String[] args) throws  Exception{
        InputStream in = Test.class.getClassLoader().getResourceAsStream("applicationContext.properties");
        Properties p = new Properties();
        p.load(in);
        System.out.println(p.getProperty("base.package"));
    }
}
