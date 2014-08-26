这是一个对持久化功能消息队列

部署 :
在eyas-server目录下执行 mvn assembly:assembly
 然后拷贝  eyas-server*.tar.gz到 你指定的目录 解压,创建/var/log/eyas-server  /var/spool/eyas等目录 之后 执行 脚本  eyas-server.sh start 