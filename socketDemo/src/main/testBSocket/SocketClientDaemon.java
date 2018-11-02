package testBSocket;

import java.util.concurrent.CountDownLatch;

public class SocketClientDaemon {
    public static void main(String[] args) throws Exception {
        Integer clientNumber = 20;
        CountDownLatch countDownLatch = new CountDownLatch(clientNumber);
        for (int i =0;i<clientNumber;i++){
            countDownLatch.countDown();
            SocketClientRequestThread clent = new SocketClientRequestThread(countDownLatch,i);
            new Thread(clent).start();

        }
        //这个wait不涉及到具体的实验逻辑，只是保持线程
        synchronized (SocketClientDaemon.class){
            SocketClientDaemon.class.wait();
        }
    }
}
