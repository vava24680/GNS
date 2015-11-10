/*
 * Copyright (C) 2013
 * University of Massachusetts
 * All Rights Reserved 
 */
package edu.umass.cs.gnsclient.client;

import edu.umass.cs.gnsclient.client.UniversalTcpClientExtended;
import edu.umass.cs.gnsclient.client.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnsclient.client.util.JSONUtils;
import edu.umass.cs.gnsclient.client.util.ServerSelectDialog;
import edu.umass.cs.gnsclient.client.util.ThreadUtils;
import edu.umass.cs.gnsclient.client.util.Utils;
import edu.umass.cs.gnsclient.exceptions.GnsException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Comprehensive functionality test for the GNS using the UniversalGnsClientFull.
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AliasTest {

  private static final String ACCOUNT_ALIAS = "david@westy.org"; // REPLACE THIS WITH YOUR ACCOUNT ALIAS
  private static final String PASSWORD = "password";
  private static UniversalTcpClientExtended client = null;
  /**
   * The address of the GNS server we will contact
   */
  private static GuidEntry masterGuid;
  private static final String alias = "ALIAS-" + Utils.randomString(4) + "@blah.org";

  public AliasTest() {
    if (client == null) {
      InetSocketAddress address = ServerSelectDialog.selectServer();
      client = new UniversalTcpClientExtended(address.getHostName(), address.getPort(), true);
    }
  }

  @Test
  public void test_01_CreateAccount() {
    try {
      masterGuid = GuidUtils.lookupOrCreateAccountGuid(client, ACCOUNT_ALIAS, PASSWORD, true);
    } catch (Exception e) {
      fail("Exception when we were not expecting it: " + e);
    }
  }

  @Test
  public void test_02_AliasAdd() {
    try {
      //
      // KEEP IN MIND THAT CURRENTLY ONLY ACCOUNT GUIDS HAVE ALIASES
      //
      // add an alias to the masterGuid
      client.addAlias(masterGuid, alias);
      // lookup the guid using the alias
      assertEquals(masterGuid.getGuid(), client.lookupGuid(alias));

      // grab all the aliases from the guid
      HashSet<String> actual = JSONUtils.JSONArrayToHashSet(client.getAliases(masterGuid));
      // make sure our new one is in there
      assertThat(actual, hasItem(alias));

    } catch (Exception e) {
      fail("Exception when we were not expecting it: " + e);
    }
  }

  @Test
  public void test_03_AliasRemove() {
    try {
      // now remove it 
      client.removeAlias(masterGuid, alias);
    } catch (Exception e) {
      fail("Exception while removing alias: " + e);
    }
    // make sure it is gone
//    try {
//      client.lookupGuid(alias);
//      System.out.println(alias + " should not exist (first read)");
//    } catch (GnsException e) {
//    } catch (IOException e) {
//      fail("Exception while looking up alias: " + e);
//    }
//    ThreadUtils.sleep(10);
//    try {
//      client.lookupGuid(alias);
//      fail(alias + " should not exist (second read)");
//    } catch (GnsException e) {
//    } catch (IOException e) {
//      fail("Exception while looking up alias: " + e);
//    }
    int cnt = 0;
    try {
      do {
        try {
          client.lookupGuid(alias);
          if (cnt++ > 10) {
            fail(alias + " should not exist (after 10 checks)");
            break;
          }

        } catch (IOException e) {
          fail("Exception while looking up alias: " + e);
        }
        ThreadUtils.sleep(10);
      } while (true);
      // the lookup should fail and throw to here
    } catch (GnsException e) {
      System.out.println(alias + " was gone on " + (cnt + 1) + " read");
    }
  }
}
