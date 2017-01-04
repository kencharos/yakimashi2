import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jdk.internal.util.xml.impl.Input;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * insert data to heroku postgres
 */
public class TestData {

    public static void main(String[] args) throws Exception {
        Config conf = ConfigFactory.load("application.conf");

       String url = conf.getString("slick.dbs.default.db.url");
       url = "<heroku run echo $JDBC_DATABASE_URL>";
       String driver = conf.getString("slick.dbs.default.db.driver");

       Class.forName(driver);

       Connection con = DriverManager.getConnection(url);

        con.prepareStatement("delete from T_PHOTO").executeUpdate();
        con.prepareStatement("delete from T_PHOTO_IMAGE").executeUpdate();

        makeAlbum(new File("c:/temp/20160911"), con);

        con.close();
    }

    private static void makeAlbum(File dir, Connection con) {
        Stream.of(dir.listFiles()).parallel()
                .forEach(f ->{
                    InputStream is = resize(f);
                    insertPhoto(is, dir.getName(), f.getName(), con);

                });
    }

    private static InputStream resize(File image) {
        try {

            BufferedImage input = ImageIO.read(image);
            BufferedImage resized = Scalr.resize(input, Scalr.Method.ULTRA_QUALITY,
                    Scalr.Mode.FIT_TO_WIDTH, 800, 700 , Scalr.OP_BRIGHTER);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    private static void insertPhoto(InputStream pImage, String album, String name, Connection con) {
        try {

            PreparedStatement photo = con.prepareStatement("INSERT INTO T_PHOTO values(?,?,0,'',false)");
            PreparedStatement image = con.prepareStatement("INSERT INTO T_PHOTO_IMAGE values(?,?,?)");

            photo.setString(1, album);
            photo.setString(2, name);

            photo.executeUpdate();

            image.setString(1,album);
            image.setString(2, name);
            image.setBinaryStream(3, pImage);
            image.executeUpdate();
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void save(InputStream is, String out) {
        try (FileOutputStream fos = new FileOutputStream(new File(out))) {
            byte[] b = new byte[is.available()];
            is.read(b);
            fos.write(b);

        } catch (IOException e) {}
    }
}
