package kr.ac.postech.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by hwan on 7/22/16.
 */

public class WBestInfoRcv implements Runnable{
    ServerSocket serverSocket;
    Thread thread;

    private final Logger log = LoggerFactory.getLogger(getClass());
    static HashMap<String, Integer> currentWirelessBW = new HashMap<>();

    public WBestInfoRcv(){
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

                Date dt = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, hh:mm:ss a");
                System.out.println("[" + sdf.format(dt).toString() + "]" + " " +msg);

                if(msg != null){
                    int dotIndex = 0;
                    int valueSize = 0;
                    int value;

                    if(msg.contains(".")){
                        dotIndex = msg.indexOf('.');
                        valueSize = msg.substring(dotIndex+1).length() - 1;

                        value = Integer.parseInt(msg.substring(dotIndex+1, dotIndex+valueSize+1));

                        if(value >= 0)
                            currentWirelessBW.put(msg.substring(0, dotIndex), value);
                    }
                }
                //System.out.println(msg);
                socket.close();
            }catch(IOException e){
                ;
            }
        }
    }

    public static void printCurrentWirelessBWMap(){
        Set<Map.Entry<String, Integer>> set = currentWirelessBW.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            System.out.println("dev:" + e.getKey() +", buffer length:" + e.getValue() + "sec");
        }
    }
}