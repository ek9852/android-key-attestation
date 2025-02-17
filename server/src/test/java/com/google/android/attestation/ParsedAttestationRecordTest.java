/* Copyright 2019, The Android Open Source Project, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.attestation;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.android.attestation.AuthorizationList.UserAuthType;
import com.google.android.attestation.ParsedAttestationRecord.SecurityLevel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.google.common.collect.ImmutableSet;
import org.bouncycastle.asn1.ASN1Sequence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link ParsedAttestationRecord}. */
@RunWith(JUnit4.class)
public class ParsedAttestationRecordTest {

  // Certificate generated by TestDPC with RSA Algorithm and StrongBox Security Level
  private static final String CERT =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIIGCDCCBHCgAwIBAgIBATANBgkqhkiG9w0BAQsFADApMRkwFwYDVQQFExAyZGM1OGIyZDFhMjQx"
          + "MzI2MQwwCgYDVQQMDANURUUwIBcNNzAwMTAxMDAwMDAwWhgPMjEwNjAyMDcwNjI4MTVaMB8xHTAb"
          + "BgNVBAMMFEFuZHJvaWQgS2V5c3RvcmUgS2V5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC"
          + "AQEApNVcnyN40MANMbbo2nMGNq2NNysDSjfLm0W3i6wPKf0ffCYkhWM4dCmQKKf50uAZTBeTit4c"
          + "NwXeZn3qellMlOsIN3Qc384rfN/8cikrRvUAgibz0Jy7STykjwa7x6tKwqITxbO8HqAhKo8/BQXU"
          + "xzrOdIg5ciy+UM7Vgh7a7ogen0KL2iGgrsalb1ti7Vlzb6vIJ4WzIC3TGD2sCkoPahghwqFDZZCo"
          + "/FzaLoNY0jAUX2mL+kf8aUaoxz7xA9FTvgara+1pLBR1s4c8xPS2HdZipcVXWfey0wujv1VAKs4+"
          + "tXjKlHkYBHBBceEjxUtEmrapSQEdpHPv7Xh9Uanq4QIDAQABo4ICwTCCAr0wDgYDVR0PAQH/BAQD"
          + "AgeAMIICqQYKKwYBBAHWeQIBEQSCApkwggKVAgEDCgEBAgEECgEBBANhYmMEADCCAc2/hT0IAgYB"
          + "ZOYGEYe/hUWCAbsEggG3MIIBszGCAYswDAQHYW5kcm9pZAIBHTAZBBRjb20uYW5kcm9pZC5rZXlj"
          + "aGFpbgIBHTAZBBRjb20uYW5kcm9pZC5zZXR0aW5ncwIBHTAZBBRjb20ucXRpLmRpYWdzZXJ2aWNl"
          + "cwIBHTAaBBVjb20uYW5kcm9pZC5keW5zeXN0ZW0CAR0wHQQYY29tLmFuZHJvaWQuaW5wdXRkZXZp"
          + "Y2VzAgEdMB8EGmNvbS5hbmRyb2lkLmxvY2FsdHJhbnNwb3J0AgEdMB8EGmNvbS5hbmRyb2lkLmxv"
          + "Y2F0aW9uLmZ1c2VkAgEdMB8EGmNvbS5hbmRyb2lkLnNlcnZlci50ZWxlY29tAgEdMCAEG2NvbS5h"
          + "bmRyb2lkLndhbGxwYXBlcmJhY2t1cAIBHTAhBBxjb20uZ29vZ2xlLlNTUmVzdGFydERldGVjdG9y"
          + "AgEdMCIEHWNvbS5nb29nbGUuYW5kcm9pZC5oaWRkZW5tZW51AgEBMCMEHmNvbS5hbmRyb2lkLnBy"
          + "b3ZpZGVycy5zZXR0aW5ncwIBHTEiBCAwGqPLCBE0UBxF8UIqvGbCQiT9Xe1f3I8X5pcXb9hmqjCB"
          + "rqEIMQYCAQICAQOiAwIBAaMEAgIIAKUFMQMCAQSmCDEGAgEDAgEFv4FIBQIDAQABv4N3AgUAv4U+"
          + "AwIBAL+FQEwwSgQgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQAKAQIEIHKNsSdP"
          + "HxzxVx3kOAsEilVKxKOA529TVQg1KQhKk3gBv4VBAwIBAL+FQgUCAwMUs7+FTgUCAwMUs7+FTwUC"
          + "AwMUszANBgkqhkiG9w0BAQsFAAOCAYEAJMIuzdNUdfrE6sIdmsnMn/scSG2odbphj8FkX9JGdF2S"
          + "OT599HuDY9qhvkru2Dza4sLKK3f4ViBhuR9lpfeprKvstxbtBO7jkLYfVn0ZRzHRHVEyiW5IVKh+"
          + "qOXVJ9S1lMShOTlsaYJytLKIlcrRAZBEXZiNbzTuVh1CH6X9Ni1dog14snm+lcOeORdL9fht2CHa"
          + "u/caRnpWiZbjoAoJp0O89uBrRkXPpln51+3jPY6AFny30grNAvKguauDcPPhNV1yR+ylSsQi2gm3"
          + "Rs4pgtlxFLMfZLgT0cbkl+9zk/QUqlpBP8ftUBsOI0ARr8xhFN3cvq9kXGLtJ9hEP9PRaflAFREk"
          + "DK3IBIbVcAFZBFoAQOdE9zy0+F5bQrznPGaZg4Dzhcx33qMDUTgHtWoy+k3ePGQMEtmoTTLgQywW"
          + "OIkXEoFqqGi9GKJXUT1KYi5NsigaYqu7FoN4Qsvs61pMUEfZSPP2AFwkA8uNFbmb9uxcxaGHCA8i"
          + "3i9VM6yOLIrP\n"
          + "-----END CERTIFICATE-----";
  private static final String CERT2 =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIIEFDCCAnygAwIBAgIVAKZFQPAXr5VWrosuqx4C8tai2XbHMA0GCSqGSIb3DQEB\n"
          + "CwUAMBgxFjAUBgNVBAMMDVVua25vd25Jc3N1ZXIwHhcNMjMwMjE1MTU0MzIwWhcN\n"
          + "MjMwMjE1MTU0ODIwWjAdMRswGQYDVQQDDBJBbmRyb2lkQXR0ZXN0ZWRLZXkwggGi\n"
          + "MA0GCSqGSIb3DQEBAQUAA4IBjwAwggGKAoIBgQDJAVfP/7F1bUbDqxMnOVXpSjt5\n"
          + "NJwYemBJkN7l7TTbAhTfMW91006Si/snd79Y6bsJklVoiEN9LGL7tQrJEf5lSSLX\n"
          + "ZeppjsbLqKnogFHhDJy2vaSiypV2wZdX+kO0qqIKjRvgSqHuTz3gemI1rWilrG3C\n"
          + "vd3iHGlkw/4X5PpHQKz99/20p85HP6f/jydMHewFDRQCbkbo2pJ5WrJsyPe9me3o\n"
          + "QE0O3lgij7jJ/UBHyb9iH0w13yi+1yZ/jgyojL4QNUeWZnxW656zfHCB8weePD+l\n"
          + "tX4AAztZTziJQwk3zVClw4xIPTeztQV6ddRQgjSjGvWanpXqhJx8mq11gWaVJoCl\n"
          + "q/I0KOguVsKq42M25uhF7/iAQjC+6lOUUfi2+aPwyTUfGHc5Bw/rTSw2LzvZDnUW\n"
          + "8/yw4OUTyDravVcQLeoBES4+O5cVL0yTKDY0THG+ymgsFNgFS7PXUnAbXczYzvg8\n"
          + "ldXKOXxnF5nWgg55n2iSQ6mqtHDEUsjcxjmuFcMCAwEAAaNQME4wTAYKKwYBBAHW\n"
          + "eQIBEQQ+MDwCAQEKAQECAQIKAQEEEkEgcmFuZG9tIGNoYWxsZW5nZQQAMAAwFr+F\n"
          + "SQgEBlNFUklBTL+FSgYEBElNRUkwDQYJKoZIhvcNAQELBQADggGBAHSms4IBjkc8\n"
          + "1ZLHu5l70Ih2RrNU4XAc2E/oJX8OsBte9ZRwDT3TdcfLeg0rSneS+aB4xN1BGfmL\n"
          + "DPZ1epRzMY4RagVhzBEauHpTaM2imRT9RN5TxbFvuMC4ELICYr5qHfqeALIlMET3\n"
          + "TbCAo3njpNh5ids6qdlmpZRoYBQNMKfWJn8SUtCmVMk87FA7RZZCqCiRk+PBnciT\n"
          + "O3LLbwT4aBlMinQ84gBfVXRqOvGAeGOgojDqGyK3tDMjIS7itpGb23vGogxHiHjA\n"
          + "i8hiQhsHA+C89duCdeGyWZGmxwln7QRsosFI7G4ZOufXPLZt/DauNAC2Mb2OPcDw\n"
          + "4tSKQvzQiL9UG4X3Cck0JnATxjT5sLttshJl98V6jQHcWSnjg8+oa3B8WgcePX8E\n"
          + "QgcLhYaEGo9WDYJQvHfuUE5AquTxdTRbeiDbV7W+FAOQ5zi/wiGit86gF26120OQ\n"
          + "KzQHP94/ORuAT/lkv3Fp3HytF4n3scur1nI0WqrfKpbUuPkmndCIbg==\n"
          + "-----END CERTIFICATE-----\n";

