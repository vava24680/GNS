/*
 *
 *  Copyright (c) 2015 University of Massachusetts
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  Initial developer(s): Abhigyan Sharma, Westy
 *
 */
package edu.umass.cs.gnsserver.gnsapp.clientSupport;

import com.google.common.collect.Sets;

import edu.umass.cs.gnscommon.exceptions.server.FailedDBOperationException;
import edu.umass.cs.gnscommon.exceptions.server.FieldNotFoundException;
import edu.umass.cs.gnscommon.exceptions.server.RecordNotFoundException;
import edu.umass.cs.gnsserver.main.GNSConfig;
import edu.umass.cs.gnscommon.utils.Base64;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import static edu.umass.cs.gnscommon.GnsProtocol.*;
import edu.umass.cs.gnsserver.gnsapp.GNSApplicationInterface;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.ClientUtils;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.MetaDataTypeName;
import edu.umass.cs.gnsserver.gnsapp.recordmap.BasicRecordMap;
import edu.umass.cs.utils.Util;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.logging.Logger;

/**
 * Provides signing and ACL checks for commands.
 *
 * @author westy
 */
public class NSAccessSupport {

  private static final Logger LOG = Logger.getLogger(NSAccessSupport.class.getName());

  private static KeyFactory keyFactory;
  private static Signature sig;

  static {
    try {
      keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
      sig = Signature.getInstance(SIGNATURE_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      LOG.severe("Unable to initialize for authentication:" + e);
    }
  }

  /**
   * Verifies that the signature corresponds to the message using the public key.
   *
   * @param accessorPublicKey
   * @param signature
   * @param message
   * @return true if the signature verifies successfully
   * @throws InvalidKeyException
   * @throws SignatureException
   * @throws UnsupportedEncodingException
   * @throws InvalidKeySpecException
   */
  public static boolean verifySignature(String accessorPublicKey, String signature, String message) throws
          InvalidKeyException, SignatureException, UnsupportedEncodingException, InvalidKeySpecException {
    if (!GNSConfig.enableSignatureAuthentication) {
      return true;
    }
    byte[] publickeyBytes = Base64.decode(accessorPublicKey);
    if (publickeyBytes == null) { // bogus public key
      LOG.log(Level.FINE, "&&&&Base 64 decoding is bogus!!!");
      return false;
    }
    LOG.log(Level.FINE,
            "public_key:{0}, signature:{1}, message:{2}",
            new Object[]{Util.truncate(accessorPublicKey, 16, 16),
              Util.truncate(signature, 16, 16),
              Util.truncate(message, 16, 16)});
    boolean result = verifySignatureInternal(publickeyBytes, signature, message);
    LOG.log(Level.FINE,
            "public_key:{0} {1} as author of message:{2}",
            new Object[]{Util.truncate(accessorPublicKey, 16, 16),
              (result ? " verified " : " NOT verified "),
              Util.truncate(message, 16, 16)});
    return result;
  }

  private static synchronized boolean verifySignatureInternal(byte[] publickeyBytes, String signature, String message)
          throws InvalidKeyException, SignatureException, UnsupportedEncodingException, InvalidKeySpecException {

    //KeyFactory keyFactory = KeyFactory.getInstance(RSAALGORITHM);
    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publickeyBytes);
    PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

    //Signature sig = Signature.getInstance(SIGNATUREALGORITHM);
    sig.initVerify(publicKey);
    sig.update(message.getBytes("UTF-8"));
    return sig.verify(Base64.decode(signature));
  }

  /**
   * Checks to see if the reader given in readerInfo can access the field of the user given by guidInfo.
   * Access type is some combo of read, and write, and blacklist or whitelist.
   * Note: Blacklists are currently not activated.
   *
   * @param access
   * @param guid
   * @param field
   * @param accessorGuid
   * @param activeReplica
   * @return true if the the reader has access
   * @throws edu.umass.cs.gnscommon.exceptions.server.FailedDBOperationException
   */
  public static boolean verifyAccess(MetaDataTypeName access, String guid, String field,
          String accessorGuid, GNSApplicationInterface<String> activeReplica) throws FailedDBOperationException {
    //String accessorGuid = ClientUtils.createGuidStringFromBase64PublicKey(accessorPublicKey);
    LOG.log(Level.FINE,
            "User: {0} Reader: {1} Field: {2}",
            new Object[]{guid, accessorGuid, field});
    if (guid.equals(accessorGuid)) {
      return true; // can always read your own stuff
    } else if (hierarchicalAccessCheck(access, guid, field, accessorGuid, activeReplica)) {
      return true; // accessor can see this field
    } else if (checkForAccess(access, guid, ALL_FIELDS, accessorGuid, activeReplica)) {
      return true; // accessor can see all fields
    } else {
      LOG.log(Level.FINE,
              "User {0} NOT allowed to access user {1}'s field {2}",
              new Object[]{accessorGuid, guid, field});
      return false;
    }
  }

  /**
   * Handles checking of fields with dot notation.
   * Checks deepest field first then backs up.
   *
   * @param access
   * @param guidInfo
   * @param field
   * @param accessorInfo
   * @param accessorPublicKey
   * @return true if the accessor has access
   * @throws FailedDBOperationException
   */
  private static boolean hierarchicalAccessCheck(MetaDataTypeName access, String guid,
          String field, String accessorGuid,
          GNSApplicationInterface<String> activeReplica) throws FailedDBOperationException {
    LOG.log(Level.FINE, "###field={0}", field);
    if (checkForAccess(access, guid, field, accessorGuid, activeReplica)) {
      return true;
    }
    // otherwise go up the hierarchy and check
    if (field.contains(".")) {
      return hierarchicalAccessCheck(access, guid, field.substring(0, field.lastIndexOf(".")),
              accessorGuid, activeReplica);
    } else {
      return false;
    }
  }

