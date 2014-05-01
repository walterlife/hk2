/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.hk2.configuration.hub.test;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glassfish.hk2.configuration.hub.api.Change;
import org.glassfish.hk2.configuration.hub.api.Hub;
import org.glassfish.hk2.configuration.hub.api.Type;
import org.glassfish.hk2.configuration.hub.api.WriteableBeanDatabase;
import org.glassfish.hk2.configuration.hub.api.WriteableType;
import org.glassfish.hk2.configuration.hub.internal.HubImpl;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.testing.junit.HK2Runner;

/**
 * @author jwells
 *
 */
public class HubTest extends HK2Runner {
    private final static String EMPTY_TYPE = "EmptyType";
    private final static String ONE_INSTANCE_TYPE = "OneInstanceType";
    private final static String TYPE_TWO = "TypeTwo";
    private final static String TYPE_THREE = "TypeThree";
    
    private final static String NAME_PROPERTY = "Name";
    private final static String OTHER_PROPERTY = "Other";
    
    private final static String ALICE = "Alice";
    private final static String BOB = "Bob";
    
    private final static String OTHER_PROPERTY_VALUE1 = "value1";
    private final static String OTHER_PROPERTY_VALUE2 = "value2";
    
    private Hub hub;
    private Map<String, Object> oneFieldBeanLikeMap = new HashMap<String, Object>();
    
    @Before
    public void before() {
        super.before();
        
        // This is necessary to make running in an IDE easier
        Hub hub = testLocator.getService(Hub.class);
        if (hub == null) {
            ServiceLocatorUtilities.addClasses(testLocator, HubImpl.class);
        }
        
        this.hub = testLocator.getService(Hub.class);
        
        oneFieldBeanLikeMap.put(NAME_PROPERTY, ALICE);
    }
    
    /**
     * Tests we can add an empty type to the database
     */
    @Test
    public void testAddEmptyType() {
        Assert.assertNull(hub.getCurrentDatabase().getType(EMPTY_TYPE));
        
        WriteableBeanDatabase wbd = hub.getWriteableDatabaseCopy();
        wbd.addType(EMPTY_TYPE);
        
        wbd.commit();
        
        try {
            Type emptyType = hub.getCurrentDatabase().getType(EMPTY_TYPE);
            
            Assert.assertNotNull(emptyType);
            Assert.assertEquals(0, emptyType.getInstances().size());
        }
        finally {
            // Cleanup
            wbd = hub.getWriteableDatabaseCopy();
            wbd.removeType(EMPTY_TYPE);
            wbd.commit();
        }
        
    }
    
    /**
     * Tests we can add an empty type to the database with a listener
     */
    @Test
    public void testAddEmptyTypeWithListener() {
        Assert.assertNull(hub.getCurrentDatabase().getType(EMPTY_TYPE));
        
        GenericBeanDatabaseUpdateListener listener = new GenericBeanDatabaseUpdateListener();
        hub.addListener(listener);
        
        WriteableBeanDatabase wbd = null;
        
        try {
            Hub hub = testLocator.getService(Hub.class);
            
            wbd = hub.getWriteableDatabaseCopy();
            wbd.addType(EMPTY_TYPE);
        
            wbd.commit();
        
            Type emptyType = hub.getCurrentDatabase().getType(EMPTY_TYPE);
            
            List<Change> changes = listener.getLastSetOfChanges();
            
            Assert.assertEquals(1, changes.size());
            
            Change change = changes.get(0);
            
            Assert.assertEquals(Change.ChangeCategory.ADD_TYPE, change.getChangeCategory());
            Assert.assertEquals(emptyType.getName(), change.getChangeType().getName());
            Assert.assertEquals(0, change.getChangeType().getInstances().size());
            Assert.assertNull(change.getInstanceKey());
            Assert.assertNull(change.getInstanceValue());
            Assert.assertNull(change.getModifiedProperties());
        }
        finally {
            // Cleanup
            if (wbd != null) {
                wbd = hub.getWriteableDatabaseCopy();
                wbd.removeType(EMPTY_TYPE);
                wbd.commit();
            }
            
            hub.removeListener(listener);
        }
        
    }
    