  private static final int EXPECTED_ATTESTATION_VERSION = 3;
  private static final SecurityLevel EXPECTED_ATTESTATION_SECURITY_LEVEL =
      SecurityLevel.TRUSTED_ENVIRONMENT;
  private static final int EXPECTED_KEYMASTER_VERSION = 4;
  private static final SecurityLevel EXPECTED_KEYMASTER_SECURITY_LEVEL =
      SecurityLevel.TRUSTED_ENVIRONMENT;
  private static final byte[] EXPECTED_ATTESTATION_CHALLENGE = "abc".getBytes(UTF_8);
  private static final byte[] EXPECTED_UNIQUE_ID = "".getBytes(UTF_8);

  private static X509Certificate getAttestationRecord(String certStr) throws CertificateException {
    CertificateFactory factory = CertificateFactory.getInstance("X509");
    X509Certificate cert =
        (X509Certificate)
            factory.generateCertificate(new ByteArrayInputStream(certStr.getBytes(UTF_8)));
    return cert;
  }

  @Test
  public void testParseAttestationRecord() throws CertificateException, IOException {
    X509Certificate x509Certificate = getAttestationRecord(CERT);
    X509Certificate x509Certificate2 = getAttestationRecord(CERT2);
    ParsedAttestationRecord attestationRecord =
        ParsedAttestationRecord.createParsedAttestationRecord(new X509Certificate[] {x509Certificate2, x509Certificate});

    assertThat(attestationRecord.attestationVersion).isEqualTo(EXPECTED_ATTESTATION_VERSION);
    assertThat(attestationRecord.attestationSecurityLevel)
        .isEqualTo(EXPECTED_ATTESTATION_SECURITY_LEVEL);
    assertThat(attestationRecord.keymasterVersion).isEqualTo(EXPECTED_KEYMASTER_VERSION);
    assertThat(attestationRecord.keymasterSecurityLevel)
        .isEqualTo(EXPECTED_KEYMASTER_SECURITY_LEVEL);
    assertThat(attestationRecord.attestationChallenge).isEqualTo(EXPECTED_ATTESTATION_CHALLENGE);
    assertThat(attestationRecord.uniqueId).isEqualTo(EXPECTED_UNIQUE_ID);
    assertThat(attestationRecord.softwareEnforced).isNotNull();
    assertThat(attestationRecord.teeEnforced).isNotNull();
  }

