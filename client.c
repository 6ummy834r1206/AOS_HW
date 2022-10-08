#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>


int main(int argc, char *argv[])
{

    //socket creation
    int sockfd = 0;
    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    if (sockfd == -1)
    {
        printf("Fail to create a socket.");
    }

    //socket connection

    struct sockaddr_in info;
    bzero(&info,sizeof(info));
    info.sin_family = PF_INET;

    //localhost test
    info.sin_addr.s_addr = inet_addr("127.0.0.1");
    info.sin_port = htons(8787);


    int err = connect(sockfd,(struct sockaddr *)&info,sizeof(info));
    if(err==-1)
    {
        printf("Connection error");
    }
    //Send a message to server
    char sendMessage[1000];
    char receiveMessage[1000];
    while(1)
    {
        memset(receiveMessage, '\0', sizeof(receiveMessage));
        memset(sendMessage,'\0', sizeof(sendMessage));
        recv(sockfd,receiveMessage,sizeof(receiveMessage),0);
        sleep(1);
        printf("Server response: %s\nTo server:",receiveMessage);
        gets(sendMessage);
        send(sockfd,sendMessage,strlen(sendMessage),0);
        sleep(1);
        if(strcmp(sendMessage, "exit") == 0)
        {
            break;
        }
    }
    close(sockfd);
    return 0;
}
