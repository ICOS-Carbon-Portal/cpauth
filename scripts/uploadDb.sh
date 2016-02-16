#!/bin/bash

echo 'Be careful with this script! Exiting for now.'
exit 0

cd "$(dirname "$0")"

source config.sh

ssh "$host" "$deployPath"stop.sh
rsync -azP --delete ../db "$host:$deployPath"
ssh "$host" chown -R :nginx "$deployPath"db
ssh "$host" "$deployPath"start.sh

