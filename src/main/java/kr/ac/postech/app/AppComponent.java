/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.ac.postech.app;

import kr.ac.postech.app.*;
import kr.ac.postech.app.ArimaRcv;
import org.onosproject.net.Device;
import org.onosproject.net.device.*;
import org.apache.felix.scr.annotations.*;
import org.slf4j.Logger;


import java.io.*;
import java.net.Socket;
import java.util.*;



import static org.slf4j.LoggerFactory.getLogger;


/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service(value = AppComponent.class)

public class AppComponent {

    private static final int ARRAYLIST_CAPACITY = 30;
    private static final String DST_IP = "10.1.100.1"; //AP address

    private final Logger log = getLogger(getClass());


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;


    @Activate
    protected void activate()
    {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate()p[]
    {
        log.info("Stopped");
    }
    HashMap<String, Integer> flowMap = new HashMap<>();
    HashMap<String, Integer> currentThroughput = new HashMap<>();
    HashMap<String, Integer> throughputFlag = new HashMap<>();
    HashMap<String, Integer> averageThroughput = new HashMap<>();


    ArrayList<HashMap<String, Integer>> throughputHistory = new ArrayList<>(ARRAYLIST_CAPACITY);




    public void printFlowMap(HashMap<String, Integer> map){
        Set<Map.Entry<String, Integer>> set = map.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            System.out.println("dev:" + e.getKey() +", bytes:" + e.getValue());
        }
    }

    public void measureTraffic() {
        Iterable<Device> devices = deviceService.getDevices();

        for (Device device : devices) {

            List<PortStatistics> ports = deviceService.getPortDeltaStatistics(device.id());
            long byteRxSum = 0;
            double kbps = 0.0;
            String devid, devmacaddr;
            for(PortStatistics port : ports)
            {
                byteRxSum += port.bytesReceived();
            }

            kbps = (8 * byteRxSum) / 1024.0;
            devid = device.id().toString().substring(7);
            devmacaddr = devid.substring(0,2)+":"+devid.substring(2,4)+":"+devid.substring(4,6)+
                    ":"+devid.substring(6,8)+":"+devid.substring(8,10)+":"+devid.substring(10,12);

            if(kbps > 500){
                if(throughputFlag.containsKey(devmacaddr)){
                    throughputFlag.put(devmacaddr, 0);
                }
            }
            else {
                if (currentThroughput.containsKey(devmacaddr) && currentThroughput.get(devmacaddr) > 200) {
                    if ((currentThroughput.get(devmacaddr) - kbps) / currentThroughput.get(devmacaddr) > 0.8) {
                        if (throughputFlag.containsKey(devmacaddr) && throughputFlag.get(devmacaddr) < 10) {
                            throughputFlag.put(devmacaddr, throughputFlag.get(devmacaddr) + 1);

                            if (kr.ac.postech.app.ArimaRcv.estimatedThroughput.containsKey(devmacaddr)) {
                                kbps = kr.ac.postech.app.ArimaRcv.estimatedThroughput.get(devmacaddr);
                            } else {
                                kbps = currentThroughput.get(devmacaddr);
                            }
                        } else if (!throughputFlag.containsKey(devmacaddr)) {
                            throughputFlag.put(devmacaddr, 1);
                            if (kr.ac.postech.app.ArimaRcv.estimatedThroughput.containsKey(devmacaddr)) {
                                kbps = kr.ac.postech.app.ArimaRcv.estimatedThroughput.get(devmacaddr);
                            } else {
                                kbps = currentThroughput.get(devmacaddr);
                            }
                        } else {
                            throughputFlag.put(devmacaddr, 0);
                        }
                    }
                }
            }
            currentThroughput.put(devmacaddr, (int) kbps); //MAC Address format
        }
        updateThroughputHistory();
    }

