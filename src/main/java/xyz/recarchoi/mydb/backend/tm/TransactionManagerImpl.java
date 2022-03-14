package xyz.recarchoi.mydb.backend.tm;

import xyz.recarchoi.mydb.backend.util.Panic;
import xyz.recarchoi.mydb.backend.util.Parser;
import xyz.recarchoi.mydb.common.error.Error;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author recarchoi
 * @since 2022/3/7 22:17
 */
public class TransactionManagerImpl implements TransactionManager {
    /**
     * XID文件头长度
     */
    static final int LEN_XID_HEADER_LENGTH = 8;
    /**
     * 每个事务的占用长度
     */
    private static final int XID_FIELD_SIZE = 1;
    /**
     * 事务的三种状态
     */
    private static final byte FIELD_TRAN_ACTIVE = 0;
    private static final byte FIELD_TRAN_COMMITTED = 1;
    private static final byte FIELD_TRAN_ABORTED = 2;
    /**
     * 超级事务，永远为committed状态
     */
    public static final long SUPER_XID = 0;
    static final String XID_SUFFIX = ".xid";

    private RandomAccessFile file;
    private FileChannel fileChannel;
    private long xidCounter;
    private Lock counterLock;

    public TransactionManagerImpl(RandomAccessFile file, FileChannel fileChannel) {
        this.file = file;
        this.fileChannel = fileChannel;
        counterLock = new ReentrantLock(false);
        checkXidCounter();
    }

    /**
     * 检查XID文件是否合法
     * 读取XID_FILE_HEADER中的xidCounter，根据它计算文件的理论长度，对比实际长度
     */
    private void checkXidCounter() {
        long fileLen = 0;
        try {
            fileLen = file.length();
        } catch (IOException e) {
            Panic.panic(e);
        }
        if (fileLen < LEN_XID_HEADER_LENGTH) {
            Panic.panic(Error.BAD_XID_FILE_EXCEPTION);
        }
        ByteBuffer buffer = ByteBuffer.allocate(LEN_XID_HEADER_LENGTH);
        readDataFromChannel(buffer);
        this.xidCounter = Parser.parseLong(buffer.array());
        long end = getXidPosition(this.xidCounter + 1);
        if (end != fileLen) {
            Panic.panic(Error.BAD_XID_FILE_EXCEPTION);
        }
    }

    /**
     * 根据事务xid取得其在xid文件中对应的位置
     *
     * @param xid 事务的xid
     * @return xid在文件中的位置
     */
    private long getXidPosition(long xid) {
        return LEN_XID_HEADER_LENGTH + (xid - 1) * XID_FIELD_SIZE;
    }

    /**
     * 更新xid事务的状态为status
     *
     * @param xid    需要更改的事务的xid
     * @param status 需要转换成的状态
     */
    private void updateXid(long xid, byte status) {
        long offset = getXidPosition(xid);
        byte[] bytes = new byte[XID_FIELD_SIZE];
        bytes[0] = status;
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        writeDataToChannel(offset, buffer);
    }

    /**
     * 将xid加一，并更新xid header
     */
    private void incrXidCounter() {
        ++xidCounter;
        ByteBuffer buffer = ByteBuffer.wrap(Parser.long2Byte(xidCounter));
        writeDataToChannel(buffer);
    }

    /**
     * 判断当前事务是否处于status状态
     *
     * @param xid    事务的xid
     * @param status 需要判断的状态
     * @return 是否处于该状态
     */
    private boolean checkXid(long xid, byte status) {
        long offset = getXidPosition(xid);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[XID_FIELD_SIZE]);
        readDataFromChannel(offset, buffer);
        return buffer.array()[0] == status;
    }

    private void writeDataToChannel(ByteBuffer buffer) {
        writeDataToChannel(0, buffer);
    }

    /**
     * 向Channel中写入数据
     *
     * @param offset 开始写入的位置
     * @param buffer 包含数据的缓存
     */
    private void writeDataToChannel(long offset, ByteBuffer buffer) {
        try {
            fileChannel.position(offset);
            fileChannel.write(buffer);
        } catch (IOException e) {
            Panic.panic(e);
        }
        try {
            fileChannel.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    private void readDataFromChannel(ByteBuffer buffer) {
        readDataFromChannel(0, buffer);
    }

    /**
     * 从Channel中读取数据到缓冲区
     *
     * @param offset 开始读取的数据
     * @param buffer 读取到的缓冲区
     */
    private void readDataFromChannel(long offset, ByteBuffer buffer) {
        try {
            fileChannel.position(offset);
            fileChannel.read(buffer);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }


    @Override
    public long begin() {
        counterLock.lock();
        try {
            long xid = xidCounter + 1;
            updateXid(xid, FIELD_TRAN_ACTIVE);
            incrXidCounter();
            return xid;
        } finally {
            counterLock.unlock();
        }
    }

    @Override
    public void commit(long xid) {
        updateXid(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public void abort(long xid) {
        updateXid(xid, FIELD_TRAN_ABORTED);
    }

    /**
     * xid是否是SUPER_XID
     *
     * @param xid 事务的xid
     * @return true是 false否
     */
    private boolean isSuperXid(long xid) {
        return xid == SUPER_XID;
    }

    @Override
    public boolean isActive(long xid) {
        return !isSuperXid(xid) && checkXid(xid, FIELD_TRAN_ACTIVE);
    }

    @Override
    public boolean isCommitted(long xid) {
        return !isSuperXid(xid) && checkXid(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public boolean isAborted(long xid) {
        return !isSuperXid(xid) && checkXid(xid, FIELD_TRAN_ABORTED);
    }

    @Override
    public void close() {
        try {
            fileChannel.close();
            file.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }
}
