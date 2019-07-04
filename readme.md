# OMF-Publisher
> The OSIsoft Message Format ([OMF](http://omf-docs.osisoft.com/en/v1.1/ "OMF")) defines a set of message headers and bodies that can be used to generate messages for ingestion into a compliant back-end system. The PI System and OCS both have a compliant OMF receiving endpoint.

>OMF can be used to develop data acquisition applications on platforms and in languages for which there are no supported OSIsoft libraries.

## Description
The **OMF-Publisher** is a Kura-Wire component, which can be used to transfer real data to an destination system data endpoint (for example to a PI system). OMF-Publisher is a component which can be easily installed onto your Kura-Device over the Kura - Webinterface. 
 **OMF-Publisher** sends messages via HTTP but OMF is independent of protocols, so you can use the code and rewrite it to choose another one. 
With the **OMF-Publisher** it is possible to use OMF v1.0 and v1.1.

## Features
- Send your data via OMF to a destination system
- compress your data for smaller data packages
- configure individual time-settings
- Caching the data due to connection loss

## Getting Started
1.  To use the **OMF-Publisher** you have to install the deployment package (*OMFPublisher#.dp*) over the Kura - Webinterface first. 

2. After the Installation you should check if it was successful:
 2.1 You find it under *Device --> Bundles*
 2.2 Search for the *OMFPublisher* - Bundle. The state of it should be **Active**.
3. Now the Wire-Component is installed and can be used:
 3.1 Drag 'n Drop the component *OMFPublisher* into the *Wire-Graph-Field*
 
4. Configure the *OMF-Publisher* 
 4.1 To connect your device with the destination system you need a *Producertoken* and a *Target-URL* of it
 4.2 Give your root-element an individual name
 
5. Create an Asset and connect it with the *OMFPublisher*