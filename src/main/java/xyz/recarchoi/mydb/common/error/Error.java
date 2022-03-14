package xyz.recarchoi.mydb.common.error;

/**
 * @author recarchoi
 * @since 2022/3/8 8:52
 */
public class Error {
    /**
     * TransactionManager
     */
    public static final Exception BAD_XID_FILE_EXCEPTION = new RuntimeException("Bad Xid File!");
}
