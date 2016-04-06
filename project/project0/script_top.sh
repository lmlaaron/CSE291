
# create docker with app installed 
cd docker_w_app
bash script.sh
docker ps -a
cd ..

# create volume containter
cd docker_volume
bash script.sh
cd ..

# create cat server and client
cd docker_cat
#sudo docker network create -d bridge mynetwork
cd docker_server
bash script.sh

cd ..
cd docker_client
bash script.sh


#clean up
sudo docker stop catserver
sudo docker rm catserver