    /**
     * Tests adding a type with one instance
     */
    @Test
    public void addNewTypeWithOneInstance() {
        Assert.assertNull(hub.getCurrentDatabase().getType(ONE_INSTANCE_TYPE));
        
        GenericBeanDatabaseUpdateListener listener = new GenericBeanDatabaseUpdateListener();
        hub.addListener(listener);
        
        WriteableBeanDatabase wbd = null;
        
        try {
        
            wbd = hub.getWriteableDatabaseCopy();
            WriteableType wt = wbd.addType(ONE_INSTANCE_TYPE);
            
            wt.addInstance(ALICE, oneFieldBeanLikeMap);
        
            wbd.commit();
        
            Type oneInstanceType = hub.getCurrentDatabase().getType(ONE_INSTANCE_TYPE);
            
            List<Change> changes = listener.getLastSetOfChanges();
            
            Assert.assertEquals(2, changes.size());
            
            {
                Change typeChange = changes.get(0);
            
                Assert.assertEquals(Change.ChangeCategory.ADD_TYPE, typeChange.getChangeCategory());
                Assert.assertEquals(oneInstanceType.getName(), typeChange.getChangeType().getName());
                Assert.assertEquals(1, typeChange.getChangeType().getInstances().size());
                Assert.assertNull(typeChange.getInstanceKey());
                Assert.assertNull(typeChange.getInstanceValue());
                Assert.assertNull(typeChange.getModifiedProperties());
            }
            
            {
                Change instanceChange = changes.get(1);
            
                Assert.assertEquals(Change.ChangeCategory.ADD_INSTANCE, instanceChange.getChangeCategory());
                Assert.assertEquals(oneInstanceType.getName(), instanceChange.getChangeType().getName());
                Assert.assertEquals(1, instanceChange.getChangeType().getInstances().size());
                Assert.assertEquals(ALICE, instanceChange.getInstanceKey());
                Assert.assertEquals(oneFieldBeanLikeMap, instanceChange.getInstanceValue());
                Assert.assertNull(instanceChange.getModifiedProperties());
            }
        }
        finally {
            // Cleanup
            if (wbd != null) {
                wbd = hub.getWriteableDatabaseCopy();
                wbd.removeType(ONE_INSTANCE_TYPE);
                wbd.commit();
            }
            
            hub.removeListener(listener);
        }
        
    }
    
    private void addType(String typeName) {
        WriteableBeanDatabase wbd = hub.getWriteableDatabaseCopy();
        
        wbd.addType(typeName);
        
        wbd.commit();
    }
    
    private void addTypeAndInstance(String typeName, String instanceKey, Object instanceValue) {
        WriteableBeanDatabase wbd = hub.getWriteableDatabaseCopy();
        
        WriteableType wt = wbd.findOrAddWriteableType(typeName);
        
        wt.addInstance(instanceKey, instanceValue);
        
        wbd.commit();
    }
    
    private void removeType(String typeName) {
        WriteableBeanDatabase wbd = hub.getWriteableDatabaseCopy();
        
        wbd.removeType(typeName);
        
        wbd.commit();
    }
    
