# jenkins-cookbook-pipeline

A multi-purpose repo for testing and publishing cookbooks using Jenkins Docker containers

### ./jenkins-master

A Dockerfile and shell script to get everything going

### TODO ./jenkins-builder

need to bring Dockerfile from chef-infra-base

### ./Dockerfile & ./var

The library to be included in a child cookbook repo


## Usage

Example of the calling [Jenkinsfile](https://github.com/mtyler/chef-infra-base/blob/master/Jenkinsfile)

## More Jenkins Cookbook Libraries
This library by James Massardo is a great, and provides more functionality.  Especially, if you are coordinating between OS & Apps teams and multiple BUs.
[https://github.com/jmassardo/Chef-Jenkins-Library](https://github.com/jmassardo/Chef-Jenkins-Library)
