/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 */
package edu.umass.cs.gnsclient.client.tcp.packet;

import edu.umass.cs.gnsclient.client.GNSClient;
import org.json.JSONException;

/**
 * Provides the basics for Packets including a type field.
 * 
 * @author westy
 */
public abstract class BasicPacket implements PacketInterface {

  /**
   * The packet type *
   */
  protected Packet.PacketType type;

  @Override
  public String toString() {
    try {
      return this.toJSONObject().toString();
    } catch (JSONException e) {
      GNSClient.getLogger().severe("Problem converting packet to string:" + e);
      return "BasicPacket{" + "type=" + getType() + '}';
    }
  }

  /**
   * @return the type
   */
  public Packet.PacketType getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(Packet.PacketType type) {
    this.type = type;
  }

}