    public void updateThroughputHistory()
    {
        HashMap<String, Integer> tmpMap = new HashMap<>();
        tmpMap.putAll(currentThroughput);
        if(throughputHistory.size() < ARRAYLIST_CAPACITY)
        {
            throughputHistory.add(tmpMap);
        }
        else
        {
            for( int i = 0; i < ARRAYLIST_CAPACITY-1; i++ )
            {
                throughputHistory.set(i, throughputHistory.get(i+1));
            }
            throughputHistory.set(ARRAYLIST_CAPACITY-1,tmpMap);
        }

        // Send throughput history to ARIMA program
        try{
            int i = 0;
            Set<Map.Entry<String, Integer>> set = throughputHistory.get(throughputHistory.size()-1).entrySet();
            Iterator<Map.Entry<String, Integer>> it = set.iterator();
            while (it.hasNext())
            {
                Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
                Socket socket = new Socket("127.0.0.1", 8899);
                OutputStream out = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                dos.writeUTF(e.getKey()+"."+e.getValue());
                dos.close();
                socket.close();
            }
            Socket socket = new Socket("127.0.0.1", 8899);
            OutputStream out = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeUTF("upd");
            dos.close();
            socket.close();
        } catch(IOException e){
            ;
        }

    }

    public void printCurrentThroughputMap(){
        Set<Map.Entry<String, Integer>> set = currentThroughput.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        System.out.println("Current throughput::");
        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            System.out.println("dev:" + e.getKey() +", throughput:" + e.getValue() + "kbps");
        }
    }
    public void sendBitrateMsg()
    {
        Set<Map.Entry<String, Integer>> set = ArimaRcv.estimatedThroughput.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        try {
            while (it.hasNext()) {
                Socket socket = new Socket(DST_IP, 8000);
                OutputStream out = socket.getOutputStream();
                Map.Entry<String, Integer> e = (Map.Entry<String, Integer>) it.next();
                String tmp = e.getKey() + "." + e.getValue();
                out.write(tmp.getBytes());
                out.flush();
                socket.close();
            }
        } catch (IOException e) {
            ;
        }
    }






    //Below methods are not used now.

    public void printThroughputHistory(){
        for(int i = 0; i<throughputHistory.size(); i++){
            System.out.println("History index : "+i);
            Set<Map.Entry<String, Integer>> set = throughputHistory.get(i).entrySet();
            Iterator<Map.Entry<String, Integer>> it = set.iterator();
            while (it.hasNext())
            {
                Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
                System.out.println("dev:" + e.getKey() +", throughput:" + e.getValue() + "kbps");
            }
        }
    }

    public void calculateAverageThroughput()
    {
        for(int i = 0; i < throughputHistory.size(); i++){
            Set<Map.Entry<String, Integer>> set = throughputHistory.get(i).entrySet();
            Iterator<Map.Entry<String, Integer>> it = set.iterator();
            while (it.hasNext())
            {
                Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
                if(averageThroughput.containsKey(e.getKey())){
                    averageThroughput.put(e.getKey(), averageThroughput.get(e.getKey()) + e.getValue());
                }
                else{
                    averageThroughput.put(e.getKey(), e.getValue());
                }
                if(i == throughputHistory.size() - 1){
                    averageThroughput.put(e.getKey(), averageThroughput.get(e.getKey())/throughputHistory.size());
                }
            }
        }
        /*
        int tmp = (int)(Math.random() * set_of_bitrate.length);
        int tmp2 = (int)(Math.random() * set_of_bitrate.length);
        String sendmsg = Integer.toString(tmp2) + "." + set_of_bitrate[tmp];
        return sendmsg;
        */
    }

    public void printAverageThroughputMap(){
        System.out.println("Average throughput::");
        Set<Map.Entry<String, Integer>> set = averageThroughput.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            System.out.println("dev:" + e.getKey() +", Avg throughput:" + e.getValue() + "kbps");
        }
    }

    public void measureThroughput() throws InterruptedException {
        HashMap<String, Integer> oldFlowMap = new HashMap<>();
        oldFlowMap.putAll(flowMap);


        System.out.println("old flow map:");
        printFlowMap(oldFlowMap);
        Thread.sleep(2000);

        System.out.println("current flow map:");
        printFlowMap(flowMap);

        Set<Map.Entry<String, Integer>> set = flowMap.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            if(oldFlowMap.containsKey(e.getKey()))
            {
                //currentThroughput.put(""+e.getKey(), e.getValue() - oldFlowMap.get(e.getKey()));
            }
        }

        printCurrentThroughputMap();
    }


}
