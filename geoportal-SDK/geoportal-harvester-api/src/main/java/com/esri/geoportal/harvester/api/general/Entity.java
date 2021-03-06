/*
 * Copyright 2016 Esri, Inc.
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
package com.esri.geoportal.harvester.api.general;

import com.esri.geoportal.harvester.api.defs.UITemplate;
import java.util.Locale;

/**
 * Entity.
 */
public interface Entity {
  /**
   * Gets item type.
   * @return item type.
   */
  String getType();
  
  /**
   * Gets entity UI template.
   * @param locale locale
   * @return entity UI template
   */
  UITemplate getTemplate(Locale locale);
}
