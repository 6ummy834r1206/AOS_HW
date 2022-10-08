int getFileID(struct file *file3,char *name);
int getUserID(struct user *user3,char *name);
int createFile(struct file *file3,int id, char *name, char *otherCommand, char *owner, char *group,long *size,int *isUse);
int newUser(struct user *user3, char *name, char *group, int id);
int modifyPermission(struct file *file3,struct user *user3,int id, char *otherCommand,int id);
int Seepermission(struct file *file3,struct user *user3,int id, int id, int action);

int showAllUser(struct user *user3);
int showAllFile(struct file *file3);