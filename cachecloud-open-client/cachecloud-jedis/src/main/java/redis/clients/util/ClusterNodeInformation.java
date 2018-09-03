package redis.clients.util;

import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.HostAndPort;

public class ClusterNodeInformation {
  private HostAndPort node;
  private String nodeId;
  private List<Integer> availableSlots;
  private List<ImportingSlot> slotsBeingImported;
  private List<MigratingSlot> slotsBeingMigrated;

  public ClusterNodeInformation(HostAndPort node) {
    this.node = node;
    this.availableSlots = new ArrayList<Integer>();
    this.slotsBeingImported = new ArrayList<ImportingSlot>();
    this.slotsBeingMigrated = new ArrayList<MigratingSlot>();
  }

  public void addAvailableSlot(int slot) {
    availableSlots.add(slot);
  }

  public void addSlotBeingImported(ImportingSlot slot) {
    slotsBeingImported.add(slot);
  }

  public void addSlotBeingMigrated(MigratingSlot slot) {
    slotsBeingMigrated.add(slot);
  }

  public HostAndPort getNode() {
    return node;
  }

  public List<Integer> getAvailableSlots() {
    return availableSlots;
  }

  public List<ImportingSlot> getSlotsBeingImported() {
    return slotsBeingImported;
  }

  public List<MigratingSlot> getSlotsBeingMigrated() {
    return slotsBeingMigrated;
  }

public String getNodeId() {
	return nodeId;
}

public void setNodeId(String nodeId) {
	this.nodeId = nodeId;
}

public static class MigratingSlot {
	private int slot;
	private String dstNodeId;
	
	public MigratingSlot(int slot,String dstNodeId){
		this.slot = slot;
		this.dstNodeId = dstNodeId;
	}
	public int getSlot() {
		return slot;
	}
	public void setSlot(int slot) {
		this.slot = slot;
	}
	public String getDstNodeId() {
		return dstNodeId;
	}
	public void setDstNodeId(String dstNodeId) {
		this.dstNodeId = dstNodeId;
	}
}
public static class ImportingSlot {
	private int slot;
	private String srcNodeId;
	
	public ImportingSlot(int slot,String srcNodeId){
		this.slot = slot;
		this.srcNodeId = srcNodeId;
	}
	public int getSlot() {
		return slot;
	}
	public void setSlot(int slot) {
		this.slot = slot;
	}
	
	public String getSrcNodeId() {
		return srcNodeId;
	}
	public void setSrcNodeId(String srcNodeId) {
		this.srcNodeId = srcNodeId;
	}
	
}

}
