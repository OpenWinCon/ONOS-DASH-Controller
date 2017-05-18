package kr.ac.postech.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;



public class BufferInfoRcv implements Runnable{
    ServerSocket serverSocket = null;
    Thread thread;

    private final Logger log = LoggerFactory.getLogger(getClass());
    static HashMap<String, Integer> currentBufferLength = new HashMap<>();
    static HashMap<String, Integer> currentSegNumber = new HashMap<>();
    public BufferInfoRcv(){
        try{
            serverSocket = new ServerSocket(9900);
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
                String[] recievedInfo;
                Date dt = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, hh:mm:ss a");
             //   System.out.println("[" + sdf.format(dt).toString() + "]" + " " +msg);

                if(msg != null){

                    if(msg.contains(".")){
                        int bl, sn;
                        recievedInfo = msg.split("\\.");
                        if(recievedInfo.length > 2){
                            bl = Integer.parseInt(recievedInfo[1]);
                            sn = Integer.parseInt(recievedInfo[2]);
                            currentBufferLength.put(recievedInfo[0], bl);
                            currentSegNumber.put(recievedInfo[0], sn);
                        }

                    }
                }
                //System.out.println(msg);
                socket.close();
            }catch(IOException e){
                ;
            }
        }
    }

    public static void printCurrentBufferLengthMap(){
        System.out.println("[ Buffer length of clients ] ");
        Set<Map.Entry<String, Integer>> set = currentBufferLength.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            System.out.println("Client:" + e.getKey() +", Buffer length:" + e.getValue() + "sec" +", Seg #:" + currentSegNumber.get(e.getKey()));
        }
    }
}