  private static boolean checkForAccess(MetaDataTypeName access, String guid, String field, String accessorGuid,
          GNSApplicationInterface<String> activeReplica) throws FailedDBOperationException {
    try {
      // FIXME: Tidy this mess up.
      @SuppressWarnings("unchecked")
      Set<String> allowedusers = (Set<String>) (Set<?>) NSFieldMetaData.lookupOnThisNameServer(access,
              guid, field, activeReplica.getDB());
      LOG.log(Level.FINE, "{0} allowed users of {1} : {2}", new Object[]{guid, field, allowedusers});
      if (checkAllowedUsers(accessorGuid, allowedusers, activeReplica)) {
        LOG.log(Level.FINE, "User {0} allowed to access {1}",
                new Object[]{accessorGuid,
                  field != ALL_FIELDS ? ("user " + guid + "'s " + field + " field")
                          : ("all of user " + guid + "'s fields")});
        return true;
      }
      return false;
    } catch (FieldNotFoundException e) {
      // This is actually a normal result.. so no warning here.
      return false;
    } catch (RecordNotFoundException e) {
      LOG.log(Level.WARNING,
              "User {0} access problem for {1}'s {2} field: {3}",
              new Object[]{accessorGuid, guid, field, e});
      return false;
    }
  }

  private static boolean checkAllowedUsers(String accessorGuid,
          Set<String> allowedUsers, GNSApplicationInterface<String> activeReplica) throws FailedDBOperationException {
    if (ClientUtils.publicKeyListContainsGuid(accessorGuid, allowedUsers)) {
      //if (allowedUsers.contains(accessorPublicKey)) {
      return true;
    } else if (allowedUsers.contains(EVERYONE)) {
      return true;
    } else {
      // see if allowed users (the public keys for the guids and group guids that is in the ACL) 
      // intersects with the groups that this
      // guid is a member of (which is stored with this guid)
      LOG.log(Level.FINE,
              "Looking up groups for {0} and check against {1}",
              new Object[]{accessorGuid, ClientUtils.convertPublicKeysToGuids(allowedUsers)});
      return !Sets.intersection(ClientUtils.convertPublicKeysToGuids(allowedUsers),
              NSGroupAccess.lookupGroups(accessorGuid, activeReplica.getRequestHandler())).isEmpty();
    }
  }

  /**
   * Returns true if the field has access setting that allow it to be read globally.
   *
   * @param access
   * @param guid
   * @param field
   * @param activeReplica
   * @return true if the field can be accessed
   * @throws FailedDBOperationException
   */
  public static boolean fieldAccessibleByEveryone(MetaDataTypeName access, String guid, String field,
          GNSApplicationInterface<String> activeReplica) throws FailedDBOperationException {
    try {
      return NSFieldMetaData.lookupOnThisNameServer(access, guid, field, activeReplica.getDB()).contains(EVERYONE)
              || NSFieldMetaData.lookupOnThisNameServer(access, guid, ALL_FIELDS, activeReplica.getDB()).contains(EVERYONE);
    } catch (FieldNotFoundException e) {
      // This is actually a normal result.. so no warning here.
      return false;
    } catch (RecordNotFoundException e) {
      LOG.log(Level.WARNING,
              "User {0} access problem for {1}'s {2} field: {3}",
              new Object[]{guid, field, access.toString(), e});
      return false;
    }
  }

  /**
   * Looks up the public key for a guid using the acl of a field.
   *
   * @param access
   * @param guid
   * @param field
   * @param database
   * @return a set of public keys
   * @throws FailedDBOperationException
   */
  @SuppressWarnings("unchecked")
  public static Set<String> lookupPublicKeysFromAcl(MetaDataTypeName access, String guid, String field,
          BasicRecordMap database) throws FailedDBOperationException {
    LOG.log(Level.FINE, "###field={0}",
            new Object[]{field});
    try {
      //FIXME: Clean this mess up.
      return (Set<String>) (Set<?>) NSFieldMetaData.lookupOnThisNameServer(access, guid, field, database);
    } catch (FieldNotFoundException e) {
      // do nothing
    } catch (RecordNotFoundException e) {
      LOG.log(Level.WARNING, "User {0} access problem for {1}'s {2} field: {3}",
              new Object[]{guid, field, access.toString(), e});
      return new HashSet<>();
    }
    // otherwise go up the hierarchy and check
    if (field.contains(".")) {
      return lookupPublicKeysFromAcl(access, guid, field.substring(0, field.lastIndexOf(".")), database);
    } else {
      return new HashSet<>();
    }
  }

  /**
   * Extracts out the message string without the signature part.
   *
   * @param messageStringWithSignatureParts
   * @param signatureParts
   * @return
   */
  public static String removeSignature(String messageStringWithSignatureParts, String signatureParts) {
    LOG.log(Level.FINER, "fullstring = {0} fullSignatureField = {1}", new Object[]{messageStringWithSignatureParts, signatureParts});
    String result = messageStringWithSignatureParts.substring(0, messageStringWithSignatureParts.lastIndexOf(signatureParts));
    LOG.log(Level.FINER, "result = {0}", result);
    return result;
  }

}