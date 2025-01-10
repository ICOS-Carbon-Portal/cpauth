cpauth
======

Authentication service for ICOS Carbon Portal and SITES Data Portal.

Works as a SAML2 Service Provider.

Acts as a single sign on system for all the services provided by the ICOS Carbon Portal (http://icos-cp.eu)

Hosted on https://cpauth.icos-cp.eu/ and on https://auth.fieldsites.se/

SAML metadata: /saml/cpauth

## Useful openssl commands for developers

Generate a key pair for a cpauth domain (such as icos-cp.eu och fieldsites.se):
`$ openssl ecparam -name secp384r1 -genkey -out cpauth_private.pem`

Extract the public key from the keypair:
`$ openssl ec -pubout -in private_key.pem -out public_key.pem`

Convert PEM private key to DER format:
`$ openssl ec -outform DER -in private.pem -out private.der`

Convert private key from PKCS#1 to PKCS#8:
`$ openssl pkcs8 -topk8 -inform DER -outform DER -in private.der -out private8.der -nocrypt`

Convert PEM public key to DER format:
`$ openssl ec -pubin -pubout -outform DER -in public.pem -out public.der`

Generate a certificate for SAML SP:
`$ openssl req -keyform DER -key private.der -new -x509 -days 3650 -out self_signed.crt`

Inspect a public key:
`$ openssl ec -pubin -in public.pem -text`

Inspect an x509 certificate:
`$ openssl x509 -in self_signed.crt -text -noout`


To produce SAML SP metadata, edit and run
`se.lu.nateko.cp.cpauth.xmldsig.SpSamlMetadataProducer.produceSignedMetadata()`
in Scala REPL.