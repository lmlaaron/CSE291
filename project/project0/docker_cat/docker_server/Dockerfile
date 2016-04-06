# Mulong Luo
# muluo@ucsd.edu
#

FROM ubuntu:14.04
MAINTAINER Mulong Luo <muluo@ucsd.edu>
#RUN apt-get update && apt-get install -y ruby ruby-dev
#RUN gem install sinatra
#$VOLUME ["/data"]
#ADD string.txt /data/
ADD catserver.cpp /home/
RUN apt-get update && apt-get install -y g++
RUN g++ /home/catserver.cpp -o /home/catserver
RUN ifconfig
#RUN /home/catserver /data/string.txt 2222 
