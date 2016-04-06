sudo docker build -t lmlaaron/catclient .
#sudo docker run -i -t --volumes-from lml_dbstore lmlaaron/catclient /bin/bash
sudo docker run -P --volumes-from store --name catclient_ok lmlaaron/catclient /home/catclient /data/string.txt "$(sudo docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' catserver)" 2222
sudo docker logs catclient_ok
sudo docker run -P --volumes-from store --name catclient_missing lmlaaron/catclient /home/catclient /data/string1.txt "$(sudo docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' catserver)" 2222
sudo docker logs catclient_missing



