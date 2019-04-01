<p style="font-size:10px;">Project Astro: A Distributed Discovery and Lookup Service for Edge networks</p>

  <p>Cloud computing has revolutionized the availability and accessibility of resources. With increasing amounts of data
being generated at the edge of the network, it is impossible to send all the data to the cloud for pre-processing without compromising on latency and clogging the network bandwidth. Network latency can have serious impacts in the operation of
edge devices which require real-time data processing, such as autonomous vehicles.</p>
  <p>Fog computing came into being to tackle this problem by extending cloud computing to the edge of the network. This new paradigm is supported by the computing power provided by the vast number of mobile edge devices present today. This has opened a new array of services and applications that are at a close proximity to the end-users.</p>
  <p>A distinguishing characteristic of fog computing is its dense geographical distribution of heterogeneous nodes such
as Android devices, resource-limited structures like Raspberry Pi, or embedded devices like network-connected sensors,
and routers. If we can harness the data and compute resources of these nodes, we can develop a large-scale service infrastructure which can be scaled according to the requirements of the application.</p>
  <p>The heterogeneous nature of the system poses a lot of challenges in resource management of the underlying infrastructure. The foremost challenge is the discovery of such resources. The system can be visualized to comprise of two main parts, first - device owners who are willing to lease their resources, and second - users who want to use these resources. Our project aims to develop a service, Astro, for resource discovery, registry, sharing and access management in such an infrastructure. Astro is built on top of ZooKeeper, a light-weight coordination system. We present the underlying three-layer architecture of Astro. Taking into account the high mobility in the edge network and the recent release of ZooKeeper (ZK) to support dynamic reconfiguration of ZK servers, we present the implementation of the first layer using the ZK service.</p>
  
This repository contains:
1. Java implementation of Astro: This folder contains src and test. ASTRO/astro/src/main/java/test/Test.java contains the main runnable method. 
2. Zookeeper 3.5.4 beta 

Report for the same can be located at: ASTRO/ASTRO_report.pdf. This contains latency test results and future directions. 