    /**
     * Tests adding an instance to an existing a type
     */
    @Test
    public void addInstanceToExistingType() {
        addType(ONE_INSTANCE_TYPE);
        
        GenericBeanDatabaseUpdateListener listener = null;
        WriteableBeanDatabase wbd = null;
        
        try {
            listener = new GenericBeanDatabaseUpdateListener();
            hub.addListener(listener);
        
            wbd = hub.getWriteableDatabaseCopy();
            WriteableType wt = wbd.getWriteableType(ONE_INSTANCE_TYPE);
            Assert.assertNotNull(wt);
            
            wt.addInstance(ALICE, oneFieldBeanLikeMap);
        
            wbd.commit();
        
            Type oneInstanceType = hub.getCurrentDatabase().getType(ONE_INSTANCE_TYPE);
            
            List<Change> changes = listener.getLastSetOfChanges();
            
            Assert.assertEquals(1, changes.size());
            
            {
                Change instanceChange = changes.get(0);
            
                Assert.assertEquals(Change.ChangeCategory.ADD_INSTANCE, instanceChange.getChangeCategory());
                Assert.assertEquals(oneInstanceType.getName(), instanceChange.getChangeType().getName());
                Assert.assertEquals(1, instanceChange.getChangeType().getInstances().size());
                Assert.assertEquals(ALICE, instanceChange.getInstanceKey());
                Assert.assertEquals(oneFieldBeanLikeMap, instanceChange.getInstanceValue());
                Assert.assertNull(instanceChange.getModifiedProperties());
            }
        }
        finally {
            // Cleanup
            if (listener != null) {
                hub.removeListener(listener);
            }
            
            if (wbd != null) {
                removeType(ONE_INSTANCE_TYPE);
            }
            
        }
    }
    
    /**
     * Tests adding an instance to an existing a type
     */
    @Test
    public void testModifyProperty() {
        addTypeAndInstance(TYPE_TWO, ALICE, new GenericJavaBean(ALICE, OTHER_PROPERTY_VALUE1));
        
        GenericBeanDatabaseUpdateListener listener = null;
        WriteableBeanDatabase wbd = null;
        
        try {
            listener = new GenericBeanDatabaseUpdateListener();
            hub.addListener(listener);
        
            wbd = hub.getWriteableDatabaseCopy();
            WriteableType wt = wbd.getWriteableType(TYPE_TWO);
            Assert.assertNotNull(wt);
            
            GenericJavaBean newBean = new GenericJavaBean(ALICE, OTHER_PROPERTY_VALUE2);
            wt.modifyInstance(ALICE, newBean,
                    new PropertyChangeEvent(newBean, OTHER_PROPERTY, OTHER_PROPERTY_VALUE1, OTHER_PROPERTY_VALUE2));
        
            wbd.commit();
        
            Type typeTwo = hub.getCurrentDatabase().getType(TYPE_TWO);
            
            List<Change> changes = listener.getLastSetOfChanges();
            
            Assert.assertEquals(1, changes.size());
            
            {
                Change instanceChange = changes.get(0);
            
                Assert.assertEquals(Change.ChangeCategory.MODIFY_INSTANCE, instanceChange.getChangeCategory());
                Assert.assertEquals(TYPE_TWO, instanceChange.getChangeType().getName());
                Assert.assertEquals(1, instanceChange.getChangeType().getInstances().size());
                Assert.assertEquals(ALICE, instanceChange.getInstanceKey());
                Assert.assertEquals(newBean, instanceChange.getInstanceValue());
                
                List<PropertyChangeEvent> propertyChanges = instanceChange.getModifiedProperties();
                Assert.assertNotNull(propertyChanges);
                Assert.assertEquals(1, propertyChanges.size());
                
                PropertyChangeEvent pce = propertyChanges.get(0);
                
                Assert.assertEquals(OTHER_PROPERTY, pce.getPropertyName());
                Assert.assertEquals(OTHER_PROPERTY_VALUE1, pce.getOldValue());
                Assert.assertEquals(OTHER_PROPERTY_VALUE2, pce.getNewValue());
                Assert.assertEquals(newBean, pce.getSource());
            }
            
            typeTwo = hub.getCurrentDatabase().getType(TYPE_TWO);
            
            GenericJavaBean bean = (GenericJavaBean) typeTwo.getInstance(ALICE);
            
            Assert.assertEquals(ALICE, bean.getName());
            Assert.assertEquals(OTHER_PROPERTY_VALUE2, bean.getOther());
        }
        finally {
            // Cleanup
            if (listener != null) {
                hub.removeListener(listener);
            }
            
            if (wbd != null) {
                removeType(TYPE_TWO);
            }
            
        }
    }
    
