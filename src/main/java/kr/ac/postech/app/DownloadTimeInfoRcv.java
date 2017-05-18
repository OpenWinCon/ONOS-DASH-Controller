package kr.ac.postech.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class DownloadTimeInfoRcv implements Runnable{
    ServerSocket serverSocket;
    Thread thread;

    private final Logger log = LoggerFactory.getLogger(getClass());
    static HashMap<String, Double> currentDownloadTime = new HashMap<>();

    public DownloadTimeInfoRcv(){
        try{
            serverSocket = new ServerSocket(9899);
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

                if(msg != null && msg.length() > 18){
                    int commaIndex = 0;
                    int valueSize = 0;
                    double value;

                    if(msg.contains("/")){
                        commaIndex = msg.indexOf('/');
                        valueSize = msg.substring(commaIndex+1).length() - 1;

                        value = Double.parseDouble(msg.substring(commaIndex+1, commaIndex+valueSize+1));

                        if(value >= 0)
                            currentDownloadTime.put(msg.substring(0, commaIndex), value);
                    }
                }
                socket.close();
            }catch(IOException e){
                ;
            }
        }
    }

    public static void printDownloadTimeMap(){
        System.out.println("[  ] ");
        Set<Map.Entry<String, Double>> set = currentDownloadTime.entrySet();
        Iterator<Map.Entry<String, Double>> it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry<String, Double> e = (Map.Entry<String, Double>)it.next();
            System.out.println("dev:" + e.getKey() +", Estimated available bw:" + e.getValue()*1024 + "kbps");
        }
    }
}