  @Test
  public void testCreateAndParseAttestationRecord() {
    AuthorizationList.Builder teeEnforcedBuilder = AuthorizationList.builder();
    teeEnforcedBuilder.userAuthType = ImmutableSet.of(UserAuthType.FINGERPRINT);
    teeEnforcedBuilder.attestationIdBrand = "free food".getBytes(UTF_8);
    ParsedAttestationRecord expected =
        ParsedAttestationRecord.create(
            /* attestationVersion= */ 2,
            /* attestationSecurityLevel= */ SecurityLevel.TRUSTED_ENVIRONMENT,
            /* keymasterVersion= */ 4,
            /* keymasterSecurityLevel= */ SecurityLevel.SOFTWARE,
            /* attestationChallenge= */ "abc".getBytes(UTF_8),
            /* uniqueId= */ "foodplease".getBytes(UTF_8),
            /* softwareEnforced= */ AuthorizationList.builder().build(),
            /* teeEnforced= */ AuthorizationList.builder()
                .setUserAuthType(ImmutableSet.of(UserAuthType.FINGERPRINT))
                .setAttestationIdBrand("free food".getBytes(UTF_8)).build());
    ASN1Sequence seq = expected.toAsn1Sequence();
    ParsedAttestationRecord actual = ParsedAttestationRecord.create(seq);
    assertThat(actual.attestationVersion).isEqualTo(expected.attestationVersion);
    assertThat(actual.attestationSecurityLevel).isEqualTo(expected.attestationSecurityLevel);
    assertThat(actual.keymasterVersion).isEqualTo(expected.keymasterVersion);
    assertThat(actual.keymasterSecurityLevel).isEqualTo(expected.keymasterSecurityLevel);
    assertThat(actual.attestationChallenge).isEqualTo(expected.attestationChallenge);
    assertThat(actual.uniqueId).isEqualTo(expected.uniqueId);
    assertThat(actual.teeEnforced.userAuthType).isEqualTo(expected.teeEnforced.userAuthType);
    assertThat(actual.teeEnforced.attestationIdBrand)
        .isEqualTo(expected.teeEnforced.attestationIdBrand);
  }
}
