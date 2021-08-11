mkdir linux.status
cd linux.status
date --rfc-3339=seconds > date--rfc-3339.txt 2>&1
df -h > df-h.txt 2>&1
fdisk -l > fdisk-l.txt 2>&1
mount > mount.txt 2>&1
free -h > free-h.txt 2>&1
cat /proc/meminfo > meminfo.txt 2>&1
cat /proc/cpuinfo > cpuinfo.txt 2>&1
ip addr > ip_addr.txt 2>&1
netstat -anp > netstat-anp.txt 2>&1
iptables -L -n > iptables-L-n.txt 2>&1
firewall-cmd --list-port > firewall-cmd--list-port.txt 2>&1
firewall-cmd --list-service > firewall-cmd--list-service.txt 2>&1
route > route.txt 2>&1
ps -auxww > ps-auxww.txt 2>&1
docker ps > docker_ps.txt 2>&1
set > set.txt 2>&1
