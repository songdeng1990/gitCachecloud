package redis.clients.util;

import redis.clients.jedis.HostAndPort;
import redis.clients.util.ClusterNodeInformation.ImportingSlot;
import redis.clients.util.ClusterNodeInformation.MigratingSlot;

public class ClusterNodeInformationParser {
  private static final String SLOT_IMPORT_IDENTIFIER = "-<-";
  private static final String SLOT_MIGRATING_IDENTIFIER = "->-";
  private static final String SLOT_IN_TRANSITION_IDENTIFIER = "[";
  public static final int SLOT_INFORMATIONS_START_INDEX = 8;
  public static final int HOST_AND_PORT_INDEX = 1;
  private static final int NODE_ID_INDEX = 0;
  private static final int NODE_ID_LENGTH = 40;

  public ClusterNodeInformation parse(String nodeInfo, HostAndPort current) {
    String[] nodeInfoPartArray = nodeInfo.split(" ");

    HostAndPort node = getHostAndPortFromNodeLine(nodeInfoPartArray, current);
    ClusterNodeInformation info = new ClusterNodeInformation(node);
    if (nodeInfoPartArray[NODE_ID_INDEX].length() == NODE_ID_LENGTH){
    	info.setNodeId(nodeInfoPartArray[NODE_ID_INDEX]);
    }
    

    if (nodeInfoPartArray.length >= SLOT_INFORMATIONS_START_INDEX) {
      String[] slotInfoPartArray = extractSlotParts(nodeInfoPartArray);
      fillSlotInformation(slotInfoPartArray, info);
    }

    return info;
  }
  
  public String getNodeId(String nodeInfo){
	  String[] nodeInfoPartArray = nodeInfo.split(" ");
	  if (nodeInfoPartArray[NODE_ID_INDEX].length() == NODE_ID_LENGTH){
	    	return nodeInfoPartArray[NODE_ID_INDEX];
	    }
	  throw new RuntimeException("Can not get nodeId from " + nodeInfo);
  }

  private String[] extractSlotParts(String[] nodeInfoPartArray) {
    String[] slotInfoPartArray = new String[nodeInfoPartArray.length
        - SLOT_INFORMATIONS_START_INDEX];
    for (int i = SLOT_INFORMATIONS_START_INDEX; i < nodeInfoPartArray.length; i++) {
      slotInfoPartArray[i - SLOT_INFORMATIONS_START_INDEX] = nodeInfoPartArray[i];
    }
    return slotInfoPartArray;
  }

  public HostAndPort getHostAndPortFromNodeLine(String[] nodeInfoPartArray, HostAndPort current) {
    String stringHostAndPort = nodeInfoPartArray[HOST_AND_PORT_INDEX];
    if (stringHostAndPort.contains("@")){
    	stringHostAndPort = stringHostAndPort.split("@")[0];
    }
    String[] arrayHostAndPort = stringHostAndPort.split(":");
    
    return new HostAndPort(arrayHostAndPort[0].isEmpty() ? current.getHost() : arrayHostAndPort[0],
        arrayHostAndPort[1].isEmpty() ? current.getPort() : Integer.valueOf(arrayHostAndPort[1]));
  }

  private void fillSlotInformation(String[] slotInfoPartArray, ClusterNodeInformation info) {
    for (String slotRange : slotInfoPartArray) {
      fillSlotInformationFromSlotRange(slotRange, info);
    }
  }

  private void fillSlotInformationFromSlotRange(String slotRange, ClusterNodeInformation info) {
    if (slotRange.startsWith(SLOT_IN_TRANSITION_IDENTIFIER)) {
      // slot is in transition
      int slot = Integer.parseInt(slotRange.substring(1).split("-")[0]);

      if (slotRange.contains(SLOT_IMPORT_IDENTIFIER)) {
        // import
    	String srcNodeId = slotRange.substring(1,slotRange.length()-1).split(SLOT_IMPORT_IDENTIFIER)[1];
        info.addSlotBeingImported(new ImportingSlot(slot, srcNodeId));
      } else {
        // migrate (->-)
    	String dstNodeId = slotRange.substring(1,slotRange.length()-1).split(SLOT_MIGRATING_IDENTIFIER)[1];
        info.addSlotBeingMigrated(new MigratingSlot(slot, dstNodeId));
      }
    } else if (slotRange.contains("-")) {
      // slot range
      String[] slotRangePart = slotRange.split("-");
      for (int slot = Integer.valueOf(slotRangePart[0]); slot <= Integer.valueOf(slotRangePart[1]); slot++) {
        info.addAvailableSlot(slot);
      }
    } else {
      // single slot
      info.addAvailableSlot(Integer.valueOf(slotRange));
    }
  }

}
