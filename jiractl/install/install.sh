#!/bin/bash
sudo cp -t /etc/init jiractl.conf

if [ ! -d /var/log/jiractl ] ; then
  sudo mkdir -p /var/log/jiractl
fi

echo "Do you want to install the jiractl-mongodb-upstart-script? [Y|n]"
read decision
if [ $decision = "n" ] || [ $decision = "N" ] ; then
  echo "Done."
else
  sudo cp -t /etc/init mongodb.conf
fi
echo "Please cd to your jiractl-Directory and run 'npm install' (you might need to specify the Path to npm)"
