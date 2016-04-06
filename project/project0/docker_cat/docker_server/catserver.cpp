#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <stdio.h>
#include <string.h>
#include <fstream>
#include <iostream>
#include <string>
#include <algorithm>
#include <unistd.h>

int main( int argc, char *argv[]) {
	char str[1000];
	int listen_fd, comm_fd;



	struct sockaddr_in servaddr;

	listen_fd = socket(AF_INET, SOCK_STREAM, 0);

	bzero( &servaddr, sizeof(servaddr));



	servaddr.sin_family = AF_INET;
	servaddr.sin_addr.s_addr = htons(INADDR_ANY);
	servaddr.sin_port = htons(atoi(argv[2]));
	std::fstream fs;

	if (argc != 3 ) {
		printf("Usage server filename portnumber");
		return 0;	
	} else {
		fs.open(argv[1], std::fstream::in | std::fstream::out | std::fstream::app);
		if (!fs.is_open()) {
			printf("File open failed");
			return 0;
		} else {
		}
	}

	bind(listen_fd, (struct sockaddr *) &servaddr, sizeof(servaddr));

	while(1) {

		listen(listen_fd, 10);

		comm_fd = accept(listen_fd, (struct sockaddr *) NULL, NULL);

		bzero(str, 1000);
		read(comm_fd, str, 1000);
		char new_str[1000];
		printf(str);
		if (strcmp(str,"LINE\n")== 0) {
			if ( fs.getline(new_str,1000) ) {
			} else {
			   fs.close();
			   fs.open(argv[1], std::fstream::in | std::fstream::out | std::fstream::app);
			   fs.getline(new_str, 1000);
			}
			int counter = 0;
			while (new_str[counter]) {
				new_str[counter]= toupper(new_str[counter]);
				counter++;
			}

		}
		//printf(new_str);
		write(comm_fd, new_str, strlen(new_str)+1);
	}
}
