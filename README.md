cpauth
======

Authentication service for ICOS Carbon Portal and SITES Data Portal.

Works as a SAML2 Service Provider.

Acts as a single sign on system for all the services provided by the ICOS Carbon Portal (http://icos-cp.eu)

Hosted on https://cpauth.icos-cp.eu/ and on https://auth.fieldsites.se/

SAML metadata: /saml/cpauth

## Useful openssl commands for developers

Generate a key pair for a cpauth domain (such as cpauth.icos-cp.eu or auth.fieldsites.se;
note that cityauth.icos-cp.eu shares authentication service with ICOS):
`$ openssl ecparam -name secp384r1 -genkey -out private.pem`

Extract the public key from the keypair:
`$ openssl ec -pubout -in private.pem -out public.pem`

The public key shall be copied to `core/src/main/resources/cpauthCore/crypto/`,
the names should be prefixed with cpauth domain, e.g. `cpauth_public.pem`.

Convert PEM private key to DER format:
`$ openssl ec -outform DER -in private.pem -out private.der`

Convert private key from PKCS#1 to PKCS#8:
`$ openssl pkcs8 -topk8 -inform DER -outform DER -in private.der -out private8.der -nocrypt`


Generate a certificate for SAML SP:
`$ openssl req -keyform DER -key private.der -new -x509 -days 3650 -out self_signed.crt`

The resulting contents of the certificate shall be pasted into the corresponding
`..._unsigned.xml` file in `src/main/resources`.

To produce SAML SP metadata, edit and run
`se.lu.nateko.cp.cpauth.xmldsig.SpSamlMetadataProducer.produceSignedMetadata()`
in Scala REPL (before that edit the corresponding `..._unsigned.xml` file in `src/main/resources`).

To inspect a public key:
`$ openssl ec -pubin -in public.pem -text`

To inspect an x509 certificate:
`$ openssl x509 -in self_signed.crt -text -noout`