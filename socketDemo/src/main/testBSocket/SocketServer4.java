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

public class SocketServer4 {
    static {
        BasicConfigurator.configure();
    }
    private static Object xWait = new Object();

    private static final Log LOGGER = LogFactory.getLog(SocketServer4.class);

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(83);
        serverSocket.setSoTimeout(1000);
        try {
            while (true){
                Socket socket =null;
                try {
                    socket =  serverSocket.accept();
                }catch (SocketTimeoutException e){
                    synchronized (SocketServer4.xWait){
                        SocketServer4.LOGGER.info("这次没有从底层接收到任务数据报文，等待10毫秒，模拟事件X的处理时间");
                        SocketServer4.xWait.wait(1000);
                        continue;
                    }
                }

               //业务处理过程可以交给一个线程
                //最终改变不了.accept()只能一个一个接受socket的情况，阻塞
                SocketServerThread socketServerThread = new SocketServerThread(socket);
                new Thread(socketServerThread).start();

            }

        }catch (Exception e){

        }finally {
            if (serverSocket!=null){
                serverSocket.close();
            }
        }
    }

    /**
     * 当接收到客户端socket后，业务的处理过程可以交给一个线程来做。
     * 但还是改变不了socket被一个一个的做accept()的情况
     */
    static class SocketServerThread implements Runnable{

        /**
         * 日志
         */
        private final Log LOGGER = LogFactory.getLog(SocketServerThread.class);

        private Socket socket;

        public SocketServerThread (Socket socket) {
            this.socket = socket;
        }


        public void run() {
            InputStream in = null;
            OutputStream out = null;
            try{
                in = socket.getInputStream();
                out = socket.getOutputStream();

                Integer sourcePort = socket.getPort();
                int maxLen = 1024;
                byte[] contextBytes = new byte[maxLen];
                //使用线程，同样无法解决read方法的阻塞问题
                //也就是说read方法处同样会被阻塞，直到操作系统有数据准备好
                int realLen = in.read(contextBytes,0,maxLen);
                StringBuffer message = new StringBuffer();
                //下面我们收取信息（设置成非阻塞方式，这样read信息的时候，又可以做一些其他事情）
                this.socket.setSoTimeout(10);
                BIORead:while(true) {
                    try {
                        while((realLen = in.read(contextBytes, 0, maxLen)) != -1) {
                            message.append(new String(contextBytes , 0 , realLen));
                            /*
                             * 我们假设读取到“over”关键字，
                             * 表示客户端的所有信息在经过若干次传送后，完成
                             * */
                            if(message.indexOf("over") != -1) {
                                break BIORead;
                            }
                        }
                    } catch(SocketTimeoutException e2) {
                        //===========================================================
                        //      执行到这里，说明本次read没有接收到任何数据流
                        //      主线程在这里又可以做一些事情，记为Y
                        //===========================================================
                        LOGGER.info("这次没有从底层接收到任务数据报文，等待10毫秒，模拟事件Y的处理时间");
                        continue;
                    }
                }
                //下面打印信息
                Long threadId = Thread.currentThread().getId();
                LOGGER.info("服务器(线程：" + threadId + ")收到来自于端口：" + sourcePort + "的信息：" + message);


                out.write("会发响应信息！".getBytes());
            }catch (Exception e){
                LOGGER.error(e.getMessage(),e);

            }finally {

                    try {
                        if (in!=null){
                        in.close();
                        }
                        if (out!=null){
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

            }

        }
    }
}
