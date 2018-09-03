#!/bin/bash

set -o nounset
set -o errexit

readonly installDir="/opt/redis"
readonly redisDir="/opt/redis/redis_src"
redis_version=redis-4.0.9.tar.gz

log(){
	msg=$1
	time=`date +"+%Y-%m-%d %T"`
	echo "$time -- $msg"	
}


# create default dirs 
init() {
	# create working dirs and a tmp dir
	mkdir -p ${installDir}/data
	mkdir -p ${installDir}/conf
	mkdir -p ${installDir}/logs			
	
	log "OK: init dir done"
}



# install redis 
installRedis() {
	#which redis-server
	#if [[ $? == 0 ]]; then
	#	echo "WARN: redis is already installed, exit."
	#	return
	#fi

	#yum install -y gcc
	mkdir -p ${redisDir} && cd ${redisDir}
	wget http://download.redis.io/releases/$redis_version && mv $redis_version redis.tar.gz && tar zxvf redis.tar.gz --strip-component=1
	make && make PREFIX=${installDir} install > redis_install.log 2>&1
	if [[ $? == 0 ]]; then
		rm -rf ${redisDir}
		log "OK: redis is installed, exit."			
		echo "export PATH=$PATH:/opt/redis/bin" >> ~/.bashrc		
		return		
	fi
	log "ERROR: redis is NOT installed, exit."
}

main()
{
	init
	installRedis
	rm -f ~/cachecloud-init.sh
}

main 