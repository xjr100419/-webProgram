package testASocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import testBSocket.SocketClientRequestThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class SocketClientDaemon {
    //日志
    private static final Log LOGGER = LogFactory.getLog(SocketClientDaemon.class);

    public static void main(String[] args) throws Exception {

        SocketClientDaemon socketClientDaemon = new SocketClientDaemon();
        socketClientDaemon.send();

        //这个wait不涉及到具体的实验逻辑，只是保持线程
        synchronized (SocketClientDaemon.class){
            SocketClientDaemon.class.wait();
        }
    }

    public void send(){
        OutputStream clientRequest = null;
        InputStream clientResponse = null;
        try {
            Socket socket = new Socket("localhost",83);
            Thread.sleep(1000L);
            clientRequest = socket.getOutputStream();
            clientResponse = socket.getInputStream();


            //发送请求信息
            clientRequest.write(("这是个客户端的请求over").getBytes());
            clientRequest.flush();

            //这里等待，直到服务器返回信息
            LOGGER.info("第个客户端的请求发送完成，等待服务器返回信息over");
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
