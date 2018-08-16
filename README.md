# jenkins-cookbook-pipeline

A proof-of-concept, multi-purpose repo for testing and publishing cookbooks using Jenkins Docker containers.

In it's current form, this is best suited for creating a quick and dirty, on-the-fly, cookbook pipeline.  There are many things that would need to be addressed before porting any of this functionality to a production environment.

## Known Issues

- The setup_jenkins.sh file is heavily dependent on very specific environment variables.
- The first run fails because something is going on with the path used to copy the libraryResource and the workspace where the initial indexing occurs  

## Jenkins Master setup files

- ./jenkins-master/*

### Usage

1. Add files to the jenkins-master directory. This is the Jenkins Master Docker build context and the following files are used to build the container
  - jenkins-master/cicdsvc-knife.rb
    A knife configuration file with the appropriate configuration for your environment
  - jenkins-master/cicdsvc.pem
    A client.pem key for the user specified in the knife.rb file
  - jenkins-master/github-token
    A [Github Access Token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/) with repo and user email access to the cookbook repo

2. Run ./jenkins-master/setup_jenkins.sh
  - Edit setup_jenkins.sh and set Environment variables appropriately

## Jenkins Library

This will run the bare bones basics of a cookbook pipeline.
- Unit tests with chefspec
- Linting with foodcritic and cookstyle
- Functional tests with TestKitchen
- Publishing to a Chef Server

### Library files

- ./vars/publishMaster.groovy
- ./resources/Dockerfile (Jenkins Builder)
  A Dockerfile for running a cookbook builder inside the jenkinsci/blueocean Jenkins container

### Example Usage

Example of the calling [Jenkinsfile](https://github.com/mtyler/chef-infra-base/blob/master/Jenkinsfile)

## More Jenkins Cookbook Libraries

This library by James Massardo is more robust and provides more functionality.  Especially, if you are coordinating between OS & Apps teams and multiple BUs.
[https://github.com/jmassardo/Chef-Jenkins-Library](https://github.com/jmassardo/Chef-Jenkins-Library)
