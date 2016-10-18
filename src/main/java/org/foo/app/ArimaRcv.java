package org.foo.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by offloading on 16. 9. 29.
 */
public class ArimaRcv implements Runnable {
    ServerSocket serverSocket;
    Thread thread;

    private final Logger log = LoggerFactory.getLogger(getClass());
    static HashMap<String, Integer> estimatedThroughput = new HashMap<>();

    public ArimaRcv(){
        try{
            serverSocket = new ServerSocket(8801);
        } catch (IOException e){
            ;
        }
    }
    public void start(){
        thread = new Thread(this);
        thread.start();
    }

    public void run(){
        while(true){
            try{
                Socket socket = serverSocket.accept();
                InputStream in = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String msg = br.readLine();
                msg = msg.substring(2);
                int dotIndex = msg.indexOf('.');

                int value = Integer.parseInt(msg.substring(dotIndex+1));

                estimatedThroughput.put(msg.substring(0, dotIndex), value);
                //System.out.println(msg);
                socket.close();
            }catch(IOException e){
                ;
            }
        }
    }

    public static void printEstimatedThroughputMap(){
        Set<Map.Entry<String, Integer>> set = estimatedThroughput.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        System.out.println("Estimated throughput::");

        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            System.out.println("dev:" + e.getKey() +", throughput:" + e.getValue() +"kbps");
        }
    }
}
