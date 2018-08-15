FROM jenkinsci/blueocean
# ARG values can be overridden during docker build
# CHEF_SERVER_ADD_HOST should be the argument passed to --add-host
# `docker run --add-host $CHEF_SERVER_ADD_HOST`
ARG JENKINS_HOME=/var/jenkins_home
ARG KNIFE_RB=knife.rb
ARG CLIENT_KEY=client.pem
ARG CHEF_SERVER_ADD_HOST=chef-server.test:192.168.33.200
# this value needs to persist because it will be used on Jenkins build container
ENV CHEF_SERVER_ADD_HOST=$CHEF_SERVER_ADD_HOST

# plugins list was created from a basic install of blueocean
COPY plugins.txt $JENKINS_HOME/
RUN /usr/local/bin/install-plugins.sh < $JENKINS_HOME/plugins.txt
# initialization script to setup users and project
COPY basic-setup.groovy $JENKINS_HOME/init.groovy.d/
# files that jenkins expects in order to disable setupWizard
COPY jenkins.install.InstallUtil.lastExecVersion $JENKINS_HOME/
COPY jenkins.install.UpgradeWizard.state $JENKINS_HOME/
COPY jenkins.model.JenkinsLocationConfiguration.xml $JENKINS_HOME/
# copy knife and client configuration used by the pipline to talk to chef-server
# then set environment variables used in jenkins build container
RUN mkdir -p $JENKINS_HOME/.chef
COPY $KNIFE_RB $JENKINS_HOME/.chef/$KNIFE_RB
COPY $CLIENT_KEY $JENKINS_HOME/.chef/$CLIENT_KEY
ENV KNIFE_RB=$JENKINS_HOME/.chef/$KNIFE_RB
ENV KNIFE_RB_FILE=$KNIFE_RB
ENV CLIENT_KEY=$JENKINS_HOME/.chef/$CLIENT_KEY
ENV CLIENT_KEY_FILE=$CLIENT_KEY
