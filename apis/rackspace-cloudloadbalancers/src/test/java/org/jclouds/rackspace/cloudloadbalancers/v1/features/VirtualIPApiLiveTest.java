/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.rackspace.cloudloadbalancers.v1.features;

import static org.jclouds.rackspace.cloudloadbalancers.v1.predicates.LoadBalancerPredicates.awaitAvailable;
import static org.jclouds.rackspace.cloudloadbalancers.v1.predicates.LoadBalancerPredicates.awaitDeleted;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancerCreate;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.NodeAdd;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.VirtualIP;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.VirtualIPWithId;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.VirtualIP.Type;
import org.jclouds.rackspace.cloudloadbalancers.v1.internal.BaseCloudLoadBalancersApiLiveTest;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

/**
 * @author Everett Toews
 */
@Test(groups = "live", singleThreaded = true, testName = "VirtualIPApiLiveTest")
public class VirtualIPApiLiveTest extends BaseCloudLoadBalancersApiLiveTest {
   private LoadBalancer lb;
   private String zone;

   public void testCreateLoadBalancer() {
      NodeAdd nodeAdd = NodeAdd.builder().address("192.168.1.1").port(8080).build();
      LoadBalancerCreate lbCreate = LoadBalancerCreate.builder()
            .name(prefix+"-jclouds").protocol("HTTP").port(80).virtualIPType(Type.PUBLIC).node(nodeAdd).build(); 

      zone = Iterables.getFirst(clbApi.getConfiguredZones(), null);
      lb = clbApi.getLoadBalancerApiForZone(zone).create(lbCreate);
      
      assertTrue(awaitAvailable(clbApi.getLoadBalancerApiForZone(zone)).apply(lb));
   }

   @Test(dependsOnMethods = "testCreateLoadBalancer")
   public void testCreateVirtualIPs() throws Exception {
      clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).create(VirtualIP.publicIPv6());
      assertTrue(awaitAvailable(clbApi.getLoadBalancerApiForZone(zone)).apply(lb));
      clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).create(VirtualIP.publicIPv6());
      assertTrue(awaitAvailable(clbApi.getLoadBalancerApiForZone(zone)).apply(lb));
      clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).create(VirtualIP.publicIPv6());
      assertTrue(awaitAvailable(clbApi.getLoadBalancerApiForZone(zone)).apply(lb));

      Iterable<VirtualIPWithId> actualVirtualIPs = clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).list();

      assertEquals(Iterators.size(actualVirtualIPs.iterator()), 5);
   }
   
   @Test(dependsOnMethods = "testCreateVirtualIPs")
   public void testRemoveSingleVirtualIP() throws Exception {
      Iterable<VirtualIPWithId> actualVirtualIPs = clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).list();
      VirtualIPWithId removedVirtualIP = Iterables.getFirst(actualVirtualIPs, null);
      
      assertTrue(clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).delete(removedVirtualIP.getId()));
      assertTrue(awaitAvailable(clbApi.getLoadBalancerApiForZone(zone)).apply(lb));
      
      actualVirtualIPs = clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).list();

      assertEquals(Iterators.size(actualVirtualIPs.iterator()), 4);
   }
   
   @Test(dependsOnMethods = "testRemoveSingleVirtualIP")
   public void testRemoveManyVirtualIPs() throws Exception {
      Iterable<VirtualIPWithId> actualVirtualIPs = clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).list();
      VirtualIPWithId removedVirtualIP1 = Iterables.getFirst(actualVirtualIPs, null);
      VirtualIPWithId removedVirtualIP2 = Iterables.getLast(actualVirtualIPs);
      List<Integer> removedVirtualIPIds = ImmutableList.<Integer> of(removedVirtualIP1.getId(), removedVirtualIP2.getId());
      
      assertTrue(clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).delete(removedVirtualIPIds));
      assertTrue(awaitAvailable(clbApi.getLoadBalancerApiForZone(zone)).apply(lb));
      
      actualVirtualIPs = clbApi.getVirtualIPApiForZoneAndLoadBalancer(zone, lb.getId()).list();

      assertEquals(Iterators.size(actualVirtualIPs.iterator()), 2);
   }

   @Override
   @AfterGroups(groups = "live")
   protected void tearDownContext() {
      assertTrue(awaitAvailable(clbApi.getLoadBalancerApiForZone(zone)).apply(lb));
      clbApi.getLoadBalancerApiForZone(zone).delete(lb.getId());
      assertTrue(awaitDeleted(clbApi.getLoadBalancerApiForZone(zone)).apply(lb));
      super.tearDownContext();
   }
}
