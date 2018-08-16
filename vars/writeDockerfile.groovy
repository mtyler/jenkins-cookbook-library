#!groovy

def call(){

  def file = new File('Dockerfile')
  file.text = '''
FROM evarga/jenkins-slave
ARG CHANNEL=stable
ARG CHEFDK_V=2.3.3
ARG VAGRANT_V=2.1.1
ARG USER=jenkins
ARG USER_ID=1000
ARG GROUP_ID=1000
# set some environment vars with sane defaults
ENV DEBIAN_FRONTEND=noninteractive \
    PATH=/opt/chefdk/bin:/opt/chefdk/embedded/bin:/root/.chefdk/gem/ruby/2.4.0/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
# prereqs
RUN apt-get update && \
    apt-get install -y apt-transport-https ca-certificates curl software-properties-common wget ssh
# install docker
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - && \
    add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" && \
    apt-get update && \
    apt-get install -y docker-ce
# install vagrant
RUN wget --quiet --content-disposition "https://releases.hashicorp.com/vagrant/${VAGRANT_V}/vagrant_${VAGRANT_V}_x86_64.deb" -O /tmp/vagrant.deb && \
    dpkg -i /tmp/vagrant.deb
# install chefdk
RUN wget --quiet --content-disposition "http://packages.chef.io/files/${CHANNEL}/chefdk/${CHEFDK_V}/ubuntu/16.04/chefdk_${CHEFDK_V}-1_amd64.deb" -O /tmp/chefdk.deb && \
    dpkg -i /tmp/chefdk.deb && \
    chef gem install kitchen-docker && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    groupmod -g ${GROUP_ID} jenkins && \
    usermod -u ${USER_ID} -g ${GROUP_ID} ${USER} && \
    adduser ${USER} docker
USER jenkins
VOLUME /var/run/docker.sock
'''

}
