# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version
VAGRANTFILE_API_VERSION = "2"

# Override host locale variable
ENV["LC_ALL"] = "en_US.UTF-8"

$script = <<-'SCRIPT'
    GRADLE_DIR=/home/vagrant/.gradle
    mkdir -p $GRADLE_DIR
    echo "renjinHomeDir=/home/ubuntu/renjin-release/renjin/tools/gnur-installation/src/main/resources" > $GRADLE_DIR/gradle.properties
    echo "gccBridgePlugin=/home/ubuntu/renjin-release/libstdc++/build/bridge.so" >> $GRADLE_DIR/gradle.properties
    chown vagrant $GRADLE_DIR
    chown vagrant $GRADLE_DIR/gradle.properties
SCRIPT

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

  config.vm.box = "ubuntu/xenial64"
  config.vm.provision :shell, inline: "apt-get update && apt-get install openjdk-8-jdk maven make gcc-4.7 gcc-4.7-plugin-dev gfortran-4.7 g++-4.7 gcc-4.7.multilib g++-4.7-multilib unzip libz-dev -y"
  config.vm.provision "shell", inline: $script
  config.vm.synced_folder ".", "/home/ubuntu/renjin-release"
  config.vm.synced_folder "~/.m2", "/home/vagrant/.m2"

  config.vm.provider "virtualbox" do |v|
    v.memory = 9216
    v.cpus = 4
  end
end
