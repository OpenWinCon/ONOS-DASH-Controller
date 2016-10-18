package org.foo.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Created by hwan on 7/22/16.
 */

public class BufferInfoRcv implements Runnable{
    ServerSocket serverSocket;
    Thread thread;

    private final Logger log = LoggerFactory.getLogger(getClass());
    static HashMap<String, Integer> currentBufferLength = new HashMap<>();

    public BufferInfoRcv(){
        try{
            serverSocket = new ServerSocket(8800);
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

                System.out.println(msg);

                //int commaIndex = msg.indexOf(',');
                //int dotIndex = msg.indexOf('.');

                /*System.out.println("original msg : " + msg);
                System.out.println("commaIndex : " + commaIndex + " dotIndex : " + dotIndex);
                System.out.println("value : " + msg.substring(0, dotIndex));
                System.out.println("key : " + msg.substring(commaIndex+1));*/
                //int value = Integer.parseInt(msg.substring(0, dotIndex));

                //currentBufferLength.put(msg.substring(commaIndex+1), value);
                //System.out.println(msg);
                socket.close();
            }catch(IOException e){
                ;
            }
        }
    }

    public static void printCurrentBufferLengthMap(){
        Set<Map.Entry<String, Integer>> set = currentBufferLength.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            System.out.println("dev:" + e.getKey() +", buffer length:" + e.getValue() + "sec");
        }
    }
}
