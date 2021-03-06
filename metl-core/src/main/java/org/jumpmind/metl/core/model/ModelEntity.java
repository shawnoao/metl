/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.model;

import java.util.ArrayList;
import java.util.List;

public class ModelEntity extends AbstractNamedObject implements IAuditable {

    private static final long serialVersionUID = 1L;

    List<ModelAttribute> modelAttributes;

    String modelId;

    String name;
    
    String description;

    public ModelEntity() {
        modelAttributes = new ArrayList<ModelAttribute>();
    }

    public ModelEntity(String id, String name) {
        this();
        setId(id);
        this.name = name;
    }

    public List<ModelAttribute> getModelAttributes() {
        return modelAttributes;
    }

    public void setModelAttributes(List<ModelAttribute> modelAttributes) {
        this.modelAttributes = modelAttributes;
    }

    public void addModelAttribute(ModelAttribute modelAttribute) {
        modelAttribute.setAttributeOrder(modelAttributes.size());
        this.modelAttributes.add(modelAttribute);
    }

    public void removeModelAttribute(ModelAttribute modelAttribute) {
        this.modelAttributes.remove(modelAttribute);
    }

    public ModelAttribute getModelAttributeByName(String name) {
        for (ModelAttribute modelAttribute : modelAttributes) {
            if (modelAttribute.getName().equalsIgnoreCase(name)) {
                return modelAttribute;
            }
        }
        return null;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelVersionId) {
        this.modelId = modelVersionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean hasOnlyPrimaryKeys() {
        boolean pksOnly = true;
        for (ModelAttribute modelAttribute : modelAttributes) {
            pksOnly &= modelAttribute.isPk();
        }
        return pksOnly;
    }

//    @Override
//    public AbstractObject copy() {
//        ModelEntity entity = (ModelEntity) super.copy();
//        entity.setModelAttributes(new ArrayList<ModelAttribute>());
//        for (ModelAttribute modelAttribute : modelAttributes) {
//            modelAttribute = (ModelAttribute) modelAttribute.copy();
//            modelAttribute.setEntityId(entity.getId());
//            entity.getModelAttributes().add(modelAttribute);
//        }
//
//        return entity;
//    }

}
