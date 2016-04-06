# Mulong Luo
# muluo@ucsd.edu
#

FROM ubuntu:14.04
MAINTAINER Mulong Luo <muluo@ucsd.edu>
#RUN apt-get update && apt-get install -y ruby ruby-dev
#RUN gem install sinatra
#VOLUME ["/data"]
#ADD string.txt /data/
ADD catclient.cpp /home/
RUN apt-get update && apt-get install g++ -y
RUN g++ /home/catclient.cpp -o /home/catclient
#RUN /home/catclient
#RUN apt-get update && apt-get install g++
