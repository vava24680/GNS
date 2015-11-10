/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 */
package edu.umass.cs.gnsclient.client.tcp.packet;

import edu.umass.cs.gnsclient.client.GNSClient;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/**
 * So we have these packets see and we convert them back and forth to and from JSON Objects.
 * And send them over UDP and TCP connections. And we have an enum called PacketType that we
 * use to keep track of the type of packet that it is.
 *
 * @author westy
 */
public class Packet {

  /**
   * Defines the type of this packet *
   */
  public final static String TYPE = "type";
  //Type of packets

  public enum PacketType implements IntegerPacketType {
    COMMAND(7),
    COMMAND_RETURN_VALUE(8);

    private int number;
    private static final Map<Integer, PacketType> map = new HashMap<Integer, PacketType>();

    static {
      for (PacketType type : PacketType.values()) {
        if (map.containsKey(type.getInt())) {
          GNSClient.getLogger().warning("**** Duplicate ID number for packet type " + type + ": " + type.getInt());
        }
        map.put(type.getInt(), type);
      }
    }

    private PacketType(int number) {
      this.number = number;
    }

    @Override
    public int getInt() {
      return number;
    }

    public static PacketType getPacketType(int number) {
      return map.get(number);
    }
  }

  // some shorthand helpers
  public static PacketType getPacketType(int number) {
    return PacketType.getPacketType(number);
  }

  public static PacketType getPacketType(JSONObject json) throws JSONException {
    if (Packet.hasPacketTypeField(json)) {
      return PacketType.getPacketType(json.getInt(TYPE));
    }
    throw new JSONException("Missing packet type field");
  }

  public static boolean hasPacketTypeField(JSONObject json) {
    return json.has(TYPE);
  }

  public static void putPacketType(JSONObject json, PacketType type) throws JSONException {
    json.put(TYPE, type.getInt());
  }

}
