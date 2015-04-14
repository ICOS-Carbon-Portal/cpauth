#!/bin/bash

cd "$(dirname "$0")"

source config.sh

ssh "$host" "$deployPath"stop.sh
rsync -azP --delete ../db "$host:$deployPath"
ssh "$host" chown -R :nginx "$deployPath"db
ssh "$host" "$deployPath"start.sh

