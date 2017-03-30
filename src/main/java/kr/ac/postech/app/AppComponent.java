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

import org.onosproject.net.Device;
import org.onosproject.net.device.*;
import org.apache.felix.scr.annotations.*;
import org.slf4j.Logger;


import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;



import static org.slf4j.LoggerFactory.getLogger;


/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service(value = AppComponent.class)

public class AppComponent {

    private static final int ARRAYLIST_CAPACITY = 30;
    static final String AP_MAC_ADDRESS = "b8:27:eb:d0:f3:f3";
    static final int BUFFER_THRESHOLD = 10;
    static int t_seg = 2;
    private static final String DST_IP = "10.1.100.1"; //AP address
    static double[][] psnr = new double[300][3];
    static int[] mpd_bitrate = {45652, 89283, 131087, 178351, 221600, 262537, 334349, 396126, 522286, 595491, 791182, 1032682, 1244778, 1546902, 2133691,
    2484135, 3078587, 3526922, 3840360, 4219897};

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;


    @Activate
    protected void activate()
    {
        log.info("Started");
        fillPSNR();
    }

    @Deactivate
    protected void deactivate()
    {
        log.info("Stopped");
    }
    HashMap<String, Integer> flowMap = new HashMap<>();
    HashMap<String, Integer> currentThroughput = new HashMap<>();
    HashMap<String, Integer> throughputFlag = new HashMap<>();
    HashMap<String, Integer> averageThroughput = new HashMap<>();
    HashMap<String, int[]> ueBitrate = new HashMap<String, int[]>();

    ArrayList<HashMap<String, Integer>> throughputHistory = new ArrayList<>(ARRAYLIST_CAPACITY);


    public void fillPSNR(){
        try{
            BufferedReader in = new BufferedReader(new FileReader("/home/offloading/DASH-Controller/src/main/java/kr/ac/postech/app/psnrdata"));
            String line;
            int i = 1;
            while((line = in.readLine()) != null){
                String[] spl = line.split("\\s");
                psnr[i][0] = Double.parseDouble(spl[0]);
                psnr[i][1] = Double.parseDouble(spl[1]);
                psnr[i][2] = Double.parseDouble(spl[2]);
                i++;
            }

        }catch(IOException e){
            System.err.println(e);
        }
    }

