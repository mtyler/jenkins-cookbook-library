def CLIENT_KEY = 'nokey'
def KNIFE_RB = 'nokniferb'
def COOKBOOK_NAME = 'noname'
def COOKBOOK_DIR = 'nodir'
def COOKBOOK_ARTIFACT = 'noartifact'

def call(String chefServerAddHost, String cookbookName, String clientKeyFile, String knifeRbFile){
// This pipeline assumes that human approval happens through a PR
// to master.
  pipeline {
  environment {
    // client and knife are setup this way because docker inside docker
    // this is currently closely tied to the jenkinsci/blueocean container
    // docker run should include -v [directory with files]:/root/.altchef
    CLIENT_KEY= sh (
      script: 'cat ${clientKeyFile}',
      returnStdout: true
    ).trim()
    KNIFE_RB= sh (
      script: 'cat ${knifeRbFile}',
      returnStdout: true
    ).trim()
    // cookbook name is derived from metadata.rb and would need mod to include
    // metadata.json
//    COOKBOOK_NAME= sh (
//      script: "sed -n -e '/^name/p' metadata.rb | sed 's/^name*//g;s/^[[:space:]]*//g' | sed \"s/'//g\"",
//      returnStdout: true
//    ).trim()
    COOKBOOK_NAME= ${cookbookName}
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
      filename 'Dockerfile'
      args "--net host \
            -u 0:0 \
            --dns 192.168.1.1 \
            --add-host ${chefServerAddHost} \
            -v /var/run/docker.sock:/var/run/docker.sock"

    }
  }
//  options {
//    newContainerPerStage()
//    checkoutToSubdirectory('my-workspace')
//  }
  triggers {
    pollSCM('H */4 * * 1-5')
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
        sh """
          mkdir -p /root/.chef
          printf '%s\n' '${env.CLIENT_KEY}' > /root/.chef/cicdsvc.pem
          printf '%s\n' '${env.KNIFE_RB}' > /root/.chef/knife.rb
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
  }
}
