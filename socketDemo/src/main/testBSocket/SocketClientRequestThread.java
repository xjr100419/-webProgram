package testBSocket;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/**
 * 一个SocketClientRequestThread线程模拟一个客户端请求
 */
public class SocketClientRequestThread implements Runnable{
    static {
        BasicConfigurator.configure();
    }

    //日志
    private static final Log LOGGER = LogFactory.getLog(SocketClientRequestThread.class);
    private CountDownLatch countDownLatch;

    //这个线程层的编号
    private Integer clientIndex;

    public SocketClientRequestThread(CountDownLatch countDownLatch, Integer clientIndex) {
        this.countDownLatch = countDownLatch;
        this.clientIndex = clientIndex;
    }

    public void run() {
        Socket socket = null;
        OutputStream clientRequest = null;
        InputStream clientResponse = null;

        try {
            socket = new Socket("localhost",83);
            clientRequest = socket.getOutputStream();
            clientResponse = socket.getInputStream();

            //等待，直到socketClientDaemon 完成所有线程启动，然后所有线程一起发送请求
            this.countDownLatch.await();

            //发送请求信息
            clientRequest.write(("这是第"+this.clientIndex+"个客户端的请求").getBytes());
            clientRequest.flush();

            //这里等待，直到服务器返回信息
            LOGGER.info("第"+this.clientIndex+"个客户端的请求发送完成，等待服务器返回信息over");
            int maxLen = 1024;
            byte[] contextBytes = new byte[maxLen];
            int realLen;
            String message = "";
            //程序执行到这里，会一直等待服务器返回信息（注意，前提是int和out都不能close,如果close了就收不到服务器的反馈了）
            while ((realLen = clientResponse.read(contextBytes,0,maxLen))!=-1){
                message+= new String(contextBytes,0,realLen);
            }
            LOGGER.info("接收到来自服务器的信息："+message);


        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage(),e);
        }finally {

                try {
                    if (clientRequest!=null){
                    clientRequest.close();
                    }
                    if (clientResponse!=null){
                        clientResponse.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    LOGGER.error(e.getMessage(),e);
                }

        }
    }
}
