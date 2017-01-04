import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by kentaro.maeda on 2017/01/03.
 */
public class TestData {

    public static void main(String[] args) throws Exception {
        Config conf = ConfigFactory.load("application.conf");

       String url = conf.getString("slick.dbs.default.db.url");
       String driver = conf.getString("slick.dbs.default.db.driver");

       Class.forName(driver);

       Connection con = DriverManager.getConnection(url);

        PreparedStatement photo = con.prepareStatement("INSERT INTO T_PHOTO values(?,?,0,'',false)");
        PreparedStatement image = con.prepareStatement("INSERT INTO T_PHOTO_IMAGE values(?,?,?)");

        for(File f : Arrays.asList(new File("c:/temp").listFiles()).stream()
                .filter(f -> f.getName().endsWith(".jpg")).collect(Collectors.toList())) {

            photo.setString(1,"TEST1");
            photo.setString(2,f.getName());

            photo.executeUpdate();

            image.setString(1,"TEST1");
            image.setString(2,f.getName());
            image.setBinaryStream(3, new FileInputStream(f));
            image.executeUpdate();
        }

        con.close();
    }
}
