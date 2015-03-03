package edu.umass.cs.gns.newApp;

import edu.umass.cs.gns.reconfiguration.reconfigurationutils.*;
import java.net.InetAddress;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.gns.reconfiguration.InterfaceRequest;

/**
 * @author Westy
 */
/*
 * This class maintains a demand profile that neither wants reports or reconfigurations.
 *
 * FIXME: Created this in part because the default profile was resulting in a reconfiguration that
 * was either not working or just exacerbating issues that we were trying to fix while debugging.
 * So we need to verify that the default DemandProfile works ok with our stuff instead of this one.
 */
public class NullDemandProfile extends AbstractDemandProfile {

  public enum Keys {

    SERVICE_NAME
  };

  public NullDemandProfile(String name) {
    super(name);
  }

  // deep copy constructor
  public NullDemandProfile(NullDemandProfile dp) {
    super(dp.name);
  }

  public NullDemandProfile(JSONObject json) throws JSONException {
    super(json.getString(Keys.SERVICE_NAME.toString()));
  }

  public static NullDemandProfile createDemandProfile(String name) {
    return new NullDemandProfile(name);
  }

  @Override
  public void register(InterfaceRequest request, InetAddress sender) {
  }

  @Override
  public boolean shouldReport() {
    return false;
  }

  @Override
  public JSONObject getStats() {
    JSONObject json = new JSONObject();
    try {
      json.put(Keys.SERVICE_NAME.toString(), this.name);
    } catch (JSONException je) {
      je.printStackTrace();
    }
    return json;
  }

  @Override
  public void reset() {
  }

  @Override
  public NullDemandProfile clone() {
    return new NullDemandProfile(this);
  }

  @Override
  public void combine(AbstractDemandProfile dp) {
  }

  @Override
  public ArrayList<InetAddress> shouldReconfigure(ArrayList<InetAddress> curActives) {
    return null;
  }

  @Override
  public void justReconfigured() {
  }
}