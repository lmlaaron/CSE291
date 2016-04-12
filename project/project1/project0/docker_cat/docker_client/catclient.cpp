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
#include <arpa/inet.h>
int main(int argc,char **argv)
{
	if ( argc != 4) {
		printf("Usage: catclient filename inet_ip portnumber");
		return 0;
	}		
	int sockfd,n;
	char sendline[1000];
	char recvline[1000];
	struct sockaddr_in servaddr;
			 
	time_t seconds;
	seconds = time(NULL);	



	while(time(NULL)-seconds < 30) {

		sockfd=socket(AF_INET,SOCK_STREAM,0);
		bzero(&servaddr,sizeof servaddr);
					 
		servaddr.sin_family=AF_INET;
		servaddr.sin_port=htons(atoi(argv[3]));
						 
		inet_pton(AF_INET, argv[2], &(servaddr.sin_addr));
						     
		connect(sockfd,(struct sockaddr *)&servaddr,sizeof(servaddr));
		
			sleep(3);
	        bzero( sendline, 1000);
	        bzero( recvline, 1000);
		//fgets(sendline,100,stdin); /*stdin = 0 , for standard input */
	        strcpy(sendline,"LINE\n");
		write(sockfd,sendline,strlen(sendline)+1);
	        read(sockfd,recvline,1000);
		//printf("%s",recvline);
		
		std::fstream fs;
		char fileline[1000];
		fs.open(argv[1],std::fstream::in | std::fstream::out | std::fstream::app);
		int found = 0;
		while (fs.getline(fileline,1000)) {
			int counter = 0;
			while (fileline[counter]) {
				fileline[counter]= toupper(fileline[counter]);						counter++;
			}
			if (strcmp(fileline,recvline ) == 0) {
				found = 1;
				printf("OK\n");
				break;
			}
		}
		if (found == 0 ) {
			printf("MISSING\n");
		}

	}					     
}
