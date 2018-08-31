def COOKBOOK_NAME = 'noname'
def COOKBOOK_DIR = 'nodir'
def COOKBOOK_ARTIFACT = 'noartifact'

def call(String cookbookName){
  // It's not clear where jenkins is getting the @2 suffix from.  Additionally,
  // this appears to fail on the first pass
  writeFile file: "${WORKSPACE}@2/Dockerfile", text: libraryResource('Dockerfile')
  // Now we're really going into hacked up territory
  // TODO figure out how to work with Workspace and this mystical @2 suffix
  writeFile file: "${WORKSPACE}/Dockerfile", text: libraryResource('Dockerfile')

// This pipeline assumes that human approval happens through a PR
// to master.
//
// This requires the environment variables $KNIFE_RB, $CLIENT_KEY and $CHEF_SERVER_ADD_HOST
// be set on the jenkinsci/blueocean container or the derived container
// KNIFE_RB is the knife.rb configuration typically found in ~/.chef
// CLIENT_KEY is the client.pem typically found in ~/.chef
// CHEF_SERVER_ADD_HOST is the etc/hosts file entry formatted for the docker --add-host arg
  pipeline {
    environment {
      // client and knife are setup this way because docker inside docker
      // this is currently closely tied to the jenkinsci/blueocean container
      // docker build should copy the files to the container and set the vars
      client_key= sh (
        script: "cat ${CLIENT_KEY}",
        returnStdout: true
      ).trim()
      knife_rb= sh (
        script: "cat ${KNIFE_RB}",
        returnStdout: true
      ).trim()
      COOKBOOK_NAME= cookbookName.trim()
      COOKBOOK_DIR= sh (
        script: "printf '%s' \"cookbooks/${COOKBOOK_NAME}\"",
        returnStdout: true
      ).trim()
      COOKBOOK_ARTIFACT= sh (
        script: "printf '%s' \"${COOKBOOK_NAME}-${BRANCH_NAME}.tar\"",
        returnStdout: true
      ).trim()
    }
    agent {
      dockerfile {
        //filename is relative to $WORKSPACE + whatever suffix is used
        filename 'Dockerfile'
        args "--net host \
              -u 0:0 \
              --add-host \"\$CHEF_SERVER_ADD_HOST\" \
              -v /var/run/docker.sock:/var/run/docker.sock"
      }
    }
    triggers {
      // poll scm every 2m
      cron('H/2 * * * *')
    }
    stages {
      stage('Prep') {
        steps {
          sh 'printenv'
        }
      }
      stage('Chef') {
        steps {
          parallel(
            "Chef Cookbook Unit": {
              sh '''
                chef exec 'rspec --format progress --format RspecJunitFormatter --out rspec_junit.xml'
              '''
            },
            "Chef Cookbook Lint": {
              sh "chef exec 'foodcritic .'"
              sh "chef exec 'cookstyle . --format progress'"
            }
            ,
            "Chef Cookbook Functional": {
              sh '''
                export KITCHEN_LOCAL_YAML='.kitchen.docker.yml'
                chef exec 'kitchen test --concurrency=5 --destroy=always'
              '''
            }
          )
        }
      }
      stage('Create Artifact') {
        steps {
          //clean up any potential previous runs and copy current cookbook
          sh '''
            rm -rf ${COOKBOOK_DIR}
            mkdir -p ${COOKBOOK_DIR}
            for i in `ls -A1 | grep -v '^.kitchen\$' | grep -v '^cookbooks\$' | grep -v '^.git\$' | grep -v '^.gitignore\$'`; do
              cp -R \$i ${COOKBOOK_DIR}
            done
            tar cvf ${COOKBOOK_ARTIFACT} ${COOKBOOK_DIR}
          '''
        }
      }
      stage('Publish Cookbook') {
        when {
          branch 'master'
        }
        steps {
          //configure knife and publish
          // TODO cicdsvc.pem is dependant upon knife.rb values...
          //      this needs to be resolved cleanly, through add'l vars/logic
          sh """
            mkdir -p /root/.chef
            printf '%s\n' '${env.client_key}' > /root/.chef/cicdsvc.pem
            printf '%s\n' '${env.knife_rb}' > /root/.chef/knife.rb
            knife ssl fetch -c /root/.chef/knife.rb
            knife cookbook upload ${env.COOKBOOK_NAME} --cookbook_path ./cookbooks -c /root/.chef/knife.rb --freeze
          """
        }
        post {
          always {
            archiveArtifacts artifacts: "${env.COOKBOOK_ARTIFACT}", fingerprint: true
            junit('rspec_junit.xml')
            //clean up
            deleteDir()
          }
        }
      }
    }
  } //pipeline
}
