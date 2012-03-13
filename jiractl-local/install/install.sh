#!/bin/bash
sudo cp -t /etc/init jiractl-local.conf

if [ ! -d /var/log/jiractl ] ; then
  sudo mkdir -p /var/log/jiractl
fi
echo "Please cd to your jiractl-Directory and run 'npm install' (you might need to specify the Path to npm)"
