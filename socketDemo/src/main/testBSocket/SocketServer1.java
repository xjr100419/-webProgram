package testBSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer1 {
    static {
        BasicConfigurator.configure();
    }
    /**
     * 日志
     */
    private static final Log LOGGER = LogFactory.getLog(SocketServer1.class);

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(83);

        try {
            while (true) {
                Socket socket = serverSocket.accept();
                //下面我们收取信息
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                Integer sourcePort = socket.getPort();

                int maxLen = 2048;
                byte[] contextBytes = new byte[maxLen];
                //这里也会被阻塞，直到数据准备好
                int realLen = in.read(contextBytes,0,maxLen);
                //读取消息
                String message = new String(contextBytes,0,realLen);
                //打印日志
                LOGGER.info("服务器收到来自端口："+sourcePort+"的信息："+message);
                //返回信息
                out.write("回发响应信息！".getBytes());

                //关闭流
                out.close();
                in.close();
                socket.close();

            }
        }catch (Exception e){
            LOGGER.error(e.getMessage(),e);
        }finally {
            if(serverSocket!=null){
                serverSocket.close();
            }
        }

    }

}
