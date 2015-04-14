#!/bin/bash

cd "$(dirname "$0")"

source config.sh

rsync -azP --delete ../webpage "$host:$deployPath"
ssh "$host" chown -R :nginx "$deployPath"webpage
