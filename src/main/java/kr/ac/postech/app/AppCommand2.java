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
package kr.ac.postech.app;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Command : dash-ctl-print
 */
@Command(scope = "onos", name = "dash-ctl-print",
        description = "Sample Apache Karaf CLI command")

public class AppCommand2 extends AbstractShellCommand {
    private AppComponent service;
    @Override
    protected void execute() {
        service = get(AppComponent.class);
        //Print following information
        Date dt = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd hh:mm:ss a]");
        System.out.println("==============" + sdf.format(dt).toString() + "==============");
        service.printCurrentThroughputMap();
        System.out.println("");
        ArimaRcv.printEstimatedThroughputMap();
        System.out.println("");
        BufferInfoRcv.printCurrentBufferLengthMap();
        System.out.println("");
        service.printSolutionMap();
        System.out.println("====================================================");
    }
}

