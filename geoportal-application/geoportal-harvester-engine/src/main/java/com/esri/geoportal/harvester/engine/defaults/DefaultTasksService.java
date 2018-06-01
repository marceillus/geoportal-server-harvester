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
package com.esri.geoportal.harvester.engine.defaults;

import com.esri.geoportal.commons.constants.MimeType;
import com.esri.geoportal.harvester.api.DataContent;
import com.esri.geoportal.harvester.api.defs.EntityDefinition;
import com.esri.geoportal.harvester.api.defs.TaskDefinition;
import com.esri.geoportal.harvester.api.ex.DataException;
import com.esri.geoportal.harvester.api.ex.DataInputException;
import com.esri.geoportal.harvester.api.ex.DataProcessorException;
import com.esri.geoportal.harvester.api.ex.InvalidDefinitionException;
import com.esri.geoportal.harvester.api.specs.InputBroker;
import com.esri.geoportal.harvester.api.specs.InputConnector;
import com.esri.geoportal.harvester.engine.services.TasksService;
import com.esri.geoportal.harvester.engine.managers.History;
import com.esri.geoportal.harvester.engine.managers.HistoryManager;
import com.esri.geoportal.harvester.engine.managers.TaskManager;
import com.esri.geoportal.harvester.engine.registers.InboundConnectorRegistry;
import com.esri.geoportal.harvester.engine.utils.CrudlException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Default tasks service.
 */
public class DefaultTasksService implements TasksService {
  protected final InboundConnectorRegistry inboundConnectorRegistry;
  protected final TaskManager taskManager;
  protected final HistoryManager historyManager;

  /**
   * Creates instance of the service.
   * @param inboundConnectorRegistry inbound connector registry.
   * @param taskManager task manager
   * @param historyManager history manager
   */
  public DefaultTasksService(InboundConnectorRegistry inboundConnectorRegistry, TaskManager taskManager, HistoryManager historyManager) {
    this.inboundConnectorRegistry = inboundConnectorRegistry;
    this.taskManager = taskManager;
    this.historyManager = historyManager;
  }

  @Override
  public List<Map.Entry<UUID, TaskDefinition>> selectTaskDefinitions(Predicate<? super Map.Entry<UUID, TaskDefinition>> predicate) throws DataProcessorException {
    try {
      return taskManager.list().stream().filter(predicate != null ? predicate : (Map.Entry<UUID, TaskDefinition> e) -> true).collect(Collectors.toList());
    } catch (CrudlException ex) {
      throw new DataProcessorException(String.format("Error selecting task definitions."), ex);
    }
  }

  @Override
  public TaskDefinition readTaskDefinition(UUID taskId) throws DataProcessorException {
    try {
      return taskManager.read(taskId);
    } catch (CrudlException ex) {
      throw new DataProcessorException(String.format("Error reading task definition: %s", taskId), ex);
    }
  }

  @Override
  public boolean deleteTaskDefinition(UUID taskId) throws DataProcessorException {
    try {
      historyManager.purgeHistory(taskId);
      return taskManager.delete(taskId);
    } catch (CrudlException ex) {
      throw new DataProcessorException(String.format("Error deleting task definition: %s", taskId), ex);
    }
  }

  @Override
  public UUID addTaskDefinition(TaskDefinition taskDefinition) throws DataProcessorException {
    try {
      return taskManager.create(taskDefinition);
    } catch (CrudlException ex) {
      throw new DataProcessorException(String.format("Error adding task definition: %s", taskDefinition), ex);
    }
  }

  @Override
  public TaskDefinition updateTaskDefinition(UUID taskId, TaskDefinition taskDefinition) throws DataProcessorException {
    try {
      TaskDefinition oldTaskDef = taskManager.read(taskId);
      if (oldTaskDef != null) {
        if (!taskManager.update(taskId, taskDefinition)) {
          oldTaskDef = null;
        }
      }
      return oldTaskDef;
    } catch (CrudlException ex) {
      throw new DataProcessorException(String.format("Error updating task definition: %s <-- %s", taskId, taskDefinition), ex);
    }
  }
  
  @Override
  public History getHistory(UUID taskId) throws DataProcessorException {
    try {
      return historyManager.buildHistory(taskId);
    } catch (CrudlException ex) {
      throw new DataProcessorException(String.format("Error getting history for: %s", taskId), ex);
    }
  }
  
  @Override
  public void purgeHistory(UUID taskId) throws DataProcessorException {
    try {
      historyManager.purgeHistory(taskId);
    } catch (CrudlException ex) {
      throw new DataProcessorException(String.format("Error purging history for: %s", taskId), ex);
    }
  }
  
  @Override
  public byte[] fetchContent(UUID taskId, String recordId) throws DataException {
    try {
      TaskDefinition taskDefinition = readTaskDefinition(taskId);
      InputBroker broker = newInputBroker(taskDefinition.getSource());
      DataContent dataContent = broker.readContent(recordId);

      MimeType mimeType = dataContent.getContentType().stream().findFirst().orElse(null);

      return mimeType != null ? dataContent.getContent(mimeType) : null;
    } catch (IOException|InvalidDefinitionException ex) {
      throw new DataException(String.format("Error fetching content from: %s -> $s", taskId, recordId), ex);
    }
  }
  
  /**
   * Creates new input broker.
   * @param entityDefinition input broker definition
   * @return input broker
   * @throws InvalidDefinitionException if invalid definition
   */
  private InputBroker newInputBroker(EntityDefinition entityDefinition) throws InvalidDefinitionException {
    InputConnector<InputBroker> dsFactory = inboundConnectorRegistry.get(entityDefinition.getType());

    if (dsFactory == null) {
      throw new InvalidDefinitionException("Invalid input broker definition");
    }

    return dsFactory.createBroker(entityDefinition);
  }
}
