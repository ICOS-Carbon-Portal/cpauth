#!/bin/bash

cd "$(dirname "$0")"

host="fsicos.lunarc.lu.se"
deployPath="/disk/data/cpauth/"

if [[ -n "$1" ]]; then host="$1@$host"; fi # prepending user name if specified

./deployWebpages.sh "$host"

scp ./restart.sh "$host:$deployPath"

scp ../target/scala-2.11/Carbon\ Portal\ Authentication\ Service-assembly-0.1.jar "$host:$deployPath"assembly.jar

ssh "$host" "$deployPath"restart.sh
