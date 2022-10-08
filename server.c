#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>
struct user
{
    char name[20];
    int id ;
    char group[20];
}*client;
struct file
{
    int id;
    int isUse;
    char name[20];
    char owner[20];
    char group[20];
    char right[10];
}*clientFile;
int main()
{
    pid_t pid;
    //process communication
    client = mmap(NULL,40*sizeof(struct user),PROT_READ | PROT_WRITE,MAP_SHARED|MAP_ANONYMOUS,-1,0);
    clientFile = mmap(NULL,40*sizeof(struct file),PROT_READ | PROT_WRITE,MAP_SHARED|MAP_ANONYMOUS,-1,0);
    //
    char responeMessage [1000] = {},receiveMessage[1000] = {};
    char tmp[1000] = {}, readMessage[1000]= {};
    //socket creation
    int sockfd = 0, forClientSockfd = 0;
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd == -1)
    {
        printf("Fail to create a socket.");
    }
    //socket connection
    struct sockaddr_in serverInfo,clientInfo;
    int addrlen = sizeof(clientInfo);
    bzero(&serverInfo,sizeof(serverInfo));
    serverInfo.sin_family = PF_INET;
    serverInfo.sin_addr.s_addr = INADDR_ANY;
    serverInfo.sin_port = htons(8787);
    if(bind(sockfd,(struct sockaddr *)&serverInfo,sizeof(serverInfo)) == -1 )
    {
        printf("Error: bind\n");
        exit(0);
    }
    if(listen(sockfd,6) == -1) //最多6個client
    {
        printf("Error: listen\n");
        exit(0);
    }
    printf("Server is working!\n");
    int clientID = 0;
    int fileID = 0;
    //client and file number
    client[0].id = 0;
    clientFile[0].id = 0;
    //user_info
    char *clientName;
    char *group;
    //command in
    char *command;
    char *filename;
    char *otherCommand;
    //record number
    int *clientNum;
    char *client_name;
    int *fileNum;
    while(1)
    {
        FILE *pFile;
        addrlen = sizeof(clientInfo);
        forClientSockfd = accept(sockfd,(struct sockaddr*) &clientInfo, &addrlen);
        printf("Connected Successfully\n");
        pid = fork();
        if (pid == 0) // child
        {
            send(forClientSockfd,"Enter your name and group(AOS-students or CSE-students)\n",57,0);
            sleep(1);
            recv(forClientSockfd,receiveMessage,sizeof(receiveMessage),0);
            sleep(1);
            //command split
            strcpy(tmp,receiveMessage);
            clientName = strtok(tmp," ");
            group = strtok(NULL," ");
            //create Client
            clientID = newClient(client,clientName,group,client[0].id);
            clientNum = client[0].id + 1;
            client[0].id++ ;
            //
            printf("ClientName:%s Created Successfully\n",client[clientID].name);
            printf("Client number: %d\n",clientNum);
            send(forClientSockfd,"Server listening...\n",21,0);
            while (1)
            {
                //clean recvMessage and tmp
                memset(receiveMessage, '\0', sizeof(receiveMessage));
                memset(tmp, '\0', sizeof(tmp));
                //
                recv(forClientSockfd,receiveMessage,sizeof(receiveMessage),0);
                sleep(1);
                // command split
                strcpy(tmp,receiveMessage);
                command = strtok(tmp," ");
                filename = strtok(NULL," ");
                otherCommand = strtok(NULL," ");
                if(strcmp(command, "create") == 0)
                {
                    if(access(filename,0) < 0) //file not exists
                    {
                        pFile = fopen(filename,"w");
                        fclose(pFile);
                        newFile(clientFile,clientFile[0].id,filename, client[clientID].name,client[clientID].group,otherCommand);
                        send(forClientSockfd,"Created Successfully\n",22,0);
                        printf("%s created by %s\n",filename,client[clientID].name);
                    }
                    else
                    {
                        send(forClientSockfd,"File exists\n",13,0);
                    }
                }
                else if(strcmp(command, "read") == 0)
                {
                    fileID = getFile(clientFile,filename);
                    if(fileID == -1)
                    {
                        send(forClientSockfd,"File not exists\n",17,0);
                    }
                    else
                    {
                        if(clientFile[fileID].isUse == 1)
                        {
                            send(forClientSockfd,"File is writing\n",15,0);
                        }
                        else if(seeRight(clientFile,fileID,client,clientID,0) == 0)
                        {
                            send(forClientSockfd,"You don't have the right\n",26,0);
                        }
                        else
                        {
                            printf("%s is reading %s\n",client[clientID].name,filename);
                            long fileSize;
                            pFile = fopen(filename,"r");
                            fseek(pFile,0,SEEK_END);
                            fileSize = ftell(pFile);
                            rewind(pFile);
                            fread(readMessage,1,fileSize,pFile);
                            fclose(pFile);
                            strcat(readMessage,"\nEnter any word to exit\n");
                            send(forClientSockfd,readMessage,sizeof(readMessage),0);
                            sleep(1);
                            recv(forClientSockfd,receiveMessage,sizeof(receiveMessage),0);
                            sleep(1);
                            send(forClientSockfd,"Server Listening...\n",31,0);
                            memset(readMessage,'\0', sizeof(readMessage)); //clean readMessage
                            printf("%s finished reading %s\n",client[clientID].name,filename);
                        }
                    }
                }
                else if(strcmp(command, "write") == 0)
                {
                    fileID = getFile(clientFile,filename);
                    if(fileID == -1)
                    {
                        send(forClientSockfd,"File not exists\n",17,0);
                    }
                    else
                    {
                        if(clientFile[fileID].isUse == 1)
                        {
                            send(forClientSockfd,"File is writing\n",15,0);
                        }
                        else if(seeRight(clientFile,fileID,client,clientID,1) == 0)
                        {
                            send(forClientSockfd,"You don't have the right\n",26,0);
                        }
                        else
                        {
                            printf("%s is writing by %s\n",filename,client[clientID].name);
                            clientFile[fileID].isUse = 1;
                            send(forClientSockfd,"Write Something\n",17,0);
                            if(strcmp(otherCommand, "o") == 0)
                            {
                                pFile = fopen(filename,"w");
                            }
                            else if(strcmp(otherCommand, "a") == 0)
                            {
                                pFile = fopen(filename,"a");
                            }
                            int writeBuffer;
                            writeBuffer = read(forClientSockfd,receiveMessage,sizeof(receiveMessage));
                            fwrite(receiveMessage,1,writeBuffer,pFile);
                            fclose(pFile);
                            send(forClientSockfd,"Wrote Successfully\nServer Listening...\n",41,0);
                            clientFile[fileID].isUse = 0;
                            printf("%s finished writing %s\n",client[clientID].name,filename);
                        }
                    }

                }
                else if(strcmp(command, "changemode") == 0)
                {
                    fileID = getFile(clientFile,filename);
                    if(fileID == -1)
                    {
                        send(forClientSockfd,"File not exists\n",17,0);
                    }
                    else
                    {
                        if(changeRight(clientFile,fileID,client,clientID,otherCommand) == 0)
                        {
                            send(forClientSockfd,"You don't have the right\n",26,0);
                        }
                        else
                        {
                            printf("%s changed the right：%s\n",filename,clientFile[fileID].right);
                            send(forClientSockfd,"ChangeMode Successful\nServer Listening\n",41,0);
                        }
                    }
                }
                else if(strcmp(command, "seeAllClients") == 0)
                {
                    seeAllClients(client,clientNum);
                    send(forClientSockfd,"Successful\nServer Listening...\n",33,0);
                }
                else if(strcmp(command, "seeAllFiles") == 0)
                {
                    fileNum = clientFile[0].id;
                    seeAllFiles(clientFile,fileNum);
                    send(forClientSockfd,"Successful\nServer Listening...\n",33,0);
                }
                else if(strcmp(command, "exit") == 0)
                {
                    printf("%s left \n",client[clientID].name);
                }
                else
                {
                    printf("Date recevive :%s \n", receiveMessage);
                    send(forClientSockfd,"Command Wrong...\n",18,0);
                }
                sleep(1);
                fflush(stdout);
            }
        }
        else if (pid > 0) //parent
        {
            close(forClientSockfd);
        }
        else// error
        {
            printf("Error!\n");
            exit(0);
        }
    }
    close(sockfd);
    return 0;
}
int newClient(struct user *client, char *name, char *group, int clientId)
{
    strcpy(client[clientId].name,name);
    strcpy(client[clientId].group,group);
    return clientId++;
}
void newFile(struct file *filePointer, int fileId, char *fileName, char *owner,  char *group, char *right)
{
    strcpy(filePointer[fileId].name,fileName);
    strcpy(filePointer[fileId].right,right);
    strcpy(filePointer[fileId].owner,owner);
    strcpy(filePointer[fileId].group,group);
    filePointer[fileId].isUse = 0;
    filePointer[0].id = fileId + 1;
}
int getFile(struct file *filePointer,char *name)
{
    for(int i=0; i<20; i++)
    {
        if(strcmp(filePointer[i].name,name) == 0)
        {
            return i;
        }
    }
    return -1;
}
int seeRight(struct file *filePointer,int fileId,struct user *client, int clientId, int command)
{
    // command ,0 = read , 1 = write
    char *readWrite = "rw";
    if(strcmp(filePointer[fileId].owner,client[clientId].name) == 0)//owner?
    {
        if(filePointer[fileId].right[command] == readWrite[command]) return 1;
    }
    else if(strcmp(filePointer[fileId].group,client[clientId].group) == 0)//group?
    {
        if(filePointer[fileId].right[command+2] == readWrite[command]) return 1;
    }
    else   //others
    {
        if(filePointer[fileId].right[command+4] == readWrite[command]) return 1;
    }
    return 0;

}
int changeRight(struct file *filePointer,int fileId,struct user *client,int clientId, char *right)
{
    if(strcmp(client[clientId].name,filePointer[fileId].owner) == 0)
    {
        strcpy(filePointer[clientId].right,right);
        return 1;
    }
    return 0;
}
void seeAllClients(struct user *client,int number)
{
    for(int i=0; i < number; i++)
    {
        printf("ClientName:%s ",client[i].name);
        printf("group:%s\n",client[i].group);
    }
}
void seeAllFiles(struct file *filePointer,int number)
{
    if(number != 0)
    {
        for(int i=0; i < number; i++)
        {
            printf("filename:%s ",filePointer[i].name);
            printf("owner:%s ",filePointer[i].owner);
            printf("group:%s ",filePointer[i].group);
            printf("right:%s\n",filePointer[i].right);
        }
    }
    else
    {
        printf("No File exists\n");
    }
}
