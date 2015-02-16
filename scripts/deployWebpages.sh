#!/bin/bash

cd "$(dirname "$0")"

scp -r ../webpage "$1":/disk/data/cpauth/
