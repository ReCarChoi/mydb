package xyz.recarchoi.mydb.backend.tm;

/**
 * 事务管理器
 *
 * @author recarchoi
 * @since 2022/3/7 21:58
 */
public interface TransactionManager {
    /**
     * 开启一个新事务
     *
     * @return 事务的xid
     */
    long begin();

    /**
     * 提交一个事务
     *
     * @param xid 事务的xid
     */
    void commit(long xid);

    /**
     * 取消一个事务
     *
     * @param xid 事务的xid
     */
    void abort(long xid);

    /**
     * 查询一个事务的状态是否是正在进行的状态
     *
     * @param xid 事务的xid
     * @return 是否正在进行
     */
    boolean isActive(long xid);

    /**
     * 查询一个事务的状态是否是已提交的状态
     *
     * @param xid 事务的xid
     * @return 是否已经提交
     */
    boolean isCommitted(long xid);

    /**
     * 查询一个事务的状态是否是已取消
     *
     * @param xid 事务的xid
     * @return 是否已经取消
     */
    boolean isAborted(long xid);

    /**
     * 关闭事务管理器
     */
    void close();
}
