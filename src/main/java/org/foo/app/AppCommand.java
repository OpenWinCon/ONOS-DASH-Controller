/*
 * Copyright 2016 Open Networking Laboratory
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
package org.foo.app;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;


import java.util.Timer;
import java.util.TimerTask;

/**
 * Sample Apache Karaf CLI command
 */
@Command(scope = "onos", name = "bw",
        description = "Sample Apache Karaf CLI command")

public class AppCommand extends AbstractShellCommand {

    private AppComponent service;
    private BufferInfoRcv bufferInfo = new BufferInfoRcv();
    private ArimaRcv arimaRcv = new ArimaRcv();
    @Override
    protected void execute() {
        service = get(AppComponent.class);

        Timer timer = new Timer();
        MeasureTraffic mt = new MeasureTraffic();
        SendMsg sm = new SendMsg();


        timer.scheduleAtFixedRate(mt, 1000, 1000);
        timer.scheduleAtFixedRate(sm, 1000, 1000);

        //service.measureTraffic();
        //service.calculateAverageThroughput();
        //service.printEstimatedThroughputMap();
        //service.printEstimatedThroughputMap();
        bufferInfo.start();
        arimaRcv.start();
    }



    class MeasureTraffic extends TimerTask {
        public void run(){
                service.measureTraffic();
        }
    }


    class SendMsg extends TimerTask {
        public void run(){
            service.sendBitrateMsg();
        }
    }

}

