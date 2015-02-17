#!/bin/bash

cd "$(dirname "$0")"

#wget http://md.swamid.se/md/swamid-idp-transitive.xml -O ../src/main/resources/swamid-idps.xml
wget http://md.swamid.se/md/swamid-idp.xml -O ../src/main/resources/swamid-idps.xml