    public void printFlowMap(HashMap<String, Integer> map){
        Set<Map.Entry<String, Integer>> set = map.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            System.out.println("dev:" + e.getKey() +", bytes:" + e.getValue());
        }
    }

    public double logDiffer(double a1, double a2, double a3, int x){
        return (a1 * a2) / (a2*x + a3);
    }

    public void preSync(){
        Iterable<Device> devices = deviceService.getDevices();
        int byteRxSum = 0, byteRxSum2 = 0;
        for (Device device : devices) {
            List<PortStatistics> ports = deviceService.getPortDeltaStatistics(device.id());
            for(PortStatistics port : ports)
            {
                byteRxSum += port.bytesReceived();
            }
        }

        do{
            for (Device device : devices) {
                List<PortStatistics> ports = deviceService.getPortDeltaStatistics(device.id());
                for(PortStatistics port : ports)
                {
                    byteRxSum2 += port.bytesReceived();
                }
            }
        }while(byteRxSum == byteRxSum2);
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


            devid = device.id().toString().substring(7);
            devmacaddr = devid.substring(0,2)+":"+devid.substring(2,4)+":"+devid.substring(4,6)+
                    ":"+devid.substring(6,8)+":"+devid.substring(8,10)+":"+devid.substring(10,12);

            if(DownloadTimeInfoRcv.currentDownloadTime.get(devmacaddr) != null){
                kbps = ( (8 * byteRxSum) / DownloadTimeInfoRcv.currentDownloadTime.get(devmacaddr) ) / 1024;
            }
            else {
                kbps = ( (8 * byteRxSum) / 2 ) / 1024;
            }

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
            Set<Map.Entry<String, Integer>> set = throughputHistory.get(throughputHistory.size()-1).entrySet();
            Iterator<Map.Entry<String, Integer>> it = set.iterator();
            while (it.hasNext())
            {
                Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
                Socket socket = new Socket("127.0.0.1", 8899);
                OutputStream out = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                dos.writeUTF(e.getKey()+"."+e.getValue());


                //Send buffer information
                //dos.writeUTF(e.getKey()+"."+e.getValue()+"."+BufferInfoRcv.currentBufferLength.get(e.getKey()));

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
        System.out.println("[ Current throughput ]");
        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>)it.next();
            if(e.getKey().equals(AP_MAC_ADDRESS))
                ;//System.out.println("    AP:" + e.getKey() +", Throughput:" + e.getValue() +"kbps");
            else
                System.out.println("Client:" + e.getKey() +", Throughput:" + e.getValue() +"kbps");
        }
    }

    public void printSolutionMap(){
        System.out.println("[ Decided bitrate of clients ] ");
        Set<Map.Entry<String, int[]>> set = ueBitrate.entrySet();
        Iterator<Map.Entry<String, int[]>> it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry<String, int[]> e = it.next();
            System.out.println("Client:" + e.getKey() +", Number of segments:" + e.getValue()[0] +", Bitrate:" + e.getValue()[1]/1024 + "kbps");
        }
    }

    public boolean constraintChk(int T){
        Set<Map.Entry<String, int[]>> set = ueBitrate.entrySet();
        Iterator<Map.Entry<String, int[]>> it = set.iterator();
        double LHS = 0.0;
        int count = 0;
        while (it.hasNext()){
            Map.Entry<String, int[]> e = it.next();
            int temp[] = e.getValue();
            if(currentThroughput.get(e.getKey()) > 100)
            {
                if(ArimaRcv.estimatedThroughput.containsKey(e.getKey()) && ArimaRcv.estimatedThroughput.get(e.getKey())!=0) {
                    LHS += (temp[1] * t_seg) / ((double)Math.abs(ArimaRcv.estimatedThroughput.get(e.getKey()))*1024.0);
                    //System.out.println("temp[1] : " + temp[1]);
                    // System.out.println("divider : " + (double)Math.abs(ArimaRcv.estimatedThroughput.get(e.getKey()))*1024.0);
                }
                else {
                    LHS += (temp[1] * t_seg) / ((double)Math.abs(currentThroughput.get(e.getKey()))*1024.0);
                    // System.out.println("temp[1] : " + temp[1]);
                    // System.out.println("divider : " + (double)Math.abs(currentThroughput.get(e.getKey()))*1024.0);
                }
            }
            else
                count++;
        }
        //System.out.println("LHS : " + LHS);

        if(LHS > T || count == ueBitrate.size()){
            return false;
        }
        else{
            return true;
        }
    }

    public boolean bitrateMax(){
        Set<Map.Entry<String, int[]>> set = ueBitrate.entrySet();
        Iterator<Map.Entry<String, int[]>> it = set.iterator();
        while (it.hasNext()){
            Map.Entry<String, int[]> e = it.next();
            if(currentThroughput.get(e.getKey())>100 &&e.getValue()[1] != mpd_bitrate[mpd_bitrate.length-1])
                return false;
        }
        return true;
    }

    public int bitrateQuantization(int r){
        int i, qi=0;

        for(i = 0; i < mpd_bitrate.length; i++){
            if(mpd_bitrate[i] <= r)
                qi = i;
            else
                break;
        }
        return qi;
    }

    public void makeSolution(){
        Set<Map.Entry<String, Integer>> set = currentThroughput.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        int i = 0, seg1 = 1, seg2 = 1;
        int tmp[] = new int[2];
        double sol1, sol2, p1, p2;
        String UE[] = new String[2];
        while(it.hasNext())
        {
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>) it.next();
            if (!e.getKey().equals(AP_MAC_ADDRESS)) {
                UE[i] = e.getKey();
                i++;
            }
        }

        if(BufferInfoRcv.currentSegNumber.containsKey(UE[0]))
            seg1 = BufferInfoRcv.currentSegNumber.get(UE[0]);
        else
            seg1 = 1;
        if(BufferInfoRcv.currentSegNumber.containsKey(UE[1]))
            seg2 = BufferInfoRcv.currentSegNumber.get(UE[1]);
        else
            seg2 = 1;
        if(ArimaRcv.estimatedThroughput.containsKey(UE[0])) {
            if(ArimaRcv.estimatedThroughput.get(UE[0]) != 0)
                p1 = 1.0 / (ArimaRcv.estimatedThroughput.get(UE[0])*1024.0);
            else
                p1 = 1.0/1024.0;
        }
        else {
            if(currentThroughput.get(UE[0]) != 0)
                p1 = 1.0 / (currentThroughput.get(UE[0])*1024.0);
            else
                p1 = 1.0/1024.0;
        }
        if(ArimaRcv.estimatedThroughput.containsKey(UE[1])) {
            if(ArimaRcv.estimatedThroughput.get(UE[1]) != 0)
                p2 = 1.0 / (ArimaRcv.estimatedThroughput.get(UE[1])*1024.0);
            else
                p2 = 1.0/1024.0;
        }
        else {
            if(currentThroughput.get(UE[1]) != 0)
                p2 = 1.0 / (currentThroughput.get(UE[1])*1024.0);
            else
                p2 = 1/1024;
        }

        sol1 = (psnr[seg1][1]*psnr[seg2][0]*(psnr[seg1][0]*psnr[seg1][1]*psnr[seg2][1]-psnr[seg1][2]*psnr[seg2][0]*psnr[seg2][1]*p1+
                psnr[seg1][0]*psnr[seg1][1]*psnr[seg2][2]*p2)) / ( (psnr[seg1][0]*psnr[seg1][1]+psnr[seg1][1]*psnr[seg2][0])*psnr[seg1][1]*psnr[seg2][0]*psnr[seg2][1]*p1 );

        sol2 = (psnr[seg1][1]*psnr[seg2][0]*(psnr[seg1][1]*psnr[seg2][0]*psnr[seg2][1]+psnr[seg1][2]*psnr[seg2][0]*psnr[seg2][1]*p1-
                psnr[seg1][0]*psnr[seg1][1]*psnr[seg2][2]*p2)) / ( (psnr[seg1][0]*psnr[seg1][1]+psnr[seg1][1]*psnr[seg2][0])*psnr[seg1][1]*psnr[seg2][0]*psnr[seg2][1]*p2 );

        for(i = 0; i < 2; i++) {
            tmp[0] = 1;
            if(i==0)
                tmp[1] = (int)sol1;
            else
                tmp[1] = (int)sol2;
            ueBitrate.put(UE[i], tmp);
        }

    }


    public void sendBitrateMsg()
    {
        //long startTime = System.currentTimeMillis(), elapsedTime;
        int offset = 43000, seg;
        double diff, memo;
        int breaker = 0;
        Set<Map.Entry<String, Integer>> set = currentThroughput.entrySet();
        Set<Map.Entry<String, int[]>> set2 = ueBitrate.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        Iterator<Map.Entry<String, int[]>> it2 = set2.iterator();

        /*while(it.hasNext()){
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>) it.next();
            if (!e.getKey().equals(AP_MAC_ADDRESS)) {
                int[] tmp = {1, mpd_bitrate[0]};
                ueBitrate.put(e.getKey(), tmp);
            }
        }

        String max ="";
        while(constraintChk(2)){
            memo = 0.0;
            if(bitrateMax()){
                break;
            }
            it = set.iterator();
            while(it.hasNext()) {
                Map.Entry<String, Integer> e = (Map.Entry<String, Integer>) it.next();
                if (!e.getKey().equals(AP_MAC_ADDRESS)) {
                    if(BufferInfoRcv.currentSegNumber.get(e.getKey()) != null)
                        seg = BufferInfoRcv.currentSegNumber.get(e.getKey()) + 1;
                    else
                        seg = 1;
                    if(e.getValue() < 100)
                        diff = 0;
                    else
                        diff = logDiffer(psnr[seg][0], psnr[seg][0], psnr[seg][0], ueBitrate.get(e.getKey())[1] + offset);
                    //System.out.println("Dev : " + e.getKey());
                    //System.out.println("diff : " + diff);
                    //System.out.println("memo : " + memo);
                    if(diff > memo) {
                        memo = diff;
                        max = e.getKey();
                    }
                    //System.out.println("max : " + max);
                }
            }
            int temp[] = new int[2];
            if(max.length() > 10){
                temp[0] = 1;
                if(ueBitrate.get(max)[1] < mpd_bitrate[mpd_bitrate.length-1])
                    temp[1] = ueBitrate.get(max)[1] + offset;
                ueBitrate.put(max,temp);
            }
            breaker++;
            if(breaker > 100000){
                break;
            }
        }*/
        makeSolution();
        while(it2.hasNext()){
            Map.Entry<String, int[]> e = it2.next();
            int temp[] = new int[2];
            temp[0] = e.getValue()[0];
            temp[1] = mpd_bitrate[bitrateQuantization(e.getValue()[1])];
            ueBitrate.put(e.getKey(), temp);
            try {
                Socket socket = new Socket(DST_IP, 8000);
                OutputStream out = socket.getOutputStream();
                String msg = temp[0]+"."+e.getKey()+"."+temp[1];
                out.write(msg.getBytes());
                out.flush();
                socket.close();
            }catch(IOException e1)
            {
                ;
            }
            //System.out.println("Dev : " + e.getKey());
            //System.out.println("Bitrate : " + temp[1]);
        }
        //elapsedTime = System.currentTimeMillis() - startTime;
        //System.out.println(elapsedTime + "ms");
    }

    public void log()
    {
        Date dt = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd hh:mm:ss a]");
        System.out.println("==============" + sdf.format(dt).toString() + "==============");
        printCurrentThroughputMap();
        System.out.println("");
        ArimaRcv.printEstimatedThroughputMap();
        System.out.println("");
        BufferInfoRcv.printCurrentBufferLengthMap();
        System.out.println("");
        printSolutionMap();
        System.out.println("====================================================");
    }
}
