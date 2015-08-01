/*
 * Copyright (c) 2015 University of Massachusetts
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * Initial developer(s): V. Arun
 */
package edu.umass.cs.gigapaxos.paxospackets;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author arun
 *
 */
@SuppressWarnings("javadoc")
public final class AcceptPacket extends PValuePacket {
	/**
	 * Sender node ID. FIXME: should just be the same as the ballot coordinator.
	 */
	public final int sender;

	public AcceptPacket(int nodeID, PValuePacket pValue, int slotNumber) {
		super(pValue);
		this.packetType = PaxosPacketType.ACCEPT;
		this.sender = nodeID;
		this.setMedianCheckpointedSlot(slotNumber);
	}

	public AcceptPacket(JSONObject json) throws JSONException {
		super(json);
		assert (PaxosPacket.getPaxosPacketType(json) == PaxosPacketType.ACCEPT); 
		this.packetType = PaxosPacketType.ACCEPT;
		this.sender = json.getInt(PaxosPacket.NodeIDKeys.SNDR.toString());
		this.paxosID = json.getString(PaxosPacket.Keys.PID.toString());
	}

	@Override
	public JSONObject toJSONObjectImpl() throws JSONException {
		JSONObject json = super.toJSONObjectImpl();
		json.put(PaxosPacket.NodeIDKeys.SNDR.toString(), sender);
		return json;
	}

	@Override
	protected String getSummaryString() {
		return super.getSummaryString();
	}
}
