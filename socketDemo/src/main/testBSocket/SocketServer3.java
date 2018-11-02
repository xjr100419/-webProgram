package testBSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class SocketServer3 {
    static {
        BasicConfigurator.configure();
    }
    private static Object xWait = new Object();
    /**
     * 日志
     */
    private static final Log LOGGER = LogFactory.getLog(SocketServer3.class);

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket =null;
        try {
            serverSocket = new ServerSocket(83);
            serverSocket.setSoTimeout(1000);

            while (true){
                Socket socket =null;
                try {
                    socket = serverSocket.accept();
                }catch (SocketTimeoutException e1){
                    synchronized (SocketServer3.xWait){
                        LOGGER.info("这次没有从底层接收到任何TCP连接，等待10毫秒，模拟事件X的处理时间");
                        xWait.wait(1000);
                    }
                    continue;
                }

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                Integer sourcePort = socket.getPort();
                int maxLen = 2048;
                byte[] contextBytes = new byte[maxLen];
                int realLen;
                StringBuffer message = new StringBuffer();
                //下面收取信息（设置成非阻塞方式，这样read信息的时候，又可以做一些其他事情）
                socket.setSoTimeout(10);
                BIORead:while (true){
                    try {
                        while ((realLen=in.read(contextBytes,0,maxLen))!=-1){
                            message.append(new String(contextBytes,0,realLen));

                            if (message.indexOf("over")!=-1){
                                break BIORead;
                            }
                        }

                    }catch (SocketTimeoutException e2){
                        LOGGER.info("这次没有从底层接收到任务数据报文，等待10毫秒，模拟事件Y的处理时间");
                        continue ;
                    }
                }
                LOGGER.info("服务器收到来自于端口：" + sourcePort + "的信息：" + message);

                out.write("回发响应信息！".getBytes());

                out.close();
                in.close();
                socket.close();


            }
        }catch (Exception e){
            LOGGER.error(e.getMessage(),e);
        }finally {
            if (serverSocket!=null){
                serverSocket.close();
            }
        }
    }

}
