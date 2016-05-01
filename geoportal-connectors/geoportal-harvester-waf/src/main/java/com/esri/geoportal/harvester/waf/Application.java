/*
 * Copyright 2016 Esri, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.esri.geoportal.harvester.waf;

import com.esri.geoportal.commons.robots.BotsConfig;
import com.esri.geoportal.commons.robots.BotsMode;
import com.esri.geoportal.harvester.api.n.BrokerDefinition;
import com.esri.geoportal.harvester.api.support.DataCollector;
import com.esri.geoportal.harvester.api.support.DataPrintStreamOutput;
import java.net.URL;
import java.util.Arrays;
import com.esri.geoportal.harvester.api.n.InputBroker;

/**
 * Application.
 */
public class Application {
  public static void main(String[] args) throws Exception {
    DataPrintStreamOutput destination = new DataPrintStreamOutput(System.out);
    
    for (String sUrl: args) {
      WafConnector connector = new WafConnector();
      URL start = new URL(sUrl);
      WafDefinition attributes = new WafDefinition();
      attributes.setHostUrl(start);
      attributes.setBotsConfig(BotsConfig.DEFAULT);
      attributes.setBotsMode(BotsMode.inherit);
      try (InputBroker<String> hv = connector.createBroker(attributes)) {
        DataCollector<String> dataCollector = new DataCollector<>(hv, Arrays.asList(new DataPrintStreamOutput[]{destination}));
        dataCollector.collect();
      }
    }
  }
}