package xyz.recarchoi.mydb.backend.util;

/**
 * @author recarchoi
 * @since 2022/3/8 8:58
 */
public class Panic {
    public static void panic(Exception exception){
        exception.printStackTrace();
        System.exit(1);
    }
}
