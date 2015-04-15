#!/bin/bash

cd "$(dirname "$0")"

source config.sh

./deployWebpages.sh "$1"

scp stop.sh start.sh restart.sh "$host:$deployPath"

rsync -aP ../target/scala-2.11/Carbon\ Portal\ Authentication\ Service-assembly-0.1.jar "$host:$deployPath"assembly.jar

ssh "$host" "$deployPath"restart.sh
