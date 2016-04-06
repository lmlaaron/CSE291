sudo docker build -t lmlaaron/catserver .
sudo docker run -d --name catserver --volumes-from store lmlaaron/catserver /home/catserver /data/string.txt 2222 
 
