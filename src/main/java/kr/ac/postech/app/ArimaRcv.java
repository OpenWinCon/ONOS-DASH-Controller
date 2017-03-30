package kr.ac.postech.app;

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

    static final String AP_MAC_ADDRESS = "b8:27:eb:d0:f3:f3";
    private final Logger log = LoggerFactory.getLogger(getClass());
    static HashMap<String, Integer> estimatedThroughput = new HashMap<>();

    public ArimaRcv(){
        try{
            serverSocket = new ServerSocket(9901);
        } catch (IOException e){
            e.printStackTrace();
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

                estimatedThroughput.put(msg.substring(0, dotIndex), Math.abs(value));
                //System.out.println(msg);
                socket.close();
            }catch(IOException e){
                ;
            }
        }
    }

    public static void printEstimatedThroughputMap(){
        System.out.println("[ Estimated bandwidth ] ");
        Set<Map.Entry<String, Integer>> set = estimatedThroughput.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            if(e.getKey().equals(AP_MAC_ADDRESS))
                ;//System.out.println("    AP:" + e.getKey() +", Bandwidth:" + e.getValue() +"kbps");
            else
                System.out.println("Client:" + e.getKey() +", Bandwidth:" + e.getValue() +"kbps");
        }
    }
}