    /**
     * Tests findOrAddWriteableType and other accessors
     */
    @Test
    public void testFindOrAdd() {
        GenericJavaBean addedBean = new GenericJavaBean(ALICE, OTHER_PROPERTY_VALUE1);
        addTypeAndInstance(TYPE_TWO, ALICE, addedBean);
        
        WriteableBeanDatabase wbd = null;
        
        try {
            wbd = hub.getWriteableDatabaseCopy();
            
            GenericJavaBean gjb = (GenericJavaBean) wbd.getInstance(TYPE_TWO, ALICE);
            Assert.assertNotNull(gjb);
            Assert.assertEquals(addedBean, gjb);
        
            WriteableType wt = wbd.findOrAddWriteableType(TYPE_TWO);
            Assert.assertNotNull(wt);
            
            gjb = (GenericJavaBean) wt.getInstance(ALICE);
            Assert.assertNotNull(gjb);
            Assert.assertEquals(addedBean, gjb);
            
            WriteableType wt3 = wbd.findOrAddWriteableType(TYPE_THREE);
            Assert.assertNotNull(wt3);
            
            gjb = (GenericJavaBean) wt3.getInstance(ALICE);
            Assert.assertNull(gjb);
        }
        finally {
            // Cleanup
            
            if (wbd != null) {
                removeType(TYPE_TWO);
            }
            
        }
    }
    
    /**
     * Tests removing an instance
     */
    @Test
    public void testRemoveInstance() {
        addTypeAndInstance(TYPE_TWO, ALICE, new GenericJavaBean(ALICE, OTHER_PROPERTY_VALUE1));
        addTypeAndInstance(TYPE_TWO, BOB, new GenericJavaBean(BOB, OTHER_PROPERTY_VALUE1));
        
        GenericBeanDatabaseUpdateListener listener = null;
        WriteableBeanDatabase wbd = null;
        
        try {
            listener = new GenericBeanDatabaseUpdateListener();
            hub.addListener(listener);
        
            wbd = hub.getWriteableDatabaseCopy();
            WriteableType wt = wbd.getWriteableType(TYPE_TWO);
            Assert.assertNotNull(wt);
            
            GenericJavaBean removed = (GenericJavaBean) wt.removeInstance(ALICE);
            Assert.assertNotNull(removed);
            Assert.assertEquals(ALICE, removed.getName());
        
            wbd.commit();
        
            Type typeTwo = hub.getCurrentDatabase().getType(TYPE_TWO);
            
            List<Change> changes = listener.getLastSetOfChanges();
            
            Assert.assertEquals(1, changes.size());
            
            {
                Change instanceChange = changes.get(0);
            
                Assert.assertEquals(Change.ChangeCategory.REMOVE_INSTANCE, instanceChange.getChangeCategory());
                Assert.assertEquals(TYPE_TWO, instanceChange.getChangeType().getName());
                Assert.assertEquals(1, instanceChange.getChangeType().getInstances().size());
                Assert.assertEquals(ALICE, instanceChange.getInstanceKey());
                Assert.assertEquals(removed, instanceChange.getInstanceValue());
                Assert.assertNull(instanceChange.getModifiedProperties());
            }
            
            typeTwo = hub.getCurrentDatabase().getType(TYPE_TWO);
            
            GenericJavaBean bean = (GenericJavaBean) typeTwo.getInstance(ALICE);
            Assert.assertNull(bean);
            
            // Make sure Bob is still there though!
            bean = (GenericJavaBean) typeTwo.getInstance(BOB);
            Assert.assertNotNull(bean);
            Assert.assertEquals(BOB, bean.getName());
        }
        finally {
            // Cleanup
            if (listener != null) {
                hub.removeListener(listener);
            }
            
            if (wbd != null) {
                removeType(TYPE_TWO);
            }
            
        }
    }

}
